package com.fongmi.android.tv.player.source;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.Github;
import com.fongmi.android.tv.utils.FileUtil;
import com.forcetech.Port;
import com.gsoft.mitv.MainActivity;

import java.io.File;

import okhttp3.Headers;

public class Force {

    private boolean init;

    private static class Loader {
        static volatile Force INSTANCE = new Force();
    }

    public static Force get() {
        return Loader.INSTANCE;
    }

    private void init() throws Exception {
        check();
        start();
    }

    private void check() throws Exception {
        File file = FileUtil.getCacheFile("libmitv.so");
        String url = Github.get().getReleasePath("/other/libmitv.so");
        if (!file.exists()) FileUtil.write(file, OkHttp.newCall(url).execute().body().bytes());
    }

    private void start() {
        App.get().bindService(new Intent(App.get(), MainActivity.class), mConn, Context.BIND_AUTO_CREATE);
    }

    public String fetch(String url) {
        try {
            if (!init) init();
            while (!init) SystemClock.sleep(10);
            int port = Port.get(url);
            Uri uri = Uri.parse(url);
            String id = uri.getLastPathSegment();
            String cmd = "http://127.0.0.1:" + port + "/cmd.xml?cmd=switch_chan&server=" + uri.getHost() + ":" + uri.getPort() + "&id=" + id;
            String result = "http://127.0.0.1:" + port + "/" + id;
            OkHttp.newCall(cmd, Headers.of("user-agent", "MTV")).execute();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return url;
        }
    }

    public void stop() {
        try {
            if (init) App.get().unbindService(mConn);
            init = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            init = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            init = false;
        }
    };
}
