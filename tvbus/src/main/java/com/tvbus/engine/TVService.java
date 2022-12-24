package com.tvbus.engine;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class TVService extends Service {

    private TVCore tvcore;

    public static void start(Context context, String auth, String name, String pass, String broker) {
        try {
            Intent intent = new Intent(context, TVService.class);
            intent.putExtra("auth", auth);
            intent.putExtra("name", name);
            intent.putExtra("pass", pass);
            intent.putExtra("broker", broker);
            context.startService(intent);
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        tvcore = TVCore.getInstance().auth(intent.getStringExtra("auth")).name(intent.getStringExtra("name")).pass(intent.getStringExtra("pass")).broker(intent.getStringExtra("broker"));
        tvcore.serv(0).play(8902).mode(1);
        tvcore.init(this);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tvcore.stop();
        tvcore.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
