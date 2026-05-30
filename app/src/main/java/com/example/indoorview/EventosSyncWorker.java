package com.example.indoorview;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.FirebaseApp;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class EventosSyncWorker extends Worker {

    private static final String TAG = "EVENTOS_SYNC_WORKER";
    private static final String CHANNEL_ID = "canal_eventos";
    private static final String CHANNEL_NAME = "Nuevos Eventos";
    private static final int NOTIF_ID = 1001;
    private static final String PREFS_NAME = "sync_prefs";
    private static final String KEY_ULTIMA_SYNC = "ultima_sync_eventos";

    public EventosSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "🔄 Iniciando sincronización de eventos en background...");

        Context context = getApplicationContext();

        // Asegurar que Firebase esté inicializado
        try {
            FirebaseApp.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "❌ Firebase no inicializado, inicializando...");
            FirebaseApp.initializeApp(context);
        }

        Database db = new Database(context);
        FirebaseHelper firebaseHelper = new FirebaseHelper();
        SyncManager syncManager = new SyncManager(context, db, firebaseHelper);

        // Contar eventos ANTES
        int eventosAntes = db.contarEventos();
        Log.d(TAG, "📊 Eventos antes de sync: " + eventosAntes);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] exito = {false};

        // ✅ Misma lógica que SyncWorker que funciona
        syncManager.setSyncListener(new SyncManager.SyncListener() {
            @Override
            public void onComplete(String message) {
                Log.d(TAG, "✅ Sync completada: " + message);
                exito[0] = true;
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Error en sync: " + error);
                latch.countDown();
            }

            @Override
            public void onProgress(String message, int progress, int total) {
                Log.d(TAG, "⏳ " + message + " (" + progress + "/" + total + ")");
            }
        });

        // ✅ Lanzar sync de eventos igual que SyncWorker lanza syncAllMapWithClean
        syncManager.syncAllEventosWithClean(new SyncManager.SyncCallback() {
            @Override
            public void onSyncComplete() {
                Log.d(TAG, "✅ syncAllEventosWithClean completado");
                exito[0] = true;
                latch.countDown();
            }

            @Override
            public void onSyncError(String error) {
                Log.e(TAG, "❌ syncAllEventosWithClean error: " + error);
                latch.countDown();
            }
        });

        try {
            boolean terminoATiempo = latch.await(2, TimeUnit.MINUTES);
            if (!terminoATiempo) {
                Log.w(TAG, "⚠️ Timeout: la sync de eventos tardó demasiado");
                return Result.retry();
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "❌ Sync interrumpida: " + e.getMessage());
            return Result.retry();
        }

        if (exito[0]) {
            // Contar eventos DESPUÉS
            int eventosDespues = db.contarEventos();
            int nuevos = eventosDespues - eventosAntes;

            Log.d(TAG, "📊 Antes: " + eventosAntes + " | Después: " + eventosDespues + " | Nuevos: " + nuevos);

            if (nuevos > 0) {
                enviarNotificacion(context, nuevos);
            }

            guardarTimestampSync(context);
            return Result.success();
        }

        return Result.retry();
    }

    private void enviarNotificacion(Context context, int cantidadNuevos) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notificaciones de nuevos eventos");
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String titulo = cantidadNuevos == 1
                ? "¡Hay 1 nuevo evento!"
                : "¡Hay " + cantidadNuevos + " nuevos eventos!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(com.mapbox.maps.R.drawable.mapbox_indoor_selector_building)
                .setContentTitle(titulo)
                .setContentText("Toca para ver los eventos disponibles")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify(NOTIF_ID, builder.build());
        Log.d(TAG, "🔔 Notificación enviada: " + titulo);
    }

    private void guardarTimestampSync(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_ULTIMA_SYNC, System.currentTimeMillis()).apply();
    }
}