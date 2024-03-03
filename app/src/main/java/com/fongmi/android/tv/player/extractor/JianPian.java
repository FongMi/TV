package com.fongmi.android.tv.player.extractor;

import android.net.Uri;

import com.fongmi.android.tv.player.Source;
import com.github.catvod.utils.Path;
import com.p2p.P2PClass;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class JianPian implements Source.Extractor {

    private P2PClass p2p;
    private String path;
    private Map<String, Boolean> pathPaused;

    @Override
    public boolean match(String scheme, String host) {
        return "tvbox-xg".equals(scheme) || "jianpian".equals(scheme) || "ftp".equals(scheme);
    }

    private void init() {
        if (p2p == null) p2p = new P2PClass();
        if (pathPaused == null) pathPaused = new HashMap<>();
    }

    @Override
    public String fetch(String url) throws Exception {
        init();
        stop();
        start(url);
        return "http://127.0.0.1:" + p2p.port + "/" + URLEncoder.encode(Uri.parse(path).getLastPathSegment(), "GBK");
    }

    private void start(String url) {
        try {
            String lastPath = path;
            path = URLDecoder.decode(url).split("\\|")[0];
            path = path.replace("jianpian://pathtype=url&path=", "");
            path = path.replace("tvbox-xg://", "").replace("tvbox-xg:", "");
            path = path.replace("xg://", "ftp://").replace("xgplay://", "ftp://");
            boolean isDiff = lastPath != null && !lastPath.equals(path);
            if (isDiff) p2p.P2Pdoxdel(lastPath.getBytes("GBK"));
            p2p.P2Pdoxstart(path.getBytes("GBK"));
            if (lastPath == null || isDiff) p2p.P2Pdoxadd(path.getBytes("GBK"));
            if (isDiff && pathPaused.containsKey(lastPath)) pathPaused.remove(lastPath);
            pathPaused.put(path, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            if (p2p == null || path == null) return;
            if (pathPaused.containsKey(path) && pathPaused.get(path)) return;
            p2p.P2Pdoxpause(path.getBytes("GBK"));
            pathPaused.put(path, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exit() {
        Path.clear(Path.jpa());
    }
}
