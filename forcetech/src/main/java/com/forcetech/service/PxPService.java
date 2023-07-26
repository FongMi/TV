package com.forcetech.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.forcetech.android.ForceTV;
import com.gsoft.mitv.LocalBinder;

public abstract class PxPService extends Service {

    private ForceTV forceTV;
    private IBinder binder;

    public abstract int getPort();

    @Override
    public void onCreate() {
        super.onCreate();
        binder = new LocalBinder();
    }

    @Override
    public IBinder onBind(Intent intent) {
        forceTV = new ForceTV();
        forceTV.start(intent.getStringExtra("scheme"), getPort());
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (forceTV != null) forceTV.stop();
        return super.onUnbind(intent);
    }
}