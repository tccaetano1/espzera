package com.example.espzera; // ATENÇÃO: Verifique se o nome do seu pacote é 'com.example.espzera'

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa os botões do menu principal
        Button buttonLeituras = findViewById(R.id.button_leituras);
        Button buttonCadastros = findViewById(R.id.button_cadastros);
        Button buttonConfiguracoes = findViewById(R.id.button_configuracoes);

        // Define OnClickListener para o botão Leituras
        buttonLeituras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Inicia a LeituraActivity
                Intent intent = new Intent(MainActivity.this, LeituraActivity.class);
                startActivity(intent);
            }
        });

        // Define OnClickListener para o botão Cadastros
        buttonCadastros.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Inicia a CadastrosActivity
                Intent intent = new Intent(MainActivity.this, CadastrosActivity.class);
                startActivity(intent);
            }
        });

        // Define OnClickListener para o botão Configurações
        buttonConfiguracoes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Configurações clicado!", Toast.LENGTH_SHORT).show();
                // Você normalmente iniciaria uma nova atividade aqui para Configurações
            }
        });
    }
}
