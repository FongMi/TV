package com.fongmi.android.tv.service;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public final class PlaybackServiceConnection extends AndroidViewModel implements ServiceConnection {

    private ServiceConnection listener;
    private ConnectedService connection;
    private boolean bound;

    public PlaybackServiceConnection(@NonNull Application application) {
        super(application);
    }

    public void attach(ServiceConnection listener) {
        this.listener = listener;
        if (connection != null) {
            listener.onServiceConnected(connection.name(), connection.binder());
        } else if (!bound) {
            Application application = getApplication();
            application.startService(new Intent(application, PlaybackService.class));
            Intent intent = new Intent(application, PlaybackService.class).setAction(PlaybackService.LOCAL_BIND_ACTION);
            application.bindService(intent, this, Context.BIND_AUTO_CREATE);
            bound = true;
        }
    }

    public void detach(ServiceConnection listener) {
        if (this.listener == listener) this.listener = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        connection = new ConnectedService(name, binder);
        if (listener != null) listener.onServiceConnected(name, binder);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        connection = null;
        if (listener != null) listener.onServiceDisconnected(name);
    }

    @Override
    protected void onCleared() {
        listener = null;
        connection = null;
        if (bound) {
            getApplication().unbindService(this);
            bound = false;
        }
    }

    private record ConnectedService(ComponentName name, IBinder binder) {}
}
