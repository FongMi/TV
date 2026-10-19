package com.fongmi.android.tv.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.content.ContextCompat;

import com.github.catvod.utils.RuntimeModule;

import java.util.Arrays;

public class RuntimeReceiver extends BroadcastReceiver {

    public static void register(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        ContextCompat.registerReceiver(context, new RuntimeReceiver(), filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getData() == null) return;
        String packageName = intent.getData().getSchemeSpecificPart();
        if (isRuntimePackage(packageName)) RuntimeModule.refresh();
    }

    private boolean isRuntimePackage(String packageName) {
        return Arrays.stream(RuntimeModule.values()).anyMatch(module -> module.getPackageName().equals(packageName));
    }
}
