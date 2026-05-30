package com.example.lab16_dev_android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChronometreService extends Service {

    // Binder pour permettre à l'Activity de se connecter au service
    private final IBinder binder = new LocalBinder();
    
    private int secondes = 0;                    // temps écoulé
    private boolean isRunning = false;           // état du chrono
    private ScheduledExecutorService executor;   // pour incrémenter chaque seconde
    private static final int NOTIFICATION_ID = 1001;
    private NotificationManager notificationManager;

    // Classe interne qui permet à l'Activity de récupérer l'instance du service
    public class LocalBinder extends Binder {
        public ChronometreService getService() {
            return ChronometreService.this;   // retourne l'instance du service
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // On récupère le NotificationManager (nécessaire pour créer et mettre à jour les notifications)
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        creerNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = (intent != null) ? intent.getAction() : null;

        // Action "STOP" permet d'arrêter proprement le service depuis l'Activity
        if ("STOP".equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Si le service n'est pas déjà lancé, on le démarre en mode Foreground
        if (!isRunning) {
            isRunning = true;
            startForeground(NOTIFICATION_ID, creerNotification());  // obligatoire depuis Android 8
            demarrerChronometre();
        }
        return START_STICKY;   // redémarre automatiquement si tué par le système
    }

    private void demarrerChronometre() {
        executor = Executors.newSingleThreadScheduledExecutor();
        // Toutes les 1 seconde : on incrémente et on met à jour la notification
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                secondes++;
                updateNotification();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void creerNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "chrono_channel",
                    "Chronomètre Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification creerNotification() {
        return new NotificationCompat.Builder(this, "chrono_channel")
                .setContentTitle("Chronomètre en cours")
                .setContentText("Temps : " + formatTemps(secondes))
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)                    // impossible de supprimer la notif
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, creerNotification());
    }

    public int getSecondes() {
        return secondes;
    }

    private String formatTemps(int sec) {
        int minutes = sec / 60;
        int secondesRest = sec % 60;
        return String.format("%02d:%02d", minutes, secondesRest);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;   // retourne le binder pour la connexion
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (executor != null) {
            executor.shutdown();
        }
        stopForeground(true);   // supprime la notification persistante
        super.onDestroy();
    }
}
