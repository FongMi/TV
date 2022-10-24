package com.forcetech.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.forcetech.Port;
import com.forcetech.android.ForceTV;

public class ForceService extends Service {

    private ForceTV forceTV;

    @Override
    public IBinder onBind(Intent intent) {
        forceTV = new ForceTV();
        forceTV.start("forcetv", Port.FORCE);
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (forceTV != null) forceTV.stop();
        return super.onUnbind(intent);
    }
}
