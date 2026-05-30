package com.example.indoorview;

import android.app.Application;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MyApp extends Application {


    ///  ENCARGADA DE SINCRONIZACION BACKGROUND Y NOTIFICACIONES

    @Override
    public void onCreate() {
        super.onCreate();
        programarSyncMapa();
        programarSyncEventos();
        programarRecordatorioEventosManana();
    }

    // Se actualiza cada 6 hrs automaticamente
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

        Log.d("MY_APP", "Sync periódica programada");
    }

    // En este caso lo hace cada 8 hrs o unas 3 veces aprox por dia
    private void programarSyncEventos() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest =
                new PeriodicWorkRequest.Builder(EventosSyncWorker.class, 8, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "sync_eventos_diaria",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );

        Log.d("MY_APP", "Sync de eventos programada cada 8 horas");
    }

    // Cada 15 minutos para pruebas
    /*
    private void programarRecordatorioEventosManana() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest reminderRequest =
                new PeriodicWorkRequest.Builder(EventosTommorowWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "recordatorio_eventos_manana",
                ExistingPeriodicWorkPolicy.REPLACE, // REPLACE para que aplique inmediatamente
                reminderRequest
        );

        Log.d("MY_APP", "Recordatorio en modo PRUEBA - cada 15 minutos");
    }

     */

    // UN DIA ANTES a las 3 de la tarde las notificaciones
    private void programarRecordatorioEventosManana() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Calcular delay hasta las 8:00 PM de hoy (hora ideal para notificar eventos de mañana)
        long delayInicial = calcularDelayHasta(15, 0); // 20:00 horas

        PeriodicWorkRequest reminderRequest =
                new PeriodicWorkRequest.Builder(EventosTommorowWorker.class, 1, TimeUnit.DAYS)
                        .setConstraints(constraints)
                        .setInitialDelay(delayInicial, TimeUnit.MILLISECONDS)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "recordatorio_eventos_manana",
                ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest
        );

        Log.d("MY_APP", " Recordatorio de eventos de mañana programado para las 3:00 PM");
    }


    // Calcular cuántos milisegundos faltan para una hora específica
    private long calcularDelayHasta(int hora, int minuto) {
        Calendar objetivo = Calendar.getInstance();
        objetivo.set(Calendar.HOUR_OF_DAY, hora);
        objetivo.set(Calendar.MINUTE, minuto);
        objetivo.set(Calendar.SECOND, 0);
        objetivo.set(Calendar.MILLISECOND, 0);

        long ahora = System.currentTimeMillis();
        long objetivo_ms = objetivo.getTimeInMillis();

        // Si ya pasó la hora de hoy, programar para mañana
        if (objetivo_ms <= ahora) {
            objetivo_ms += TimeUnit.DAYS.toMillis(1);
        }

        long delay = objetivo_ms - ahora;
        Log.d("MY_APP", "Delay calculado: " + (delay / 1000 / 60) + " minutos");
        return delay;
    }


}