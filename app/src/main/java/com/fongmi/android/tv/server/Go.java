package com.fongmi.android.tv.server;

import com.github.catvod.utils.Asset;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Shell;

import java.io.File;

public class Go {

    private static final String GO = "go_proxy_video";

    public static void start() {
        new Thread(() -> {
            File file = Path.cache(GO);
            if (!file.exists()) Path.copy(Asset.open(GO), file);
            Shell.exec("killall -9 " + GO);
            Shell.exec("nohup " + file);
        }).start();
    }

    public static void stop() {
        new Thread(() -> Shell.exec("killall -9 " + GO)).start();
    }
}
