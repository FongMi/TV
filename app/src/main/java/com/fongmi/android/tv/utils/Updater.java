package com.fongmi.android.tv.utils;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogUpdateBinding;
import com.fongmi.android.tv.net.OKHttp;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.io.File;

public class Updater implements View.OnClickListener {

    private static final String DEF = "https://raw.githubusercontent.com/FongMi/TV/release/release/";
    private static final String URL = DEF + BuildConfig.FLAVOR_mode + ".json";
    private static final String APK = DEF + BuildConfig.FLAVOR_mode + "-" + BuildConfig.FLAVOR_api + ".apk";
    private static final String PROXY = "https://ghproxy.com/";

    private final Activity activity;
    private AlertDialog dialog;

    public static Updater create(Activity activity) {
        return new Updater(activity);
    }

    private Updater(Activity activity) {
        this.activity = activity;
    }

    public Updater force() {
        Prefers.putUpdate(true);
        return this;
    }

    public void start() {
        App.execute(this::doInBackground);
    }

    private void doInBackground() {
        connect(URL, 0);
    }

    private File getApk() {
        return FileUtil.getCacheFile("update.apk");
    }

    private void connect(String target, int retry) {
        try {
            JSONObject object = new JSONObject(OKHttp.newCall(target).execute().body().string());
            String name = object.optString("name");
            String desc = object.optString("desc");
            int code = object.optInt("code");
            String url = retry > 0 ? PROXY + APK : APK;
            if (code <= BuildConfig.VERSION_CODE) FileUtil.clearDir(getApk());
            else FileUtil.write(getApk(), OKHttp.newCall(url).execute().body().bytes());
            if (getApk().exists() && Prefers.getUpdate()) App.post(() -> checkActivity(name, desc));
        } catch (Exception e) {
            if (retry == 0) connect(PROXY + target, 1);
            e.printStackTrace();
        }
    }

    private void checkActivity(String version, String desc) {
        if (activity.isFinishing()) FileUtil.openFile(getApk());
        else showDialog(version, desc);
    }

    private void showDialog(String version, String desc) {
        DialogUpdateBinding binding = DialogUpdateBinding.inflate(LayoutInflater.from(activity));
        dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        binding.version.setText(ResUtil.getString(R.string.update_version, version));
        binding.confirm.setOnClickListener(this);
        binding.cancel.setOnClickListener(this);
        binding.desc.setText(desc);
        dialog.show();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.confirm) FileUtil.openFile(getApk());
        else if (view.getId() == R.id.cancel) Prefers.putUpdate(false);
        dialog.dismiss();
    }
}
