package com.example.aplicativo_maya;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.aplicativo_maya.api.AgendamentoResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DetalhesAgendamentoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhes_agendamento);

        // Receber o agendamento da Intent
        AgendamentoResponse agendamento = (AgendamentoResponse) getIntent().getSerializableExtra("agendamento");

        if (agendamento == null) {
            finish();
            return;
        }

        ImageButton btnVoltar = findViewById(R.id.btnVoltar);
        ImageView imgMapaMock = findViewById(R.id.imgMapaMock);
        View cardEndereco = findViewById(R.id.cardEndereco);
        TextView txtLocalNome = findViewById(R.id.txtLocalNome);
        TextView txtEnderecoCompleto = findViewById(R.id.txtEnderecoCompleto);
        TextView txtDataHoraDetalhe = findViewById(R.id.txtDataHoraDetalhe);
        TextView txtDescricaoConteudo = findViewById(R.id.txtDescricaoConteudo);
        TextView txtStatusTexto = findViewById(R.id.txtStatusTexto);
        View viewStatusDot = findViewById(R.id.viewStatusDot);

        btnVoltar.setOnClickListener(v -> finish());

        String localMock = "RPG - Maya Yoshiko Yamamoto";
        String enderecoMock = "Rua Rio Grande, 141 - sala 3 - RPG - Vila Mariana, São Paulo - SP, 04015-070";

        txtLocalNome.setText(localMock);
        txtEnderecoCompleto.setText(enderecoMock);
        View.OnClickListener mapClickListener = v -> {
            String mapUrl = "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(enderecoMock);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl));
            startActivity(intent);
        };

        imgMapaMock.setOnClickListener(mapClickListener);
        cardEndereco.setOnClickListener(mapClickListener);

        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = sdfInput.parse(agendamento.getDataHora());
            if (date != null) {
                SimpleDateFormat sdfOutput = new SimpleDateFormat("HH:mm - dd 'de' MMM. 'de' yyyy", new Locale("pt", "BR"));
                txtDataHoraDetalhe.setText(sdfOutput.format(date));
            }
        } catch (Exception e) {
            txtDataHoraDetalhe.setText(agendamento.getDataHora());
        }

        String obs = agendamento.getObservacao();
        if (obs == null || obs.trim().isEmpty()) {
            obs = "Nenhuma observação detalhada para este agendamento.";
        }
        txtDescricaoConteudo.setText(obs);

        String status = agendamento.getStatus();
        if (status == null) status = "AGENDADO";

        txtStatusTexto.setText(status.toUpperCase());

        if ("CONCLUIDO".equalsIgnoreCase(status)) {
            viewStatusDot.setBackgroundResource(R.drawable.dot_green);
        } else {
            viewStatusDot.setBackgroundResource(R.drawable.dot_red);
        }
    }
}