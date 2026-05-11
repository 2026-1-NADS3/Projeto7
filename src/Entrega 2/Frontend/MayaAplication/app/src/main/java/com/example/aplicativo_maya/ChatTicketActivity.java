package com.example.aplicativo_maya;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatTicketActivity extends AppCompatActivity {

    private String ticketId;
    private String tituloChat;
    private LinearLayout llMensagens;
    private EditText etMensagem;
    private ScrollView scrollChat;
    private TextView tvTituloChat;
    private DatabaseReference ticketRef;
    private ValueEventListener mensagensListener;
    public static String ticketAbertoAtual = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_ticket);


        ticketId = getIntent().getStringExtra("TICKET_ID");
        tituloChat = getIntent().getStringExtra("TITULO_CHAT");

        llMensagens = findViewById(R.id.llMensagens);
        etMensagem = findViewById(R.id.etMensagem);
        scrollChat = findViewById(R.id.scrollChat);
        tvTituloChat = findViewById(R.id.tvTituloChat);
        View header = findViewById(R.id.headerChat);
        View bottomBar = findViewById(R.id.bottomBar);

        if(tituloChat != null) tvTituloChat.setText(tituloChat);
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootChatLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomPadding = Math.max(systemBars.bottom, ime.bottom);
            header.setPadding(header.getPaddingLeft(), systemBars.top, header.getPaddingRight(), header.getPaddingBottom());
            bottomBar.setPadding(bottomBar.getPaddingLeft(), bottomBar.getPaddingTop(), bottomBar.getPaddingRight(), bottomPadding);
            if (ime.bottom > 0) {
                scrollChat.postDelayed(() -> scrollChat.fullScroll(View.FOCUS_DOWN), 100);
            }
            return insets;
        });

        findViewById(R.id.btnEnviar).setOnClickListener(v -> {
            String texto = etMensagem.getText().toString().trim();
            if (!texto.isEmpty()) {
                enviarMensagem(texto);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (mensagensListener != null && ticketRef != null) {
            ticketRef.child("mensagens").removeEventListener(mensagensListener);
        }

        ticketId = intent.getStringExtra("TICKET_ID");
        tituloChat = intent.getStringExtra("TITULO_CHAT");
        tvTituloChat.setText(tituloChat);

    }

    @Override
    protected void onResume() {
        super.onResume();
        ticketAbertoAtual = ticketId;

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (ticketId != null) {
            nm.cancel(ticketId.hashCode());
        }

        ticketRef = FirebaseDatabase.getInstance().getReference("chats").child(ticketId);

        ticketRef.child("naoLidasPaciente").setValue(0);

        mensagensListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                llMensagens.removeAllViews();
                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    String remetente = msgSnap.child("remetente").getValue(String.class);
                    String texto = msgSnap.child("texto").getValue(String.class);
                    String horario = msgSnap.child("horario").getValue(String.class);
                    adicionarBalaoNaTela(remetente, texto, horario);
                }
                scrollChat.postDelayed(() -> scrollChat.fullScroll(View.FOCUS_DOWN), 100);
                ticketRef.child("naoLidasPaciente").setValue(0);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        ticketRef.child("mensagens").addValueEventListener(mensagensListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ticketAbertoAtual = "";
        if (mensagensListener != null) {
            ticketRef.child("mensagens").removeEventListener(mensagensListener);
        }
    }

    private void enviarMensagem(String texto) {
        String horaAtual = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        DatabaseReference novaMsgRef = ticketRef.child("mensagens").push();
        novaMsgRef.child("remetente").setValue("paciente");
        novaMsgRef.child("texto").setValue(texto);
        novaMsgRef.child("horario").setValue(horaAtual);

        ticketRef.child("naoLidasAdmin").get().addOnSuccessListener(dataSnapshot -> {
            int atual = dataSnapshot.exists() ? dataSnapshot.getValue(Integer.class) : 0;
            ticketRef.child("naoLidasAdmin").setValue(atual + 1);
        });
        etMensagem.setText("");
    }

    private void adicionarBalaoNaTela(String remetente, String texto, String horario) {
        LinearLayout container = new LinearLayout(this);
        container.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        container.setOrientation(LinearLayout.VERTICAL);

        LinearLayout balao = new LinearLayout(this);
        balao.setOrientation(LinearLayout.VERTICAL);
        balao.setPadding(40, 24, 40, 24);

        LinearLayout.LayoutParams paramsBalao = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paramsBalao.setMargins(0, 8, 0, 8);

        TextView tvTexto = new TextView(this);
        tvTexto.setText(texto);
        tvTexto.setTextSize(15f);

        TextView tvHora = new TextView(this);
        tvHora.setText(horario);
        tvHora.setTextSize(10f);
        tvHora.setPadding(0, 8, 0, 0);

        GradientDrawable bgPaciente = new GradientDrawable();
        bgPaciente.setColor(Color.parseColor("#2EC4B6"));
        bgPaciente.setCornerRadii(new float[]{40, 40, 40, 40, 0, 0, 40, 40});

        GradientDrawable bgClinica = new GradientDrawable();
        bgClinica.setColor(Color.parseColor("#E5E7EB"));
        bgClinica.setCornerRadii(new float[]{40, 40, 40, 40, 40, 40, 0, 0});

        if ("paciente".equalsIgnoreCase(remetente)) {
            container.setGravity(Gravity.END);
            balao.setBackground(bgPaciente);
            tvTexto.setTextColor(Color.WHITE);
            tvHora.setTextColor(Color.parseColor("#E0E0E0"));
            tvHora.setGravity(Gravity.END);
        } else {
            container.setGravity(Gravity.START);
            balao.setBackground(bgClinica);
            tvTexto.setTextColor(Color.parseColor("#111827"));
            tvHora.setTextColor(Color.parseColor("#6B7280"));
            tvHora.setGravity(Gravity.START);
        }

        balao.setLayoutParams(paramsBalao);
        balao.addView(tvTexto);
        balao.addView(tvHora);
        container.addView(balao);
        llMensagens.addView(container);
    }
}