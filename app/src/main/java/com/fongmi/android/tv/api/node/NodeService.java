package com.fongmi.android.tv.api.node;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;

import com.github.catvod.Runtime;
import com.github.catvod.runtime.NodeRuntimeApi;
import com.github.catvod.utils.RuntimeModule;

public final class NodeService extends Service {

    private final IBinder binder = new Binder();
    private boolean started;

    @Override
    public void onCreate() {
        super.onCreate();
        Runtime.set(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        start(intent);
        return START_NOT_STICKY;
    }

    private void start(Intent intent) {
        if (intent != null && NodeRuntimeApi.SERVICE_ACTION.equals(intent.getAction()) && !started) {
            started = true;
            RuntimeModule.NODEJS.entry(NodeRuntimeApi.class).startProcess(this, intent);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        start(intent);
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Process.killProcess(Process.myPid());
    }
}
