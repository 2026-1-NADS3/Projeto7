package com.example.aplicativo_maya;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView; // IMPORTANTE: Adicionado a importação do TextView
import androidx.appcompat.app.AppCompatActivity;

public class Login_Maya extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_maya);

        Button botaoMudarTela = findViewById(R.id.btnLogar);
        botaoMudarTela.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login_Maya.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        TextView textoCalcular = findViewById(R.id.txtCadastrar);
        textoCalcular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intentTexto = new Intent(Login_Maya.this, cadastro.class);
                startActivity(intentTexto);
            }
        });
    }
}