package com.example.aplicativo_maya;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class AdminSimuladorActivity extends AppCompatActivity {

    private List<String> listaIdsTickets = new ArrayList<>();
    private List<String> listaNomesTickets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        TextView label = new TextView(this);
        label.setText("Selecione o Chat (Paciente):");
        label.setPadding(0, 0, 0, 10);

        Spinner spinnerChats = new Spinner(this);
        carregarChatsParaGaveta(spinnerChats);

        EditText etResposta = new EditText(this);
        etResposta.setHint("Digite a mensagem de resposta...");

        Button btnEnviar = new Button(this);
        btnEnviar.setText("Enviar Mensagem");

        layout.addView(label);
        layout.addView(spinnerChats);
        layout.addView(etResposta);
        layout.addView(btnEnviar);
        setContentView(layout);

        criarCanalNotificacao();

        btnEnviar.setOnClickListener(v -> {

            int posicaoSelecionada = spinnerChats.getSelectedItemPosition();
            String idSelecionado = listaIdsTickets.get(posicaoSelecionada);
            String tituloChat = listaNomesTickets.get(posicaoSelecionada);
            String resposta = etResposta.getText().toString();

            if (resposta.isEmpty()) return;

            try {
                ChatDatabase.adicionarMensagem(this, idSelecionado, "admin", resposta);

                JSONObject ticket = ChatDatabase.getTicketById(this, idSelecionado);

                int naoLidas = ticket.optInt("naoLidas", 0);

                if (naoLidas > 0) {
                    String tituloResumido = tituloChat.length() > 15 ? tituloChat.substring(0, 15) + "..." : tituloChat;
                    enviarNotificacao(tituloResumido, naoLidas);
                }
                finish();
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void carregarChatsParaGaveta(Spinner spinner) {
        try {
            JSONArray tickets = ChatDatabase.getTickets(this);
            for (int i = 0; i < tickets.length(); i++) {
                JSONObject ticket = tickets.getJSONObject(i);
                listaIdsTickets.add(ticket.getString("id"));
                listaNomesTickets.add(ticket.getString("titulo"));
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, listaNomesTickets);
            spinner.setAdapter(adapter);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void criarCanalNotificacao() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("admin_chat", "Chat", NotificationManager.IMPORTANCE_HIGH);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void enviarNotificacao(String tituloResumido, int numMensagens) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "admin_chat")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(tituloResumido)
                .setContentText("(" + numMensagens + " mensagens não lidas)")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this).notify(2, builder.build());
    }
}