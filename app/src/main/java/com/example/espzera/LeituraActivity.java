package com.example.espzera; // ATENÇÃO: VERIFIQUE SE ESTE É O NOME DO SEU PACOTE REAL!

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue; // Importar para converter DP para Pixels
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout; // Importar LinearLayout
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Para cores compatíveis

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket; // Importação para UDP
import java.net.DatagramSocket; // Importação para UDP
import java.net.InetAddress;    // Importação para UDP
import java.net.SocketException; // Importação para tratamento de exceções UDP
import java.net.URL;
import java.net.UnknownHostException; // Importação para tratamento de exceções UDP
import java.text.SimpleDateFormat; // Para formatar a data
import java.util.Date;             // Para obter a data atual
import java.util.Locale;           // Para o Locale da data
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean; // Para controlar o loop do listener

public class LeituraActivity extends AppCompatActivity {

    private EditText gridWidthInput;
    private EditText gridHeightInput;
    private Button generateGridBtn;
    private GridLayout gridContainer;
    private TextView clickedCoordinatesSpan;
    private TextView messageBox;
    private EditText collectionTimeInput;
    private Button startButton;
    private TextView collectionStatusSpan;

    private View selectedCell = null; // Armazena a célula atualmente selecionada
    private int currentGridWidth = 0;
    private int currentGridHeight = 0;

    // Variável para o DatabaseHelper
    private DatabaseHelper dbHelper;

    // Executor para operações de rede e banco de dados em segundo plano
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    // Handler para atualizar a UI na thread principal
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Variáveis para o envio UDP
    private static final int UDP_SEND_PORT = 8888; // Porta UDP para broadcast (pode ser alterada)
    private static final int UDP_RECEIVE_PORT = 8889; // NOVA Porta UDP para o APP receber dados do ESP32
    private static final String BROADCAST_ADDRESS = "255.255.255.255"; // Endereço de broadcast padrão

    private Handler udpSenderHandler = new Handler(Looper.getMainLooper());
    private Runnable udpSenderRunnable;
    private int messagesSentCount = 0;
    private static final int MAX_MESSAGES = 30; // Enviar por 30 segundos (30 mensagens de 1 em 1 segundo)

    // Variáveis para o UDP Listener
    private DatagramSocket receiveSocket;
    private UdpListenerRunnable udpListenerRunnable;
    private int currentLeituraId = -1; // ID da leitura atual para associar os dados CSI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leitura);

        // Inicializa o DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        // Configura a barra de ação (opcional, para exibir o botão Voltar)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Tela de Leitura");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Habilita o botão Voltar
        }

        // Inicializa os elementos da UI
        gridWidthInput = findViewById(R.id.gridWidth);
        gridHeightInput = findViewById(R.id.gridHeight);
        generateGridBtn = findViewById(R.id.generateGridBtn);
        gridContainer = findViewById(R.id.gridContainer);
        clickedCoordinatesSpan = findViewById(R.id.clickedCoordinates);
        messageBox = findViewById(R.id.messageBox);
        collectionTimeInput = findViewById(R.id.collectionTime);
        startButton = findViewById(R.id.startButton);
        collectionStatusSpan = findViewById(R.id.collectionStatus);

        // Define listeners para os botões
        generateGridBtn.setOnClickListener(v -> generateGrid());
        startButton.setOnClickListener(v -> startCollection());

        // Gera a grade inicial ao carregar a Activity
        generateGrid();
    }

    /**
     * Exibe uma mensagem na caixa de mensagens (Toast para Android).
     * @param message A mensagem a ser exibida.
     * @param type O tipo da mensagem ('success', 'error', 'info').
     */
    private void showMessage(String message, String type) {
        // Para Android, usaremos Toast e Log.d para mensagens
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        if (type.equals("error")) {
            Log.e("LeituraActivity", message);
        } else {
            Log.d("LeituraActivity", message);
        }
    }

    /**
     * Gera a grade com base na largura e altura fornecidas.
     */
    private void generateGrid() {
        String widthStr = gridWidthInput.getText().toString();
        String heightStr = gridHeightInput.getText().toString();

        Log.d("LeituraActivity", "Attempting to generate grid with width: " + widthStr + ", height: " + heightStr);

        if (widthStr.isEmpty() || heightStr.isEmpty()) {
            showMessage("Por favor, preencha a largura e a altura da grade.", "error");
            Log.e("LeituraActivity", "Width or height string is empty.");
            return;
        }

        int width, height;
        try {
            width = Integer.parseInt(widthStr);
            height = Integer.parseInt(heightStr);
        } catch (NumberFormatException e) {
            showMessage("Largura e Altura devem ser números inteiros válidos.", "error");
            Log.e("LeituraActivity", "NumberFormatException: " + e.getMessage());
            return;
        }


        // Validação do tamanho da grade
        if (width <= 0 || width > 50 || height <= 0 || height > 50) {
            showMessage("Por favor, insira números inteiros positivos entre 1 e 50 para Largura e Altura.", "error");
            Log.e("LeituraActivity", "Invalid grid dimensions: width=" + width + ", height=" + height);
            return;
        }

        currentGridWidth = width;
        currentGridHeight = height;
        gridContainer.removeAllViews(); // Limpa a grade existente

        // Configura o GridLayout
        gridContainer.setColumnCount(width);
        gridContainer.setRowCount(height); // Definir explicitamente o número de linhas

        // Calcula o tamanho da célula em pixels (ex: 40dp)
        // Isso garante que cada célula tenha um tamanho visível
        int cellSizePx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()
        );

        // Cria as células da grade
        for (int i = 0; i < height; i++) { // Iterar por linhas
            for (int j = 0; j < width; j++) { // Iterar por colunas
                TextView cell = new TextView(this);

                // Define os parâmetros de layout para a célula
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSizePx; // Largura fixa
                params.height = cellSizePx; // Altura fixa
                params.setMargins(1, 1, 1, 1); // Pequena margem para visibilidade

                // Definir explicitamente rowSpec e columnSpec para garantir o posicionamento
                params.rowSpec = GridLayout.spec(i);
                params.columnSpec = GridLayout.spec(j);

                cell.setLayoutParams(params);
                cell.setBackground(ContextCompat.getDrawable(this, R.drawable.grid_cell_background)); // Usar um drawable XML
                cell.setGravity(Gravity.CENTER);
                // REMOVIDO: cell.setTextColor(ContextCompat.getColor(this, R.color.black)); // Removido para não mostrar números
                // REMOVIDO: cell.setText((j + 1) + "," + (i + 1)); // Removido para não mostrar números

                cell.setTag(new int[]{i, j}); // Armazena a linha e coluna como um array de int no tag
                cell.setOnClickListener(v -> handleCellClick(v));
                gridContainer.addView(cell);
            }
        }
        clickedCoordinatesSpan.setText("Nenhum"); // Reseta as coordenadas
        startButton.setEnabled(false); // Desabilita o botão Iniciar até uma célula ser clicada
        collectionStatusSpan.setText("Aguardando..."); // Reseta o status da coleta
        selectedCell = null; // Reseta a célula selecionada
        showMessage("Grade " + width + "x" + height + " gerada com sucesso!", "success");

        // Força o GridLayout a recalcular e redesenhar
        // Isso é crucial para que a grade se atualize visualmente
        gridContainer.post(() -> {
            gridContainer.requestLayout();
            gridContainer.invalidate();
            // Também solicita o layout para o pai para garantir que ele remeça seus filhos
            // Isso é importante se o GridLayout estiver dentro de um LinearLayout ou outro container
            if (gridContainer.getParent() instanceof View) {
                ((View) gridContainer.getParent()).requestLayout();
                ((View) gridContainer.getParent()).invalidate();
            }
        });

        Log.d("LeituraActivity", "Grid generation complete for " + width + "x" + height);
    }

    /**
     * Manipula o clique em uma célula da grade.
     * @param view A View (TextView) da célula clicada.
     */
    private void handleCellClick(View view) {
        // Remove o destaque da célula anteriormente selecionada
        if (selectedCell != null) {
            selectedCell.setBackground(ContextCompat.getDrawable(this, R.drawable.grid_cell_background));
        }

        selectedCell = view;
        selectedCell.setBackgroundColor(ContextCompat.getColor(this, R.color.green_200)); // Cor de destaque

        int[] coords = (int[]) selectedCell.getTag();
        int row = coords[0];
        int col = coords[1];

        // Exibe as coordenadas (começando de 1 para o usuário)
        clickedCoordinatesSpan.setText("(" + (col + 1) + ", " + (row + 1) + ")");
        startButton.setEnabled(true); // Habilita o botão Iniciar
        showMessage("Ponto (" + (col + 1) + ", " + (row + 1) + ") selecionado. Clique em \"Iniciar Coleta\".", "info");
    }

    /**
     * Inicia o processo de coleta de dados, enviando mensagens UDP em broadcast e salvando no DB.
     */
    private void startCollection() {
        if (selectedCell == null) {
            showMessage("Por favor, selecione um ponto na grade primeiro.", "error");
            return;
        }

        String timeStr = collectionTimeInput.getText().toString();
        if (timeStr.isEmpty()) {
            showMessage("Por favor, insira um tempo de coleta válido.", "error");
            return;
        }
        final int collectionTime; // Usar final para acesso no Runnable
        try {
            collectionTime = Integer.parseInt(timeStr);
        } catch (NumberFormatException e) {
            showMessage("Tempo de coleta deve ser um número inteiro válido.", "error");
            return;
        }

        if (collectionTime <= 0) {
            showMessage("Por favor, insira um tempo de coleta válido (número inteiro positivo).", "error");
            return;
        }

        int[] coords = (int[]) selectedCell.getTag();
        final int row = coords[0]; // final para uso na thread
        final int col = coords[1]; // final para uso na thread

        startButton.setEnabled(false); // Desabilita o botão durante a coleta
        collectionStatusSpan.setText("Iniciando envio UDP e salvando dados...");
        showMessage("Iniciando envio de mensagens UDP Broadcast e salvando dados...", "info");

        // Resetar contador de mensagens enviadas
        messagesSentCount = 0;

        // Executa a lógica de banco de dados e UDP em uma thread separada
        executorService.execute(() -> {
            long idAmbiente = -1;
            long idPonto = -1;
            long idLeitura = -1; // Variável para armazenar o ID da leitura

            try {
                // 1. Salvar/Obter Ambiente
                String ambienteDesc = "Grade " + currentGridWidth + "x" + currentGridHeight;
                idAmbiente = dbHelper.getOrAddAmbiente(ambienteDesc, currentGridWidth, currentGridHeight, 1);
                if (idAmbiente == -1) {
                    throw new Exception("Falha ao obter/salvar ambiente.");
                }
                Log.d("LeituraActivity", "ID Ambiente: " + idAmbiente);

                // 2. Salvar/Obter Ponto
                int pontoNumero = (row * currentGridWidth) + col + 1;
                idPonto = dbHelper.getOrAddPonto((int) idAmbiente, pontoNumero, col + 1, row + 1, 1);
                if (idPonto == -1) {
                    throw new Exception("Falha ao obter/salvar ponto.");
                }
                Log.d("LeituraActivity", "ID Ponto: " + idPonto);

                // 3. Salvar Leitura
                String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                idLeitura = dbHelper.addLeitura((int) idAmbiente, (int) idPonto, collectionTime, currentDateTime, 1);
                if (idLeitura == -1) {
                    throw new Exception("Falha ao salvar leitura.");
                }
                Log.d("LeituraActivity", "ID Leitura: " + idLeitura);

                // Armazena o ID da leitura para o listener UDP
                currentLeituraId = (int) idLeitura;

                // Inicia o listener UDP para receber dados CSI do ESP32
                startUdpListener(currentLeituraId);

                // Inicia o agendamento do envio do comando UDP para o ESP32
                mainHandler.post(() -> {
                    udpSenderRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (messagesSentCount < MAX_MESSAGES) {
                                executorService.execute(() -> { // Executa o envio UDP em outra thread do pool
                                    try {
                                        // Mensagem UDP: "start,(tempo_da_coleta_segundos)"
                                        String message = "start," + collectionTime;
                                        byte[] buffer = message.getBytes();

                                        DatagramSocket socket = new DatagramSocket(); // Cria um socket UDP
                                        socket.setBroadcast(true); // Habilita o modo broadcast
                                        InetAddress address = InetAddress.getByName(BROADCAST_ADDRESS); // Endereço de broadcast
                                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, UDP_SEND_PORT);

                                        socket.send(packet); // Envia o pacote
                                        socket.close(); // Fecha o socket após o envio

                                        Log.d("LeituraActivity", "UDP Broadcast sent: " + message + " (Count: " + (messagesSentCount + 1) + ")");

                                        // Atualiza a UI na thread principal com o progresso
                                        mainHandler.post(() -> {
                                            collectionStatusSpan.setText("Enviando UDP: " + (messagesSentCount + 1) + "/" + MAX_MESSAGES);
                                        });

                                    } catch (SocketException e) {
                                        Log.e("LeituraActivity", "SocketException ao enviar UDP: " + e.getMessage());
                                        mainHandler.post(() -> showMessage("Erro de socket UDP: " + e.getMessage(), "error"));
                                    } catch (UnknownHostException e) {
                                        Log.e("LeituraActivity", "UnknownHostException ao enviar UDP: " + e.getMessage());
                                        mainHandler.post(() -> showMessage("Erro de host UDP: " + e.getMessage(), "error"));
                                    } catch (Exception e) {
                                        Log.e("LeituraActivity", "Erro inesperado ao enviar UDP Broadcast: " + e.getMessage());
                                        mainHandler.post(() -> showMessage("Erro ao enviar UDP: " + e.getMessage(), "error"));
                                    }
                                });

                                messagesSentCount++;
                                // Agenda o próximo envio em 1 segundo
                                udpSenderHandler.postDelayed(this, 1000);
                            } else {
                                // Após 30 segundos (MAX_MESSAGES), parar de enviar e reabilitar o botão
                                mainHandler.post(() -> {
                                    collectionStatusSpan.setText("Envio UDP Concluído!");
                                    showMessage("Envio de mensagens UDP e salvamento de dados finalizados.", "success");
                                    startButton.setEnabled(true); // Reabilita o botão
                                    stopUdpListener(); // Para o listener UDP
                                });
                            }
                        }
                    };
                    udpSenderHandler.post(udpSenderRunnable); // Inicia o primeiro envio UDP
                });

            } catch (Exception e) {
                Log.e("LeituraActivity", "Erro ao salvar dados no banco de dados ou iniciar listener: " + e.getMessage());
                mainHandler.post(() -> {
                    collectionStatusSpan.setText("Erro ao Salvar Dados ou Iniciar Coleta!");
                    showMessage("Erro ao salvar dados da medição ou iniciar coleta: " + e.getMessage(), "error");
                    startButton.setEnabled(true); // Reabilita o botão em caso de erro no DB
                    stopUdpListener(); // Garante que o listener seja parado em caso de erro
                });
            }
        });
    }

    /**
     * Inicia o listener UDP em uma nova thread para receber dados do ESP32.
     * @param leituraId O ID da leitura atual para associar os dados CSI.
     */
    private void startUdpListener(int leituraId) {
        if (udpListenerRunnable != null && udpListenerRunnable.isRunning()) {
            Log.d("LeituraActivity", "UDP Listener já está rodando.");
            return;
        }
        Log.d("LeituraActivity", "Iniciando UDP Listener na porta " + UDP_RECEIVE_PORT);
        udpListenerRunnable = new UdpListenerRunnable(leituraId);
        executorService.execute(udpListenerRunnable); // Executa o listener no pool de threads
    }

    /**
     * Para o listener UDP.
     */
    private void stopUdpListener() {
        if (udpListenerRunnable != null) {
            udpListenerRunnable.stop();
            udpListenerRunnable = null;
            Log.d("LeituraActivity", "UDP Listener parado.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Garante que o executor e o handler de UDP sejam desligados
        executorService.shutdownNow();
        if (udpSenderRunnable != null) {
            udpSenderHandler.removeCallbacks(udpSenderRunnable); // Remove callbacks pendentes
        }
        stopUdpListener(); // Garante que o listener UDP seja parado
        // Fecha o banco de dados quando a Activity for destruída
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    /**
     * Runnable para o listener UDP em segundo plano.
     */
    private class UdpListenerRunnable implements Runnable {
        private final int currentLeituraId;
        private AtomicBoolean running = new AtomicBoolean(true); // Para controlar o loop

        public UdpListenerRunnable(int leituraId) {
            this.currentLeituraId = leituraId;
        }

        public boolean isRunning() {
            return running.get();
        }

        public void stop() {
            running.set(false);
            if (receiveSocket != null && !receiveSocket.isClosed()) {
                receiveSocket.close(); // Fecha o socket para interromper o blocking receive
                Log.d("UdpListenerRunnable", "Receive socket closed.");
            }
        }

        @Override
        public void run() {
            try {
                receiveSocket = new DatagramSocket(UDP_RECEIVE_PORT); // Liga o socket à porta de recebimento
                receiveSocket.setSoTimeout(1000); // Timeout para não bloquear indefinidamente

                byte[] buffer = new byte[2048]; // Buffer para o pacote CSI (ajuste o tamanho conforme necessário)
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                mainHandler.post(() -> collectionStatusSpan.setText("Ouvindo dados CSI do ESP32..."));

                while (running.get()) {
                    try {
                        receiveSocket.receive(packet); // Espera por um pacote (bloqueante)
                        String receivedData = new String(packet.getData(), 0, packet.getLength());
                        Log.d("UdpListenerRunnable", "Received CSI data: " + receivedData + " from " + packet.getAddress().getHostAddress());

                        // Salva os dados CSI no banco de dados
                        final String csiTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
                        dbHelper.addCsiData(currentLeituraId, csiTimestamp, receivedData);
                        Log.d("UdpListenerRunnable", "CSI data saved to DB for Leitura ID: " + currentLeituraId);

                        mainHandler.post(() -> {
                            // Opcional: Atualizar a UI com algum feedback de CSI recebido
                            // collectionStatusSpan.setText("CSI Recebido! (Leitura ID: " + currentLeituraId + ")");
                        });

                    } catch (java.net.SocketTimeoutException e) {
                        // Timeout, loop continua se ainda estiver rodando
                        Log.d("UdpListenerRunnable", "UDP receive timeout.");
                    } catch (SocketException e) {
                        if (running.get()) { // Se o socket foi fechado intencionalmente, não é um erro
                            Log.e("UdpListenerRunnable", "SocketException while receiving UDP: " + e.getMessage());
                            mainHandler.post(() -> showMessage("Erro de socket UDP ao receber: " + e.getMessage(), "error"));
                        }
                    } catch (Exception e) {
                        Log.e("UdpListenerRunnable", "Erro inesperado ao receber UDP: " + e.getMessage());
                        mainHandler.post(() -> showMessage("Erro ao receber CSI: " + e.getMessage(), "error"));
                    }
                }
            } catch (SocketException e) {
                Log.e("UdpListenerRunnable", "Could not open UDP receive socket: " + e.getMessage());
                mainHandler.post(() -> showMessage("Não foi possível abrir socket UDP para receber dados: " + e.getMessage(), "error"));
            } finally {
                if (receiveSocket != null && !receiveSocket.isClosed()) {
                    receiveSocket.close();
                    Log.d("UdpListenerRunnable", "Receive socket closed in finally block.");
                }
            }
        }
    }
}
