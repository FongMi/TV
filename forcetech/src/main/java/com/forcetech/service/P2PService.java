package com.forcetech.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.forcetech.Util;
import com.forcetech.android.ForceTV;
import com.gsoft.mitv.LocalBinder;

public class P2PService extends Service {

    private ForceTV forceTV;
    private IBinder binder;

    @Override
    public void onCreate() {
        super.onCreate();
        binder = new LocalBinder();
    }

    @Override
    public IBinder onBind(Intent intent) {
        forceTV = new ForceTV();
        forceTV.start(intent.getStringExtra("path"), Util.P2P);
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (forceTV != null) forceTV.stop();
        return super.onUnbind(intent);
    }
}
