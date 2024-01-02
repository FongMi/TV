package com.fongmi.android.tv.player.extractor;

import android.net.Uri;

import com.fongmi.android.tv.player.Source;
import com.p2p.P2PClass;

import java.net.URLDecoder;
import java.net.URLEncoder;

public class JianPian implements Source.Extractor {

    private P2PClass p2p;
    private String path;

    @Override
    public boolean match(String scheme, String host) {
        return scheme.equals("tvbox-xg") || scheme.equals("jianpian") || scheme.equals("ftp");
    }

    private void init() {
        if (p2p == null) p2p = new P2PClass();
    }

    @Override
    public String fetch(String url) throws Exception {
        init();
        stop();
        set(url);
        start();
        return "http://127.0.0.1:" + p2p.port + "/" + URLEncoder.encode(Uri.parse(path).getLastPathSegment(), "GBK");
    }

    private void set(String url) {
        path = URLDecoder.decode(url).split("\\|")[0];
        path = path.replace("jianpian://pathtype=url&path=", "");
        path = path.replace("tvbox-xg://", "").replace("tvbox-xg:", "");
        path = path.replace("xg://", "ftp://").replace("xgplay://", "ftp://");
    }

    private void start() {
        try {
            if (p2p == null || path == null) return;
            p2p.P2Pdoxstart(path.getBytes("GBK"));
            p2p.P2Pdoxadd(path.getBytes("GBK"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            if (p2p == null || path == null) return;
            p2p.P2Pdoxpause(path.getBytes("GBK"));
            p2p.P2Pdoxdel(path.getBytes("GBK"));
            path = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exit() {
    }
}
