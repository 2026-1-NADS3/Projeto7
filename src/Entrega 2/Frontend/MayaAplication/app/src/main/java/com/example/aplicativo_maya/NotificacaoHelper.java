package com.example.aplicativo_maya;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificacaoHelper {

    private static final String CANAL_ID = "maya_chat_local";

    public static void disparar(Context context, String tituloChat, String mensagem, String ticketId) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ID,
                    "Mensagens do Chat",
                    NotificationManager.IMPORTANCE_HIGH
            );
            canal.enableVibration(true);
            canal.setLightColor(Color.parseColor("#2EC4B6"));
            manager.createNotificationChannel(canal);
        }

        Intent intent = new Intent(context, ChatTicketActivity.class);
        intent.putExtra("TICKET_ID", ticketId);
        intent.putExtra("TITULO_CHAT", tituloChat);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_ID)
                .setSmallIcon(R.drawable.chat)
                .setContentTitle(tituloChat != null ? tituloChat : "Nova mensagem")
                .setContentText(mensagem)
                .setColor(Color.parseColor("#2EC4B6"))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent);

        int notificationId = ticketId != null ? ticketId.hashCode() : (int) System.currentTimeMillis();
        manager.notify(notificationId, builder.build());
    }

    public static void dispararNotificacaoRotina(Context context, String nomeRotina, int rotinaId) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String CANAL_ROTINA_ID = "maya_rotina_local";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ROTINA_ID,
                    "Novas Rotinas",
                    NotificationManager.IMPORTANCE_HIGH
            );
            canal.enableVibration(true);
            canal.setLightColor(Color.parseColor("#32C0D2"));
            manager.createNotificationChannel(canal);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("ABRIR_ABA_EXERCICIOS", true);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                rotinaId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_ROTINA_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Nova Rotina Disponível!")
                .setContentText("A clínica prescreveu: " + nomeRotina)
                .setColor(Color.parseColor("#32C0D2"))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent);

        manager.notify(rotinaId, builder.build());
    }
}