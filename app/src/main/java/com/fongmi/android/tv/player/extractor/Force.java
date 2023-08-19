package com.fongmi.android.tv.player.extractor;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.Source;
import com.forcetech.Util;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Github;

import java.util.HashSet;

import okhttp3.Headers;

public class Force implements Source.Extractor {

    private final HashSet<String> set = new HashSet<>();

    @Override
    public boolean match(String scheme, String host) {
        return !scheme.equals("push") && scheme.startsWith("p") || scheme.equals("mitv");
    }

    private void init(String scheme) {
        App.get().bindService(Util.intent(App.get(), scheme, Github.getSo(scheme)), mConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public String fetch(String url) throws Exception {
        String scheme = Util.scheme(url);
        if (!set.contains(scheme)) init(scheme);
        while (!set.contains(scheme)) SystemClock.sleep(10);
        Uri uri = Uri.parse(url);
        int port = Util.port(scheme);
        String id = uri.getLastPathSegment();
        String cmd = "http://127.0.0.1:" + port + "/cmd.xml?cmd=switch_chan&server=" + uri.getHost() + ":" + uri.getPort() + "&id=" + id;
        String result = "http://127.0.0.1:" + port + "/" + id;
        OkHttp.newCall(cmd, Headers.of("user-agent", "MTV")).execute().body().string();
        return result;
    }

    @Override
    public void stop() {
    }

    @Override
    public void exit() {
        try {
            if (!set.isEmpty()) App.get().unbindService(mConn);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            set.clear();
        }
    }

    private final ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            set.add(Util.trans(name));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            set.remove(Util.trans(name));
        }
    };
}
