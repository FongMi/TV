package com.fongmi.android.tv.api;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.Github;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogUpdateBinding;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.Download;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

public class Updater {

    private DialogUpdateBinding binding;
    private AlertDialog dialog;
    private String branch;
    private boolean force;

    private static class Loader {
        static volatile Updater INSTANCE = new Updater();
    }

    public static Updater get() {
        return Loader.INSTANCE;
    }

    private File getFile() {
        return FileUtil.getCacheFile(branch + ".apk");
    }

    private String getJson() {
        return Github.get().getBranchPath(branch, "/release/" + BuildConfig.FLAVOR_mode + "-" + branch + ".json");
    }

    private String getApk() {
        return Github.get().getBranchPath(branch, "/release/" + BuildConfig.FLAVOR_mode + "-" + BuildConfig.FLAVOR_api + ".apk");
    }

    private Updater() {
        this.branch = Github.RELEASE;
    }

    public Updater reset() {
        Prefers.putUpdate(true);
        return this;
    }

    public Updater force() {
        Notify.show(R.string.update_check);
        this.force = true;
        return this;
    }

    public Updater branch(String branch) {
        this.branch = branch;
        return this;
    }

    public void start(Activity activity) {
        this.binding = DialogUpdateBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).setCancelable(false).create();
        App.execute(this::doInBackground);
    }

    private boolean need(int code, String name) {
        return (branch.equals(Github.DEV) ? !name.equals(BuildConfig.VERSION_NAME) : code > BuildConfig.VERSION_CODE) && Prefers.getUpdate();
    }

    private void doInBackground() {
        try {
            JSONObject object = new JSONObject(OkHttp.newCall(getJson()).execute().body().string());
            String name = object.optString("name");
            String desc = object.optString("desc");
            int code = object.optInt("code");
            if (need(code, name) || force) App.post(() -> show(name, desc));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void show(String version, String desc) {
        binding.version.setText(ResUtil.getString(R.string.update_version, version));
        binding.confirm.setOnClickListener(this::confirm);
        binding.cancel.setOnClickListener(this::cancel);
        binding.desc.setText(desc);
        dialog.show();
    }

    private void dismiss() {
        if (dialog != null) dialog.dismiss();
        this.branch = Github.RELEASE;
        this.force = false;
    }

    private void cancel(View view) {
        Prefers.putUpdate(false);
        dismiss();
    }

    private void confirm(View view) {
        binding.confirm.setEnabled(false);
        download();
    }

    private void download() {
        Download.create(getApk(), getFile(), getCallback()).start();
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void progress(int progress) {
                binding.confirm.setText(String.format(Locale.getDefault(), "%1$d%%", progress));
            }

            @Override
            public void success() {
                FileUtil.openFile(getFile());
                dismiss();
            }

            @Override
            public void error() {
                dismiss();
            }
        };
    }
}
