package com.fongmi.android.tv.player.source;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.Github;
import com.fongmi.android.tv.utils.FileUtil;
import com.forcetech.Util;
import com.github.catvod.net.OkHttp;

import java.io.File;
import java.util.HashSet;

import okhttp3.Headers;

public class Force {

    private final HashSet<String> set;

    private static class Loader {
        static volatile Force INSTANCE = new Force();
    }

    public static Force get() {
        return Loader.INSTANCE;
    }

    public Force() {
        set = new HashSet<>();
    }

    private void init(String url) throws Exception {
        File file = FileUtil.getCacheFile(Util.so(url));
        String path = Github.get().getReleasePath("/other/jniLibs/" + file.getName());
        if (!file.exists()) FileUtil.write(file, OkHttp.newCall(path).execute().body().bytes());
        App.get().bindService(Util.intent(App.get(), url, file), mConn, Context.BIND_AUTO_CREATE);
    }

    public String fetch(String url) {
        try {
            String scheme = Util.scheme(url);
            if (!set.contains(scheme)) init(url);
            while (!set.contains(scheme)) SystemClock.sleep(10);
            Uri uri = Uri.parse(url);
            int port = Util.port(url);
            String id = uri.getLastPathSegment();
            String cmd = "http://127.0.0.1:" + port + "/cmd.xml?cmd=switch_chan&server=" + uri.getHost() + ":" + uri.getPort() + "&id=" + id;
            String result = "http://127.0.0.1:" + port + "/" + id;
            OkHttp.newCall(cmd, Headers.of("user-agent", "MTV")).execute().body().string();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return url;
        }
    }

    public void stop() {
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
