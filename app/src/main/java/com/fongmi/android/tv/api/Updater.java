package com.fongmi.android.tv.api;

import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogUpdateBinding;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;

public class Updater implements View.OnClickListener {

    private WeakReference<Activity> activity;
    private AlertDialog dialog;
    private String md5;

    private static class Loader {
        static volatile Updater INSTANCE = new Updater();
    }

    public static Updater get() {
        return Loader.INSTANCE;
    }

    private File getFile() {
        return FileUtil.getCacheFile(BuildConfig.FLAVOR + ".apk");
    }

    private String getJson() {
        return Constant.getKitkatPath("/release/" + BuildConfig.FLAVOR + ".json");
    }

    private String getApk() {
        return Constant.getKitkatPath("/release/" + BuildConfig.FLAVOR + ".apk");
    }

    public Updater reset() {
        Prefers.putUpdate(true);
        return this;
    }

    public void start(Activity activity) {
        this.activity = new WeakReference<>(activity);
        App.execute(this::doInBackground);
    }

    private void doInBackground() {
        FileUtil.clearDir(getFile());
        connect(getJson());
    }

    private void connect(String target) {
        try {
            JSONObject object = new JSONObject(OkHttp.newCall(target).execute().body().string());
            String name = object.optString("name");
            String desc = object.optString("desc");
            int code = object.optInt("code");
            boolean need = code > BuildConfig.VERSION_CODE;
            if (need) FileUtil.write(getFile(), OkHttp.newCall(getApk()).execute().body().bytes());
            if (getFile().exists() && need && Prefers.getUpdate()) App.post(() -> checkActivity(name, desc));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkActivity(String version, String desc) {
        if (dialog != null) dialog.dismiss();
        if (activity.get().isFinishing()) install();
        else showDialog(version, desc);
    }

    private void showDialog(String version, String desc) {
        DialogUpdateBinding binding = DialogUpdateBinding.inflate(LayoutInflater.from(activity.get()));
        dialog = new MaterialAlertDialogBuilder(activity.get()).setView(binding.getRoot()).create();
        binding.version.setText(ResUtil.getString(R.string.update_version, version));
        binding.confirm.setOnClickListener(this);
        binding.cancel.setOnClickListener(this);
        binding.desc.setText(desc);
        dialog.show();
    }

    private void install() {
        FileUtil.openFile(getFile());
        if (!TextUtils.isEmpty(md5)) Prefers.putApkMD5(md5);
    }

    private void dismiss() {
        if (dialog != null) dialog.dismiss();
        this.md5 = null;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.cancel) Prefers.putUpdate(false);
        if (view.getId() == R.id.confirm) install();
        dismiss();
    }
}
