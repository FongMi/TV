package com.gsoft.mitv;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.anymediacloud.iptv.standard.ForceTV;
import com.forcetech.Port;

public class MainActivity extends Service {

    private final IBinder binder = new LocalBinder();
    private ForceTV forceTV;

    public class LocalBinder extends Binder {
        MainActivity getService() {
            // Return this instance of LocalService so clients can call public methods
            return MainActivity.this;
        }
    }

    static {
        System.loadLibrary("mitv");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        loadLibrary(1);
    }

    @Override
    public IBinder onBind(Intent intent) {
        forceTV = new ForceTV();
        forceTV.start(Port.MTV);
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (forceTV != null) forceTV.stop();
        return super.onUnbind(intent);
    }

    private native void loadLibrary(int type);
}
