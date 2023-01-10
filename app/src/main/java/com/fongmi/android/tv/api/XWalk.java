package com.fongmi.android.tv.api;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Github;
import com.fongmi.android.tv.databinding.DialogXwalkBinding;
import com.fongmi.android.tv.net.Download;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.xwalk.core.XWalkInitializer;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Locale;

public class XWalk implements Download.Callback {

    private XWalkInitializer initializer;
    private DialogXwalkBinding binding;
    private AlertDialog dialog;

    private static class Loader {
        static volatile XWalk INSTANCE = new XWalk();
    }

    public static XWalk get() {
        return Loader.INSTANCE;
    }

    private String getUrl() {
        return String.format(Github.get().getKitkatPath("/other/xwalk/XWalkRuntimeLib-%s.apk"), getRuntimeAbi());
    }

    private File getFile() {
        return FileUtil.getCacheFile("XWalkRuntimeLib.apk");
    }

    private String getLibPath() {
        return App.get().getDir("extracted_xwalkcore", Context.MODE_PRIVATE).getAbsolutePath();
    }

    public void check() {
        try {
            if (!exist()) show();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private String getRuntimeAbi() {
        try {
            Class<?> cls = Class.forName("org.xwalk.core.XWalkEnvironment");
            return (String) cls.getMethod("getRuntimeAbi").invoke(null);
        } catch (Throwable e) {
            return "armeabi-v7a";
        }
    }

    private boolean exist() {
        String[] names = new String[]{"classes.dex", "icudtl.dat", "libxwalkcore.so", "xwalk.pak", "xwalk_100_percent.pak"};
        String dir = getLibPath();
        for (String name : names) if (!new File(dir + "/" + name).exists()) return false;
        return true;
    }

    private void show() {
        binding = DialogXwalkBinding.inflate(LayoutInflater.from(App.getActivity()));
        dialog = new MaterialAlertDialogBuilder(App.getActivity()).setView(binding.getRoot()).setCancelable(false).create();
        binding.confirm.setOnClickListener(this::confirm);
        dialog.show();
    }

    private void confirm(View view) {
        Download.create(getUrl(), getFile(), this).start();
        view.setEnabled(false);
    }

    private void extract() {
        try {
            Class<?> cls = Class.forName("org.xwalk.core.XWalkDecompressor");
            Method method = cls.getMethod("extractResource", String.class, String.class);
            method.invoke(null, getFile().getAbsolutePath(), getLibPath());
            App.post(() -> Notify.show("XWalk 插件已安裝"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            getFile().delete();
        }
    }

    @Override
    public void progress(int progress) {
        binding.confirm.setText(String.format(Locale.getDefault(), "%1$d%%", progress));
    }

    @Override
    public void error(String message) {
        Notify.show(message);
        dialog.dismiss();
    }

    @Override
    public void success(File file) {
        App.execute(this::extract);
        dialog.dismiss();
    }

    public void init(State state) {
        if (initializer == null) initializer = new XWalkInitializer(listener(state), App.get());
        if (initializer.isXWalkReady()) state.success();
        else initializer.initAsync();
    }

    private XWalkInitializer.XWalkInitListener listener(State state) {
        return new XWalkInitializer.XWalkInitListener() {
            @Override
            public void onXWalkInitStarted() {
            }

            @Override
            public void onXWalkInitCancelled() {
                state.fail();
            }

            @Override
            public void onXWalkInitFailed() {
                state.fail();
            }

            @Override
            public void onXWalkInitCompleted() {
                state.success();
            }
        };
    }

    public interface State {

        void success();

        void fail();
    }
}
