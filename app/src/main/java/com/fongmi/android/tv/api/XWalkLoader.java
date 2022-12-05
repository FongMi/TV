package com.fongmi.android.tv.api;

import android.content.Context;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.utils.FileUtil;

import org.xwalk.core.XWalkInitializer;

import java.io.File;
import java.lang.reflect.Method;

public class XWalkLoader {

    private XWalkInitializer initializer = null;

    private static class Loader {
        static volatile XWalkLoader INSTANCE = new XWalkLoader();
    }

    public static XWalkLoader get() {
        return Loader.INSTANCE;
    }

    private String getRuntimeAbi() {
        try {
            Class<?> cls = Class.forName("org.xwalk.core.XWalkEnvironment");
            Method method = cls.getMethod("getRuntimeAbi");
            return (String) method.invoke(null);
        } catch (Exception e) {
            return "armeabi-v7a";
        }
    }

    private String getUrl() {
        return String.format("https://ghproxy.com/https://raw.githubusercontent.com/FongMi/TV/kitkat/other/xwalk/XWalkRuntimeLib-%s.apk", getRuntimeAbi());
    }

    private File getFile() {
        return FileUtil.getCacheFile("XWalkRuntimeLib.apk");
    }

    private static String getLibPath() {
        return App.get().getDir("extracted_xwalkcore", Context.MODE_PRIVATE).getAbsolutePath();
    }

    private boolean exist() {
        String[] names = new String[]{"classes.dex", "icudtl.dat", "libxwalkcore.so", "xwalk.pak", "xwalk_100_percent.pak"};
        String dir = getLibPath();
        for (String name : names) if (!new File(dir + "/" + name).exists()) return false;
        return true;
    }

    private void download() {
        try {
            FileUtil.write(getFile(), OKHttp.newCall(getUrl()).execute().body().bytes());
            Class<?> cls = Class.forName("org.xwalk.core.XWalkDecompressor");
            Method method = cls.getMethod("extractResource", String.class, String.class);
            method.invoke(null, getFile().getAbsolutePath(), getLibPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private XWalkInitializer getInitializer() {
        return new XWalkInitializer(new XWalkInitializer.XWalkInitListener() {
            @Override
            public void onXWalkInitStarted() {
            }

            @Override
            public void onXWalkInitCancelled() {
            }

            @Override
            public void onXWalkInitFailed() {
            }

            @Override
            public void onXWalkInitCompleted() {

            }
        }, App.get());
    }

    public void load() {
        if (!exist()) download();
        if (initializer == null) initializer = getInitializer();
        if (!initializer.isXWalkReady()) initializer.initAsync();
    }
}
