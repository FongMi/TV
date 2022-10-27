package com.tvbus.engine;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class TVService extends Service {

    private TVCore tvCore;

    public static void start(Context context) {
        try {
            context.startService(new Intent(context, TVService.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stop(Context context) {
        try {
            context.stopService(new Intent(context, TVService.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        tvCore = TVCore.getInstance();
        tvCore.setServPort(0);
        tvCore.setPlayPort(8902);
        tvCore.setRunningMode(1);
        tvCore.init(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tvCore.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
