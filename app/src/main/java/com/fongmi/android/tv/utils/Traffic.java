package com.fongmi.android.tv.utils;

import android.net.TrafficStats;
import android.view.View;
import android.widget.TextView;

import com.fongmi.android.tv.App;

public class Traffic {

    private static final String UNIT_KB = " KB/s";
    private static final String UNIT_MB = " MB/s";
    private static long lastTotalRxBytes = 0;
    private static long lastTimeStamp = 0;

    public static void setSpeed(TextView view) {
        if (unsupported()) return;
        view.setText(getSpeed());
        view.setVisibility(View.VISIBLE);
    }

    private static boolean unsupported() {
        return TrafficStats.getUidRxBytes(App.get().getApplicationInfo().uid) == TrafficStats.UNSUPPORTED;
    }

    private static String getSpeed() {
        long total = TrafficStats.getTotalRxBytes() / 1024;
        long time = System.currentTimeMillis();
        long diff = (total - lastTotalRxBytes) * 1000;
        long speed = diff / Math.max(time - lastTimeStamp, 1);
        lastTimeStamp = time;
        lastTotalRxBytes = total;
        if (speed > 1000) return speed / 1000 + UNIT_MB;
        else return speed + UNIT_KB;
    }

    public static void reset() {
        lastTotalRxBytes = 0;
        lastTimeStamp = 0;
    }
}
