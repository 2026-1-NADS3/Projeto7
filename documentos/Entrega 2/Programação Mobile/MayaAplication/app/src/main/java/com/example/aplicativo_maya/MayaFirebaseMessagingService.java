package com.example.aplicativo_maya;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MayaFirebaseMessagingService extends FirebaseMessagingService {


    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM_TOKEN", "Token atualizado: " + token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String titulo = "Clínica Maya";
        String corpo = "Você tem uma nova mensagem.";
        String ticketId = null;
        String tituloChat = "Chat";


        if (remoteMessage.getData().size() > 0) {
            if (remoteMessage.getData().containsKey("title")) titulo = remoteMessage.getData().get("title");
            if (remoteMessage.getData().containsKey("body")) corpo = remoteMessage.getData().get("body");
            if (remoteMessage.getData().containsKey("ticketId")) ticketId = remoteMessage.getData().get("ticketId");
            if (remoteMessage.getData().containsKey("tituloChat")) tituloChat = remoteMessage.getData().get("tituloChat");
        }

        else if (remoteMessage.getNotification() != null) {
            titulo = remoteMessage.getNotification().getTitle();
            corpo = remoteMessage.getNotification().getBody();
        }

        if (ticketId != null && ticketId.equals(ChatTicketActivity.ticketAbertoAtual)) {
            return;
        }

        mostrarNotificacaoPush(titulo, corpo, ticketId, tituloChat);
    }

    private void mostrarNotificacaoPush(String titulo, String corpo, String ticketId, String tituloChat) {
        Intent intent;

        if (ticketId != null) {
            intent = new Intent(this, ChatTicketActivity.class);
            intent.putExtra("TICKET_ID", ticketId);
            intent.putExtra("TITULO_CHAT", tituloChat);
        } else {
            intent = new Intent(this, MainActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = "maya_urgent_channel_v4";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(titulo)
                .setContentText(corpo)
                .setColor(Color.parseColor("#2EC4B6"))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Mensagens e Alertas",
                    NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription("Notificações urgentes de mensagens e rotinas");
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setLightColor(Color.parseColor("#2EC4B6"));
            manager.createNotificationChannel(channel);
        }

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}