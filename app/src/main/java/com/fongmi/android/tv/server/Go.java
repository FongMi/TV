package com.fongmi.android.tv.server;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.ShellUtil;
import com.github.catvod.Init;
import com.github.catvod.utils.Path;

import java.io.File;

public class Go {

    public static void prepare() {
        App.execute(() -> {
            try {
                File file = new File(Init.context().getFilesDir().getAbsolutePath() + "/go_proxy_video");
                if (file.exists()) return;
                file.createNewFile();
                Path.copy(Init.context().getAssets().open("go_proxy_video"), file);
                file.setExecutable(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void start() {
        new Thread(() -> {
            try {
                File file = new File(Init.context().getFilesDir().getAbsolutePath() + "/go_proxy_video");
                if (!file.exists()) return;
                file.setExecutable(true);
                ShellUtil.execCommand("nohup " + file.getAbsolutePath(), false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void stop() {
        new Thread(() -> {
            ShellUtil.execCommand(new String[]{"killall -9 go_proxy_video"}, false);
        }).start();
    }

}
