package com.fongmi.android.tv.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.server.Server;

public class NanoService extends Service {

    public static void start() {
        ContextCompat.startForegroundService(App.get(), new Intent(App.get(), NanoService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Server.get().start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return START_STICKY;
        NotificationChannel channel = new NotificationChannel(BuildConfig.FLAVOR_mode, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        Notification notify = new Notification.Builder(this, BuildConfig.FLAVOR_mode).build();
        startForeground(1, notify);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Server.get().stop();
    }
}
