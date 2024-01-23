package com.gsoft.mitv;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.anymediacloud.iptv.standard.ForceTV;
import com.forcetech.Util;
import com.github.catvod.utils.Asset;
import com.github.catvod.utils.Path;

import java.io.File;

public class MainActivity extends Service {

    private ForceTV forceTV;
    private IBinder binder;

    public MainActivity() {
        try {
            checkLibrary();
            System.loadLibrary("mitv");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void checkLibrary() {
        String name = "libmitv.so";
        File file = Path.cache(name);
        if (!file.exists()) Path.copy(Asset.open(name), file);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            binder = new LocalBinder();
            loadLibrary(1);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        forceTV = new ForceTV();
        forceTV.start(Util.MTV);
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (forceTV != null) forceTV.stop();
        return super.onUnbind(intent);
    }

    private native void loadLibrary(int type);
}
