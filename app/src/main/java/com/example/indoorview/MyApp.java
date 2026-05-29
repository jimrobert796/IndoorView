package com.example.indoorview;

import android.app.Application;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class MyApp extends Application {


    ///  ENCARGADA DE SINCRONIZACION BACKGROUND Y NOTIFICACIONES

    @Override
    public void onCreate() {
        super.onCreate();
        programarSyncMapa();
        programarSyncEventos();
    }

    private void programarSyncMapa() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest =
                new PeriodicWorkRequest.Builder(MapaSyncWorker.class, 6, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "sync_mapa_periodica",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );

        Log.d("MY_APP", "✅ Sync periódica programada");
    }

    private void programarSyncEventos() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest =
                new PeriodicWorkRequest.Builder(EventosSyncWorker.class, 1, TimeUnit.DAYS)
                        .setConstraints(constraints)
                        //.setInitialDelay(1, TimeUnit.DAYS) // primera vez al día siguiente
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "sync_eventos_diaria",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );

        Log.d("MY_APP", "✅ Sync de eventos programada cada 24 horas");
    }


}