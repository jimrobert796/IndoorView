package com.example.indoorview;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.indoorview.models.Eventos;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventosTommorowWorker extends Worker {

    private static final String TAG = "EVENTOS_MANANA_WORKER";
    private static final String CHANNEL_ID = "canal_eventos_manana";
    private static final String CHANNEL_NAME = "Eventos de Mañana";
    private static final int NOTIF_ID = 2001;

    public EventosTommorowWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "🔍 Buscando eventos para mañana...");

        Context context = getApplicationContext();
        Database db = new Database(context);

        // Obtener la fecha de mañana en formato yyyy-MM-dd
        String fechaManana = obtenerFechaManana();
        Log.d(TAG, "📅 Fecha de mañana: " + fechaManana);

        // Buscar eventos que comiencen mañana
        List<Eventos> eventosManana = db.getEventosPorFecha(fechaManana);

        if (eventosManana == null || eventosManana.isEmpty()) {
            Log.d(TAG, "ℹ️ No hay eventos para mañana");
            return Result.success();
        }

        Log.d(TAG, "🎉 Encontrados " + eventosManana.size() + " eventos para mañana");

        // Enviar notificación
        enviarNotificacion(context, eventosManana);

        return Result.success();
    }

    private String obtenerFechaManana() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date manana = calendar.getTime();
        // Cambiado a dd/MM/yyyy para coincidir con el formato guardado en BD
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(manana);
    }

    private void enviarNotificacion(Context context, List<Eventos> eventos) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Recordatorio de eventos para mañana");
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (eventos.size() == 1) {
            // Un solo evento: una notificación
            Eventos e = eventos.get(0);
            String titulo = "Mañana: " + e.getNombre();
            String mensaje = "Comienza a las " + e.getHora_inicio() + ". ¡No te lo pierdas!";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(com.mapbox.maps.R.drawable.mapbox_indoor_selector_building)
                    .setContentTitle(titulo)
                    .setContentText(mensaje)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            manager.notify(NOTIF_ID, builder.build());
            Log.d(TAG, "🔔 Notificación enviada: " + titulo);

        } else {
            // Múltiples eventos: una notificación por cada uno con ID único
            for (int i = 0; i < eventos.size(); i++) {
                Eventos e = eventos.get(i);

                String titulo = "Mañana: " + e.getNombre();
                String mensaje = "Comienza a las " + e.getHora_inicio() + ". ¡No te lo pierdas!";

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.icon_mapa)
                        .setContentTitle(titulo)
                        .setContentText(mensaje)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

                // ID único por evento para que no se sobreescriban
                manager.notify(NOTIF_ID + i, builder.build());
                Log.d(TAG, "🔔 Notificación " + (i + 1) + " enviada: " + titulo);
            }
        }
    }
}