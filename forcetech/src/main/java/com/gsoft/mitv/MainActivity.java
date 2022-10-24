package com.gsoft.mitv;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.anymediacloud.iptv.standard.ForceTV;
import com.forcetech.Port;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class MainActivity extends Service {

    private static WeakReference<MainActivity> ref;
    private ForceTV forceTV;

    static {
        System.loadLibrary("mitv");
    }

    private native void loadLibrary(int i);

    public static void start() {
        ref.get().loadLibrary(1);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ref = new WeakReference<>(this);
        try {
            loadAsset();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        forceTV = new ForceTV();
        forceTV.start(Port.MTV);
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (forceTV != null) forceTV.stop();
        return super.onUnbind(intent);
    }

    private void loadAsset() throws Throwable {
        InputStream is = getAssets().open("libmitv.so");
        FileOutputStream fos = new FileOutputStream(getCacheDir() + "/libmitv.so");
        byte[] bytes = new byte[1024];
        while (true) {
            int read = is.read(bytes);
            if (read != -1) {
                fos.write(bytes, 0, read);
            } else {
                is.close();
                fos.flush();
                fos.close();
                return;
            }
        }
    }
}
