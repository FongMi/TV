package com.fongmi.android.tv.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.utils.Utils;

public class ActionReceiver extends BroadcastReceiver {

    public static PendingIntent getPendingIntent(Context context, String type) {
        Intent intent = new Intent(App.get().getPackageName() + "." + type);
        intent.setComponent(new ComponentName(context, ActionReceiver.class));
        return PendingIntent.getBroadcast(context, 0, intent, Utils.getPendingFlag());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ActionEvent.send(intent.getAction().replace(context.getPackageName() + ".", ""));
    }
}
