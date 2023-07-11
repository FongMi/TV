package com.fongmi.android.tv.player.source;

import android.net.Uri;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Github;
import com.github.catvod.net.OkHttp;
import com.p2p.P2PClass;

import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class JianPian {

    private P2PClass p2p;
    private String url;

    private static class Loader {
        static volatile JianPian INSTANCE = new JianPian();
    }

    public static JianPian get() {
        return Loader.INSTANCE;
    }

    private void init() throws Exception {
        if (p2p != null) return;
        String name = "libp2p-jp-" + BuildConfig.FLAVOR_abi + ".so";
        File file = FileUtil.getCacheFile(name);
        String path = Github.get().getReleasePath("/other/jniLibs/" + file.getName());
        if (!file.exists()) FileUtil.write(file, OkHttp.newCall(path).execute().body().bytes());
        p2p = new P2PClass(App.get(), file.getAbsolutePath());
    }

    public String fetch(String text) throws Exception {
        init();
        stop();
        set(text);
        start();
        return "http://127.0.0.1:" + p2p.port + "/" + URLEncoder.encode(Uri.parse(url).getLastPathSegment(), "GBK");
    }

    private void set(String text) throws Exception {
        text = text.replace("tvbox-xg://", "").replace("tvbox-xg:", "");
        String[] split = URLDecoder.decode(text, "UTF-8").split("\\|");
        url = split[0].replace("xg://", "ftp://").replace("xgplay://", "ftp://");
    }

    private void start() {
        try {
            if (p2p == null || url == null) return;
            p2p.P2Pdoxstart(url.getBytes("GBK"));
            p2p.P2Pdoxadd(url.getBytes("GBK"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if (p2p == null || url == null) return;
            p2p.P2Pdoxpause(url.getBytes("GBK"));
            p2p.P2Pdoxdel(url.getBytes("GBK"));
            url = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
