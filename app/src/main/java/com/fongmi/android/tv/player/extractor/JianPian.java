package com.fongmi.android.tv.player.extractor;

import android.net.Uri;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Github;
import com.github.catvod.net.OkHttp;
import com.p2p.P2PClass;

import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class JianPian implements Source.Extractor {

    private P2PClass p2p;
    private String url;

    @Override
    public boolean match(String scheme, String host) {
        return scheme.equals("tvbox-xg");
    }

    private void init() throws Exception {
        if (p2p != null) return;
        File file = FileUtil.getFilesFile("libp2p-jp-" + BuildConfig.FLAVOR_abi + ".so");
        String path = Github.get().getReleasePath("/other/jniLibs/" + file.getName());
        if (!file.exists()) FileUtil.write(file, OkHttp.newCall(path).execute().body().bytes());
        p2p = new P2PClass(App.get(), file.getAbsolutePath());
    }

    @Override
    public String fetch(String u) throws Exception {
        init();
        stop();
        set(u);
        start();
        return "http://127.0.0.1:" + p2p.port + "/" + URLEncoder.encode(Uri.parse(url).getLastPathSegment(), "GBK");
    }

    private void set(String u) throws Exception {
        u = u.replace("tvbox-xg://", "").replace("tvbox-xg:", "");
        String[] split = URLDecoder.decode(u, "UTF-8").split("\\|");
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

    @Override
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

    @Override
    public void destroy() {
        stop();
    }

    @Override
    public void release() {
        try {
            if (p2p != null) p2p.P2Pdoxendhttpd();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            p2p = null;
        }
    }
}
