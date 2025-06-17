package com.example.espzera;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CollectionActivity extends AppCompatActivity {

    private EditText espIpInput, collectionTimeInput, cenarioInput, listenPortInput;
    private Button discoverButton, startButton, stopButton;
    private ProgressBar discoveryProgress;
    private TextView consoleOutput;
    private ScrollView consoleScroll;
    private DatabaseHelper dbHelper;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private DatagramSocket listenSocket;
    private final AtomicBoolean isCollecting = new AtomicBoolean(false);
    private final AtomicBoolean firstPacketReceived = new AtomicBoolean(false);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        dbHelper = new DatabaseHelper(this);
        espIpInput = findViewById(R.id.esp_ip_input);
        collectionTimeInput = findViewById(R.id.collection_time_input);
        cenarioInput = findViewById(R.id.cenario_input);
        listenPortInput = findViewById(R.id.listen_port_input);
        discoverButton = findViewById(R.id.discover_button);
        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);
        discoveryProgress = findViewById(R.id.discovery_progress);
        consoleOutput = findViewById(R.id.console_output);
        consoleScroll = findViewById(R.id.console_scroll);

        discoverButton.setOnClickListener(v -> startDiscovery());
        startButton.setOnClickListener(v -> startCollection());
        stopButton.setOnClickListener(v -> stopCollection());
    }

    private void startDiscovery() {
        logToConsole("Procurando por ESP32 por 75 segundos...");
        discoverButton.setEnabled(false);
        discoveryProgress.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try (DatagramSocket s = new DatagramSocket(50002)) {
                s.setBroadcast(true);
                // O timeout do receive é em milissegundos
                s.setSoTimeout(75000);

                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                s.receive(packet); // Bloqueia até receber ou dar timeout

                String message = new String(packet.getData(), 0, packet.getLength());
                if (message.startsWith("CSI_IP,")) {
                    String discoveredIp = message.split(",")[1];
                    mainHandler.post(() -> {
                        espIpInput.setText(discoveredIp);
                        logToConsole("ESP32 encontrado em: " + discoveredIp);
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> logToConsole("ERRO: Nenhum ESP32 encontrado. " + e.getMessage()));
            } finally {
                mainHandler.post(() -> {
                    discoverButton.setEnabled(true);
                    discoveryProgress.setVisibility(View.GONE);
                });
            }
        });
    }

    private void startCollection() {
        String espIp = espIpInput.getText().toString();
        String time = collectionTimeInput.getText().toString();
        String portStr = listenPortInput.getText().toString();
        String cenario = cenarioInput.getText().toString();

        if (espIp.isEmpty() || time.isEmpty() || portStr.isEmpty()) {
            Toast.makeText(this, "Preencha o IP, Tempo e Porta.", Toast.LENGTH_SHORT).show();
            return;
        }

        if(cenario.isEmpty()){
            Toast.makeText(this, "Por favor, descreva o cenário da coleta.", Toast.LENGTH_SHORT).show();
            return;
        }

        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        discoverButton.setEnabled(false);
        isCollecting.set(true);
        firstPacketReceived.set(false);

        // Inicia o listener UDP
        executor.execute(() -> udpListenerThread(Integer.parseInt(portStr), cenario));

        // Envia o comando 'start' repetidamente até receber o primeiro pacote
        executor.execute(() -> {
            try (DatagramSocket commandSocket = new DatagramSocket()) {
                while (isCollecting.get() && !firstPacketReceived.get()) {
                    String startMessage = "start," + time;
                    byte[] buffer = startMessage.getBytes("UTF-8");
                    InetAddress espAddress = InetAddress.getByName(espIp);
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, espAddress, 50000);
                    commandSocket.send(packet);
                    logToConsole("Comando 'start' enviado para " + espIp);
                    Thread.sleep(100); // Intervalo maior para não sobrecarregar
                }
            } catch (Exception e) {
                logToConsole("Erro ao enviar 'start': " + e.getMessage());
            }
        });
    }

    private void udpListenerThread(int port, String cenario) {
        try {
            listenSocket = new DatagramSocket(port);
            listenSocket.setSoTimeout(1000);
            logToConsole("Aguardando dados CSI na porta " + port);

            byte[] buffer = new byte[1500];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());


            while (isCollecting.get()) {
                try {
                    listenSocket.receive(packet);
                    String data = new String(packet.getData(), 0, packet.getLength()).trim();
                    if (data.startsWith("CSI_DATA")) {
                        if(!firstPacketReceived.get()){
                            firstPacketReceived.set(true); // Para de enviar comandos 'start'
                            logToConsole("Primeiro pacote CSI recebido. Iniciando salvamento.");
                        }

                        // Parse e salva no banco de dados em uma thread do pool
                        final String finalData = data;
                        executor.execute(() -> {
                            String[] parts = finalData.split(",", 26);
                            if (parts.length == 26) {
                                String[] csiPayload = Arrays.copyOfRange(parts, 1, parts.length);
                                String timestamp = sdf.format(new Date());
                                dbHelper.addCsiData(timestamp, cenario, csiPayload);
                            }
                        });
                        logToConsole(data);
                    }
                } catch (java.net.SocketTimeoutException e) {
                    // Ignora timeouts, continua o loop
                }
            }
        } catch (Exception e) {
            logToConsole("Erro na thread de escuta: " + e.getMessage());
        } finally {
            if (listenSocket != null && !listenSocket.isClosed()) {
                listenSocket.close();
            }
            logToConsole("Coleta finalizada.");
            mainHandler.post(() -> {
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                discoverButton.setEnabled(true);
            });
        }
    }

    private void stopCollection() {
        isCollecting.set(false);
    }

    private void logToConsole(final String message) {
        mainHandler.post(() -> {
            consoleOutput.append(message + "\n");
            consoleScroll.fullScroll(View.FOCUS_DOWN);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCollection(); // Garante que a flag seja setada
        executor.shutdownNow(); // Encerra todas as threads em execução
        // O fechamento do socket já é tratado no bloco finally da thread
    }
}