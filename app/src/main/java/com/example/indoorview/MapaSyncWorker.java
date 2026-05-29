package com.example.indoorview;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MapaSyncWorker extends Worker {

    private static final String TAG = "SYNC_WORKER";
    private static final String PREFS_NAME = "sync_prefs";
    private static final String KEY_ULTIMA_SYNC = "ultima_sync_mapa";

    public MapaSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "🔄 Iniciando sincronización en background...");

        Context context = getApplicationContext();
        Database db = new Database(context);
        FirebaseHelper firebaseHelper = new FirebaseHelper();
        SyncManager syncManager = new SyncManager(context, db, firebaseHelper);

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] exito = {false};

        syncManager.setSyncListener(new SyncManager.SyncListener() {
            @Override
            public void onComplete(String message) {
                Log.d(TAG, "✅ Sync completada: " + message);
                guardarTimestampSync(context);
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

        syncManager.syncAllMapWithClean();

        try {
            // Espera máximo 2 minutos (ajusta según cuántas imágenes descargues)
            boolean terminoATiempo = latch.await(2, TimeUnit.MINUTES);
            if (!terminoATiempo) {
                Log.w(TAG, "⚠️ Timeout: la sync tardó demasiado");
                return Result.retry();
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "❌ Sync interrumpida: " + e.getMessage());
            return Result.retry();
        }

        return exito[0] ? Result.success() : Result.retry();
    }

    // Guarda la hora de la última sync exitosa
    private void guardarTimestampSync(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_ULTIMA_SYNC, System.currentTimeMillis()).apply();
    }
}