package com.fongmi.android.tv.player.extractor;

import com.fongmi.android.tv.player.Source;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;

public class ZLive implements Source.Extractor {

    private final String BASE = "http://127.0.0.1:6677/stream/";
    private boolean init;

    public void init() {
        //com.east.android.zlive.ZLive.INSTANCE.OnLiveStart(6677);
        //init = true;
    }

    private String getLive(String uuid) {
        return BASE + "live?uuid=" + uuid;
    }

    private String getOpen(String uuid) {
        return BASE + "open?uuid=" + uuid;
    }

    @Override
    public boolean match(String scheme, String host) {
        return scheme.equals("zlive");
    }

    @Override
    public String fetch(String url) throws Exception {
        if (!init) init();
        String[] split = url.split("/");
        String server = split[2];
        String uuid = split[3];
        String param = "&group=5850&mac=00:00:00:00:00:00&dir=";
        String result = getLive(uuid) + "&server=" + server + param + Path.cache();
        OkHttp.newCall(getOpen(uuid)).execute();
        return result;
    }

    @Override
    public void stop() {
        try {
            //if (init) com.east.android.zlive.ZLive.INSTANCE.OnLiveStop();
            //init = false;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exit() {
    }
}
