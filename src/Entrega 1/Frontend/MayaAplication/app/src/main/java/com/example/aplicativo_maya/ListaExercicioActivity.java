package com.example.aplicativo_maya;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

// Tela de detalhe — NÃO tem footer pois é uma tela "filha" aberta por cima
public class ListaExercicioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_exercicio);

        ImageButton btnComecar  = findViewById(R.id.btnComecar);
        ImageButton btnComecar1 = findViewById(R.id.btnComecar1);
        ImageButton btnComecar2 = findViewById(R.id.btnComecar2);

        View.OnClickListener goToExercicio = v -> {
            Intent intent = new Intent(this, ExercicioInActivity.class);
            startActivity(intent);
        };

        if (btnComecar  != null) btnComecar.setOnClickListener(goToExercicio);
        if (btnComecar1 != null) btnComecar1.setOnClickListener(goToExercicio);
        if (btnComecar2 != null) btnComecar2.setOnClickListener(goToExercicio);
    }
}