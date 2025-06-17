package com.example.espzera;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonCollect = findViewById(R.id.button_collect_data);
        Button buttonProvision = findViewById(R.id.button_provision);
        Button buttonCadastros = findViewById(R.id.button_cadastros);
        // BOTÃO ADICIONADO
        Button buttonViewData = findViewById(R.id.button_view_data);

        buttonCollect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CollectionActivity.class);
                startActivity(intent);
            }
        });

        buttonProvision.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ProvisioningActivity.class);
                startActivity(intent);
            }
        });

        buttonCadastros.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CadastrosActivity.class);
                startActivity(intent);
            }
        });

        // AÇÃO DO BOTÃO ADICIONADO
        buttonViewData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ViewDataActivity.class);
                startActivity(intent);
            }
        });
    }
}