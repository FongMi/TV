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

    private static final String PROXY = "https://ghproxy.com/";

    private final Activity activity;
    private AlertDialog dialog;
    private String branch;
    private boolean force;

    public static Updater create(Activity activity) {
        return new Updater(activity);
    }

    private Updater(Activity activity) {
        this.activity = activity;
        this.branch = "release";
    }

    public Updater reset() {
        Prefers.putUpdate(true);
        return this;
    }

    public Updater force() {
        this.force = true;
        return this;
    }

    public Updater branch(String branch) {
        this.branch = branch;
        return this;
    }

    public void start() {
        App.execute(this::doInBackground);
    }

    private void doInBackground() {
        connect(getJson());
    }

    private File getFile() {
        return FileUtil.getCacheFile("update.apk");
    }

    private String getPath() {
        return "https://raw.githubusercontent.com/FongMi/TV/" + branch + "/release/";
    }

    private String getJson() {
        return PROXY + getPath() + BuildConfig.FLAVOR_mode + ".json";
    }

    private String getApk() {
        return PROXY + getPath() + BuildConfig.FLAVOR_mode + "-" + BuildConfig.FLAVOR_api + ".apk";
    }

    private void connect(String target) {
        try {
            JSONObject object = new JSONObject(OKHttp.newCall(target).execute().body().string());
            String name = object.optString("name");
            String desc = object.optString("desc");
            int code = object.optInt("code");
            if (code <= BuildConfig.VERSION_CODE) FileUtil.clearDir(getFile());
            if (code > BuildConfig.VERSION_CODE || force) FileUtil.write(getFile(), OKHttp.newCall(getApk()).execute().body().bytes());
            if (getFile().exists() && (Prefers.getUpdate() || force)) App.post(() -> checkActivity(name, desc));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkActivity(String version, String desc) {
        if (activity.isFinishing()) FileUtil.openFile(getFile());
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
        if (view.getId() == R.id.confirm) FileUtil.openFile(getFile());
        else if (view.getId() == R.id.cancel) Prefers.putUpdate(false);
        dialog.dismiss();
    }
}
