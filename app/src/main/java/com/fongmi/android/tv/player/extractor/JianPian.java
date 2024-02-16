package com.fongmi.android.tv.player.extractor;

import android.net.Uri;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.utils.FileUtil;
import com.github.catvod.utils.Path;
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
        check();
        start(url);
        return "http://127.0.0.1:" + p2p.port + "/" + URLEncoder.encode(Uri.parse(path).getLastPathSegment(), "GBK");
    }

    private void check() {
        double cache = FileUtil.getDirectorySize(Path.jpa());
        double avail = FileUtil.getAvailableStorageSpace(Path.jpa());
        int usage = (int) (cache / avail * 100);
        if (usage > 30) Path.clear(Path.jpa());
    }

    private void start(String url) {
        try {
            path = URLDecoder.decode(url).split("\\|")[0];
            path = path.replace("jianpian://pathtype=url&path=", "");
            path = path.replace("tvbox-xg://", "").replace("tvbox-xg:", "");
            path = path.replace("xg://", "ftp://").replace("xgplay://", "ftp://");
            p2p.P2Pdoxstart(path.getBytes("GBK"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            if (p2p == null || path == null) return;
            p2p.P2Pdoxpause(path.getBytes("GBK"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            path = null;
        }
    }

    @Override
    public void exit() {
        App.execute(this::check);
    }
}
