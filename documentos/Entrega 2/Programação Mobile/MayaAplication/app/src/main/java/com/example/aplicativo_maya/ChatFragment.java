package com.example.aplicativo_maya;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatFragment extends Fragment {

    private LinearLayout llTicketsContainer;
    private DatabaseReference chatsRef;
    private ValueEventListener ticketsListener;
    private Query ticketsQuery;
    private SessionManager session;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        llTicketsContainer = view.findViewById(R.id.llTicketsContainer);

        session  = new SessionManager(requireContext());
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        view.findViewById(R.id.btnNovoChat).setOnClickListener(v -> abrirPopupNovoTicket());

        escutarTicketsEmTempoReal();
        return view;
    }

    private void escutarTicketsEmTempoReal() {
        String pacienteId = session.getPacienteId();

        if (pacienteId.isEmpty()) {
            llTicketsContainer.removeAllViews();
            return;
        }

        ticketsQuery = chatsRef.orderByChild("pacienteId").equalTo(pacienteId);

        ticketsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;
                llTicketsContainer.removeAllViews();

                for (DataSnapshot ticketSnap : snapshot.getChildren()) {
                    String  id       = ticketSnap.child("id").getValue(String.class);
                    String  titulo   = ticketSnap.child("titulo").getValue(String.class);
                    String  data     = ticketSnap.child("data").getValue(String.class);
                    Integer naoLidas = ticketSnap.child("naoLidasPaciente").getValue(Integer.class);
                    if (naoLidas == null) naoLidas = 0;

                    View itemView = LayoutInflater.from(requireContext())
                            .inflate(R.layout.item_chat_ticket, llTicketsContainer, false);

                    ((TextView) itemView.findViewById(R.id.tvTituloItem)).setText(titulo);
                    ((TextView) itemView.findViewById(R.id.tvDataItem)).setText("aberto: " + data);

                    TextView tvBadge = itemView.findViewById(R.id.tvBadgeNaoLidas);
                    if (naoLidas > 0) {
                        tvBadge.setVisibility(View.VISIBLE);
                        tvBadge.setText(String.valueOf(naoLidas));
                    } else {
                        tvBadge.setVisibility(View.GONE);
                    }

                    final String finalId     = id;
                    final String finalTitulo = titulo;
                    itemView.setOnClickListener(v -> {
                        Intent intent = new Intent(requireContext(), ChatTicketActivity.class);
                        intent.putExtra("TICKET_ID", finalId);
                        intent.putExtra("TITULO_CHAT", finalTitulo);
                        startActivity(intent);
                    });

                    llTicketsContainer.addView(itemView, 0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        ticketsQuery.addValueEventListener(ticketsListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (ticketsQuery != null && ticketsListener != null) {
            ticketsQuery.removeEventListener(ticketsListener);
        }
    }

    private void abrirPopupNovoTicket() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_novo_ticket, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();

        EditText etTitulo    = dialogView.findViewById(R.id.etTituloTicket);
        EditText etAssunto   = dialogView.findViewById(R.id.etAssuntoTicket);
        Button   btnConcluir = dialogView.findViewById(R.id.btnConcluirTicket);
        Button   btnCancelar = dialogView.findViewById(R.id.btnCancelarTicket);

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnConcluir.setOnClickListener(v -> {
            String titulo  = etTitulo.getText().toString().trim();
            String assunto = etAssunto.getText().toString().trim();

            if (!titulo.isEmpty() && !assunto.isEmpty()) {
                DatabaseReference novoTicketRef = chatsRef.push();
                String ticketId  = novoTicketRef.getKey();
                String dataAtual = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date());
                String horaAtual = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

                novoTicketRef.child("id").setValue(ticketId);
                novoTicketRef.child("titulo").setValue(titulo);
                novoTicketRef.child("data").setValue(dataAtual);
                novoTicketRef.child("naoLidasAdmin").setValue(1);
                novoTicketRef.child("naoLidasPaciente").setValue(0);

                novoTicketRef.child("pacienteId").setValue(session.getPacienteId());
                novoTicketRef.child("pacienteNome").setValue(session.getPacienteNome());
                novoTicketRef.child("pacienteEmail").setValue(session.getPacienteEmail());

                DatabaseReference msgRef = novoTicketRef.child("mensagens").push();
                msgRef.child("remetente").setValue("paciente");
                msgRef.child("texto").setValue(assunto);
                msgRef.child("horario").setValue(horaAtual);

                FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        novoTicketRef.child("fcmToken").setValue(task.getResult());
                    }
                });

                dialog.dismiss();
                Intent intent = new Intent(requireContext(), ChatTicketActivity.class);
                intent.putExtra("TICKET_ID", ticketId);
                intent.putExtra("TITULO_CHAT", titulo);
                startActivity(intent);
            }
        });

        dialog.show();
    }
}