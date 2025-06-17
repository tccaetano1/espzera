package com.example.espzera;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProvisioningActivity extends AppCompatActivity {

    private TextInputEditText provSsid, provIdentity, provPassword, provServerIp, provServerPort;
    private TextInputLayout layoutProvIdentity;
    private Spinner provAuthType;
    private Button sendButton;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provisioning);

        provSsid = findViewById(R.id.prov_ssid);
        provIdentity = findViewById(R.id.prov_identity);
        layoutProvIdentity = findViewById(R.id.layout_prov_identity);
        provPassword = findViewById(R.id.prov_password);
        provServerIp = findViewById(R.id.prov_server_ip);
        provServerPort = findViewById(R.id.prov_server_port);
        provAuthType = findViewById(R.id.prov_auth_type);
        sendButton = findViewById(R.id.send_provision_button);

        // Preenche o IP local
        provServerIp.setText(getLocalIpAddress());

        provAuthType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Posição 0 = WPA2-PSK, Posição 1 = PEAP
                layoutProvIdentity.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        sendButton.setOnClickListener(v -> sendProvisionCommand());
    }

    private String getLocalIpAddress() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        } catch (Exception e) {
            return "192.168.1.100"; // Fallback
        }
    }

    private void sendProvisionCommand() {
        String ssid = provSsid.getText().toString();
        String password = provPassword.getText().toString();
        String serverIp = provServerIp.getText().toString();
        String serverPort = provServerPort.getText().toString();
        // A posição no spinner (0 ou 1) determina o tipo
        int authTypePos = provAuthType.getSelectedItemPosition();

        if (ssid.isEmpty() || password.isEmpty() || serverIp.isEmpty() || serverPort.isEmpty()){
            Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        String command;
        if (authTypePos == 0) { // WPA2-PSK
            command = "wpa2psk," + ssid + "," + password + "," + serverIp + "," + serverPort;
        } else { // PEAP
            String identity = provIdentity.getText().toString();
            if(identity.isEmpty()){
                Toast.makeText(this, "O campo Identidade é obrigatório para WPA2-Enterprise.", Toast.LENGTH_SHORT).show();
                return;
            }
            command = "peap," + ssid + "," + identity + "," + password + "," + serverIp + "," + serverPort;
        }

        executor.execute(() -> {
            try (DatagramSocket s = new DatagramSocket()) {
                byte[] buffer = command.getBytes("UTF-8");
                // O ESP32 em modo AP (Access Point) para provisionamento terá sempre o mesmo IP
                InetAddress espAddress = InetAddress.getByName("192.168.4.1");
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, espAddress, 50000);
                s.send(packet);
                runOnUiThread(() -> {
                    Toast.makeText(ProvisioningActivity.this, "Configuração enviada! O ESP32 deve reiniciar.", Toast.LENGTH_LONG).show();
                    finish(); // Fecha a activity e volta para a tela principal
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(ProvisioningActivity.this, "Falha ao enviar comando: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}