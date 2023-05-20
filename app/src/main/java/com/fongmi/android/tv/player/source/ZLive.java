package com.fongmi.android.tv.player.source;

import com.fongmi.android.tv.utils.FileUtil;
import com.github.catvod.net.OkHttp;

public class ZLive {

    private final String BASE = "http://127.0.0.1:6677/stream/";
    private boolean init;

    private static class Loader {
        static volatile ZLive INSTANCE = new ZLive();
    }

    public static ZLive get() {
        return Loader.INSTANCE;
    }

    public void init() {
        com.east.android.zlive.ZLive.INSTANCE.OnLiveStart(6677);
        init = true;
    }

    private String getLive(String uuid) {
        return BASE + "live?uuid=" + uuid;
    }

    private String getOpen(String uuid) {
        return BASE + "open?uuid=" + uuid;
    }

    public String fetch(String url) {
        try {
            if (!init) init();
            String[] split = url.split("/");
            String server = split[2];
            String uuid = split[3];
            String param = "&group=5850&mac=00:00:00:00:00:00&dir=";
            String result = getLive(uuid) + "&server=" + server + param + FileUtil.getCachePath();
            OkHttp.newCall(getOpen(uuid)).execute();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return url;
        }
    }

    public void stop() {
        try {
            if (init) com.east.android.zlive.ZLive.INSTANCE.OnLiveStop();
            init = false;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
