package com.example.lab16_dev_android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvTemps;
    private Button btnStart, btnStop;
    private ChronometreService chronometreService;
    private boolean isBound = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBound && chronometreService != null) {
                int sec = chronometreService.getSecondes();
                tvTemps.setText(formatTemps(sec));
            }
            handler.postDelayed(this, 1000);
        }
    };

    private String formatTemps(int sec) {
        int minutes = sec / 60;
        int secondesRest = sec % 60;
        return String.format("%02d:%02d", minutes, secondesRest);
    }

    // Connexion au service (Bound Service)
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ChronometreService.LocalBinder binder = (ChronometreService.LocalBinder) service;
            chronometreService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTemps = findViewById(R.id.tvTemps);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);

        handler.post(updateTimeRunnable);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService();
            }
        });
    }

    private void startService() {
        Intent intent = new Intent(this, ChronometreService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private void stopService() {
        Intent intent = new Intent(this, ChronometreService.class);
        intent.setAction("STOP");
        stopService(intent);

        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        tvTemps.setText("00:00");
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(updateTimeRunnable);
        if (isBound) {
            unbindService(connection);
        }
        super.onDestroy();
    }
}
