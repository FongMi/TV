package com.fongmi.android.tv.utils;

import android.content.pm.PackageManager;
import android.net.TrafficStats;

import com.fongmi.android.tv.App;

public class Traffic {

    private static final String UNIT_KB = " KB/s";
    private static final String UNIT_MB = " MB/s";
    private static long lastTotalRxBytes = 0;
    private static long lastTimeStamp = 0;

    public static String get() {
        long total = getTotalRxBytes(getUid());
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

    private static long getTotalRxBytes(int uid) {
        return TrafficStats.getUidRxBytes(uid) == TrafficStats.UNSUPPORTED ? 0 : (TrafficStats.getTotalRxBytes() / 1024);
    }

    private static int getUid() {
        try {
            return App.get().getPackageManager().getApplicationInfo(App.get().getPackageName(), PackageManager.GET_META_DATA).uid;
        } catch (Exception e) {
            return 0;
        }
    }
}
