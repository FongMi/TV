package com.fongmi.android.tv.player.source;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.net.OKHttp;
import com.forcetech.Port;
import com.gsoft.mitv.MainActivity;

import okhttp3.Headers;

public class Force {

    private boolean init;

    private static class Loader {
        static volatile Force INSTANCE = new Force();
    }

    public static Force get() {
        return Loader.INSTANCE;
    }

    private void init() {
        App.get().bindService(new Intent(App.get(), MainActivity.class), mConn, Context.BIND_AUTO_CREATE);
        init = true;
    }

    public void destroy() {
        try {
            if (init) App.get().unbindService(mConn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String fetch(String url) {
        try {
            if (!init) init();
            int port = Port.get(url);
            Uri uri = Uri.parse(url);
            String id = uri.getLastPathSegment();
            String cmd = "http://127.0.0.1:" + port + "/cmd.xml?cmd=switch_chan&server=" + uri.getHost() + ":" + uri.getPort() + "&id=" + id;
            String result = "http://127.0.0.1:" + port + "/" + id;
            OKHttp.newCall(cmd, Headers.of("user-agent", "MTV")).execute();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return url;
        }
    }

    private final ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
}
