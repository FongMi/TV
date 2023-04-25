package com.fongmi.android.tv.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.fongmi.android.tv.utils.PiP;

public class PiPReceiver extends BroadcastReceiver {

    private final Listener listener;

    public PiPReceiver(Listener listener) {
        this.listener = listener;
    }

    public void register(Activity activity) {
        activity.registerReceiver(this, new IntentFilter(PiP.ACTION_MEDIA_CONTROL));
    }

    public void unregister(Activity activity) {
        try {
            activity.unregisterReceiver(this);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !intent.getAction().equals(PiP.ACTION_MEDIA_CONTROL)) return;
        int controlType = intent.getIntExtra(PiP.EXTRA_CONTROL_TYPE, 0);
        switch (controlType) {
            case PiP.CONTROL_TYPE_PLAY:
            case PiP.CONTROL_TYPE_PAUSE:
                listener.onControlPlay();
                break;
            case PiP.CONTROL_TYPE_NEXT:
                listener.onControlNext();
                break;
            case PiP.CONTROL_TYPE_PREV:
                listener.onControlPrev();
                break;
        }
    }

    public interface Listener {

        void onControlPlay();

        void onControlNext();

        void onControlPrev();
    }
}
