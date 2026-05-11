package com.example.aplicativo_maya;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatDatabase {
    private static final String FILE_NAME = "tickets_db.json";

    public static JSONArray getTickets(Context context) {
        try {
            FileInputStream fis = context.openFileInput(FILE_NAME);
            byte[] bytes = new byte[fis.available()];
            fis.read(bytes);
            fis.close();
            return new JSONArray(new String(bytes));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static void saveTickets(Context context, JSONArray tickets) {
        try {
            FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            fos.write(tickets.toString().getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String criarTicket(Context context, String titulo, String assunto) {
        try {
            JSONArray tickets = getTickets(context);
            String ticketId = String.valueOf(System.currentTimeMillis());
            String dataAtual = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date());
            String horaAtual = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

            JSONObject novoTicket = new JSONObject();
            novoTicket.put("id", ticketId);
            novoTicket.put("titulo", titulo);
            novoTicket.put("data", dataAtual);
            novoTicket.put("naoLidas", 0);

            JSONArray mensagens = new JSONArray();
            JSONObject primeiraMsg = new JSONObject();
            primeiraMsg.put("remetente", "paciente");
            primeiraMsg.put("texto", assunto);
            primeiraMsg.put("horario", horaAtual);
            mensagens.put(primeiraMsg);

            novoTicket.put("mensagens", mensagens);
            tickets.put(novoTicket);

            saveTickets(context, tickets);
            return ticketId;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void adicionarMensagem(Context context, String ticketId, String remetente, String texto) {
        try {
            JSONArray tickets = getTickets(context);
            String horaAtual = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

            for (int i = 0; i < tickets.length(); i++) {
                JSONObject ticket = tickets.getJSONObject(i);
                if (ticket.getString("id").equals(ticketId)) {
                    JSONObject novaMsg = new JSONObject();
                    novaMsg.put("remetente", remetente);
                    novaMsg.put("texto", texto);
                    novaMsg.put("horario", horaAtual);
                    ticket.getJSONArray("mensagens").put(novaMsg);

                    // LÓGICA DE MENSAGENS NÃO LIDAS
                    if (remetente.equals("admin")) {
                        // Se a tela atual NÃO for a desse chat, adiciona +1 nas não lidas
                        if (!ticketId.equals(ChatTicketActivity.ticketAbertoAtual)) {
                            int naoLidas = ticket.optInt("naoLidas", 0);
                            ticket.put("naoLidas", naoLidas + 1);
                        }
                    }
                    break;
                }
            }
            saveTickets(context, tickets);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // NOVO MÉTODO: Zera as mensagens não lidas ao abrir o chat
    public static void resetarNaoLidas(Context context, String ticketId) {
        try {
            JSONArray tickets = getTickets(context);
            for (int i = 0; i < tickets.length(); i++) {
                JSONObject ticket = tickets.getJSONObject(i);
                if (ticket.getString("id").equals(ticketId)) {
                    ticket.put("naoLidas", 0); // Zera o contador
                    break;
                }
            }
            saveTickets(context, tickets);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JSONObject getTicketById(Context context, String ticketId) {
        try {
            JSONArray tickets = getTickets(context);
            for (int i = 0; i < tickets.length(); i++) {
                JSONObject ticket = tickets.getJSONObject(i);
                if (ticket.getString("id").equals(ticketId)) return ticket;
            }
        } catch (Exception e) {}
        return null;
    }
}