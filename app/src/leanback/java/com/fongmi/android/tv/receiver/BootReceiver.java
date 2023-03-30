package com.fongmi.android.tv.receiver;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fongmi.android.tv.service.NanoService;

public class BootReceiver extends BroadcastReceiver {

    @Override
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    public void onReceive(Context context, Intent intent) {
        NanoService.start();
    }
}
