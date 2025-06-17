package com.example.espzera;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CollectionActivity extends AppCompatActivity {

    private static final String TAG = "CollectionActivity";

    private TextInputEditText espIpInput, collectionTimeInput, cenarioInput;
    private Button discoverButton, selectDbButton, startButton, stopButton;
    private ProgressBar discoveryProgress;
    private TextView consoleOutput, selectedDbPath;
    private ScrollView consoleScroll;
    private Uri targetDbUri;
    private ActivityResultLauncher<Intent> createFileLauncher;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private DatagramSocket listenSocket;
    private final AtomicBoolean isCollecting = new AtomicBoolean(false);
    private final AtomicBoolean firstPacketReceived = new AtomicBoolean(false);
    private final List<String> csiDataBuffer = new ArrayList<>();
    private CountDownTimer collectionTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);
        setupUI();

        createFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        targetDbUri = result.getData().getData();
                        if (targetDbUri != null) {
                            String path = targetDbUri.getPath();
                            selectedDbPath.setText("Salvar em: " + path);
                            startButton.setEnabled(true);
                            logToConsole("Arquivo de saída selecionado. Pronto para iniciar.");
                        }
                    }
                }
        );
        selectDbButton.setOnClickListener(v -> openFileCreator());
        discoverButton.setOnClickListener(v -> startDiscovery());
        startButton.setOnClickListener(v -> startCollection());
        stopButton.setOnClickListener(v -> stopCollection());
    }

    private void setupUI() {
        espIpInput = findViewById(R.id.esp_ip_input);
        collectionTimeInput = findViewById(R.id.collection_time_input);
        cenarioInput = findViewById(R.id.cenario_input);
        discoverButton = findViewById(R.id.discover_button);
        selectDbButton = findViewById(R.id.select_db_button);
        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);
        discoveryProgress = findViewById(R.id.discovery_progress);
        consoleOutput = findViewById(R.id.console_output);
        selectedDbPath = findViewById(R.id.selected_db_path);
        consoleScroll = findViewById(R.id.console_scroll);
        logToConsole("Bem-vindo! Procure um ESP ou selecione um arquivo para começar.");
    }

    private void openFileCreator() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.sqlite3");
        intent.putExtra(Intent.EXTRA_TITLE, "csi_data_" + System.currentTimeMillis() + ".db");
        createFileLauncher.launch(intent);
    }

    private void startDiscovery() {
        setDiscoveryState(true);
        logToConsole("Procurando por ESP32 por 75 segundos...");
        executor.execute(() -> {
            try (DatagramSocket s = new DatagramSocket(50002)) {
                s.setSoTimeout(75000);
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                s.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                if (message.startsWith("CSI_IP,")) {
                    String discoveredIp = message.split(",")[1];
                    mainHandler.post(() -> {
                        espIpInput.setText(discoveredIp);
                        logToConsole("ESP32 encontrado em: " + discoveredIp);
                    });
                }
            } catch (SocketTimeoutException e) {
                mainHandler.post(() -> logToConsole("ERRO: Nenhum ESP32 encontrado."));
            } catch (Exception e) {
                mainHandler.post(() -> logToConsole("ERRO na descoberta: " + e.getMessage()));
            } finally {
                mainHandler.post(() -> setDiscoveryState(false));
            }
        });
    }

    private void startCollection() {
        if (targetDbUri == null) {
            Toast.makeText(this, "Por favor, selecione um arquivo de saída primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }
        String espIp = espIpInput.getText().toString().trim();
        String timeStr = collectionTimeInput.getText().toString().trim();
        String cenario = cenarioInput.getText().toString().trim();
        if (espIp.isEmpty() || timeStr.isEmpty() || cenario.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        int collectionTime = Integer.parseInt(timeStr);

        setCollectionState(true);
        isCollecting.set(true);
        firstPacketReceived.set(false);
        csiDataBuffer.clear();

        logToConsole("---------------------------------");
        logToConsole("Iniciando coleta por " + collectionTime + " segundos...");

        executor.execute(() -> udpListenerThread());
        executor.execute(() -> sendStartCommand(espIp, timeStr));

        collectionTimer = new CountDownTimer(collectionTime * 1000L, 5000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                logToConsole("... " + csiDataBuffer.size() + " pacotes no buffer ...");
            }
            @Override
            public void onFinish() {
                logToConsole("Tempo de coleta finalizado.");
                stopCollection();
            }
        }.start();
    }

    private void sendStartCommand(String espIp, String time) {
        try (DatagramSocket commandSocket = new DatagramSocket()) {
            while (isCollecting.get() && !firstPacketReceived.get()) {
                String startMessage = "start," + time;
                byte[] buffer = startMessage.getBytes("UTF-8");
                InetAddress espAddress = InetAddress.getByName(espIp);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, espAddress, 50000);
                commandSocket.send(packet);
                logToConsole("Comando 'start' enviado para " + espIp);
                Thread.sleep(200);
            }
        } catch (Exception e) {
            logToConsole("Erro ao enviar 'start': " + e.getMessage());
        }
    }

    private void udpListenerThread() {
        try {
            listenSocket = new DatagramSocket(50001);
            logToConsole("Aguardando dados CSI na porta 50001...");
            byte[] buffer = new byte[2048];
            while (isCollecting.get()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                listenSocket.receive(packet);
                String data = new String(packet.getData(), 0, packet.getLength()).trim();

                if (data.startsWith("CSI_DATA")) {
                    if (!firstPacketReceived.getAndSet(true)) {
                        logToConsole("Primeiro pacote CSI recebido. Parando envio de 'start'.");
                    }
                    logToConsole(data);

                    synchronized (csiDataBuffer) {
                        csiDataBuffer.add(data);
                    }
                }
            }
        } catch (Exception e) {
            if (isCollecting.get()) {
                logToConsole("Erro na thread de escuta: " + e.getMessage());
            }
        } finally {
            if (listenSocket != null && !listenSocket.isClosed()) listenSocket.close();
            logToConsole("Thread de escuta finalizada.");
        }
    }

    private void stopCollection() {
        if (!isCollecting.getAndSet(false)) return;

        if (collectionTimer != null) {
            collectionTimer.cancel();
        }
        if (listenSocket != null) {
            listenSocket.close();
        }

        try {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
            new Handler(Looper.getMainLooper()).postDelayed(toneGen::release, 300);
        } catch (Exception e) {
            Log.e(TAG, "Falha ao tocar o bip de notificação", e);
        }

        mainHandler.post(() -> {
            setCollectionState(false);
            logToConsole("Coleta parada. Salvando dados...");
            new Handler(Looper.getMainLooper()).postDelayed(this::saveDataToSelectedFile, 500);
        });
    }

    private void saveDataToSelectedFile() {
        final List<String> dataToSave;
        synchronized (csiDataBuffer) {
            if (csiDataBuffer.isEmpty()) {
                logToConsole("Nenhum dado para salvar. O buffer está vazio.");
                return;
            }
            dataToSave = new ArrayList<>(csiDataBuffer);
            csiDataBuffer.clear();
        }

        logToConsole("Iniciando salvamento de " + dataToSave.size() + " registros...");
        executor.execute(() -> {
            File tempDbFile = new File(getCacheDir(), "temp_csi.db");
            if(tempDbFile.exists()) tempDbFile.delete();

            long successCount = 0;
            try (SQLiteDatabase tempDb = SQLiteDatabase.openOrCreateDatabase(tempDbFile, null)) {
                DatabaseHelper.createCsiDataTable(tempDb);
                tempDb.beginTransaction();
                try {
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    String cenario = cenarioInput.getText().toString();
                    for (String line : dataToSave) {
                        String sanitizedData = line.replaceAll("[\\[\\]]", "").trim();
                        String[] parts = sanitizedData.split(",", 0); // Pega todas as partes

                        if (parts.length > 1) {
                            String[] csiPayload = Arrays.copyOfRange(parts, 1, parts.length);
                            if (DatabaseHelper.addCsiData(tempDb, timestamp, cenario, csiPayload) != -1) {
                                successCount++;
                            }
                        }
                    }
                    tempDb.setTransactionSuccessful();
                } finally {
                    tempDb.endTransaction();
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao escrever no DB temporário", e);
                mainHandler.post(() -> logToConsole("Erro ao preparar dados."));
                return;
            }

            try (InputStream in = new FileInputStream(tempDbFile);
                 OutputStream out = getContentResolver().openOutputStream(targetDbUri)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);

                long finalCount = successCount;
                mainHandler.post(() -> {
                    logToConsole("Sucesso! " + finalCount + " de " + dataToSave.size() + " registros válidos foram salvos.");
                    Toast.makeText(this, "Arquivo .db salvo com sucesso!", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Erro ao copiar DB para o arquivo final", e);
                mainHandler.post(() -> logToConsole("Erro ao salvar arquivo final."));
            } finally {
                tempDbFile.delete();
            }
        });
    }

    private void setCollectionState(boolean collecting) {
        startButton.setEnabled(!collecting && targetDbUri != null);
        stopButton.setEnabled(collecting);
        setFieldsEnabled(!collecting);
    }

    private void setDiscoveryState(boolean discovering) {
        discoverButton.setEnabled(!discovering);
        discoveryProgress.setVisibility(discovering ? View.VISIBLE : View.GONE);
        setFieldsEnabled(!discovering);
    }

    private void setFieldsEnabled(boolean enabled) {
        espIpInput.setEnabled(enabled);
        collectionTimeInput.setEnabled(enabled);
        cenarioInput.setEnabled(enabled);
        selectDbButton.setEnabled(enabled);
        if(enabled) {
            startButton.setEnabled(targetDbUri != null);
        } else {
            startButton.setEnabled(false);
        }
        stopButton.setEnabled(false);
        if(isCollecting.get()) stopButton.setEnabled(true);
    }

    private void logToConsole(final String message) {
        mainHandler.post(() -> {
            if(consoleOutput != null) {
                consoleOutput.append("\n> " + message);
                if(consoleScroll != null) consoleScroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCollection();
        executor.shutdownNow();
    }
}