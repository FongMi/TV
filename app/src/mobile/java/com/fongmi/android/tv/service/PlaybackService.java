package com.fongmi.android.tv.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.Notify;

public class PlaybackService extends Service {

    public static void start() {
        ContextCompat.startForegroundService(App.get(), new Intent(App.get(), PlaybackService.class));
    }

    public static void stop() {
        App.get().stopService(new Intent(App.get(), PlaybackService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Notify.DEFAULT).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setSmallIcon(R.drawable.ic_logo);
        startForeground(9527, builder.build());
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
