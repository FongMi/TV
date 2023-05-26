package com.fongmi.android.tv;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.databinding.DialogUpdateBinding;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Github;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.net.OkHttp;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.io.File;
import java.util.Locale;

public class Updater implements Download.Callback {

    private DialogUpdateBinding binding;
    private AlertDialog dialog;
    private String branch;

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
        return Github.get().getBranchPath(branch, "/release/" + BuildConfig.FLAVOR_mode + ".json");
    }

    private String getApk() {
        return Github.get().getBranchPath(branch, "/release/" + BuildConfig.FLAVOR_mode + "-" + BuildConfig.FLAVOR_api + "-" + BuildConfig.FLAVOR_abi + ".apk");
    }

    private Updater() {
        this.branch = Github.RELEASE;
    }

    public Updater force() {
        Notify.show(R.string.update_check);
        Prefers.putUpdate(true);
        return this;
    }

    public Updater release() {
        this.branch = Github.RELEASE;
        return this;
    }

    public Updater dev() {
        this.branch = Github.DEV;
        return this;
    }

    private Updater check() {
        dismiss();
        return this;
    }

    public void start() {
        App.execute(this::doInBackground);
    }

    private boolean need(int code, String name) {
        return Prefers.getUpdate() && (branch.equals(Github.DEV) ? !name.equals(BuildConfig.VERSION_NAME) && code >= BuildConfig.VERSION_CODE : code > BuildConfig.VERSION_CODE);
    }

    private void doInBackground() {
        try {
            JSONObject object = new JSONObject(OkHttp.newCall(getJson()).execute().body().string());
            String name = object.optString("name");
            String desc = object.optString("desc");
            int code = object.optInt("code");
            if (need(code, name)) App.post(() -> show(App.activity(), name, desc));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void show(Activity activity, String version, String desc) {
        binding = DialogUpdateBinding.inflate(LayoutInflater.from(activity));
        binding.version.setText(ResUtil.getString(R.string.update_version, version));
        binding.confirm.setOnClickListener(this::confirm);
        binding.cancel.setOnClickListener(this::cancel);
        check().create(activity).show();
        binding.desc.setText(desc);
    }

    private AlertDialog create(Activity activity) {
        return dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).setCancelable(false).create();
    }

    private void cancel(View view) {
        Prefers.putUpdate(false);
        dismiss();
    }

    private void confirm(View view) {
        binding.confirm.setEnabled(false);
        Download.create(getApk(), getFile(), this).start();
    }

    private void dismiss() {
        try {
            if (dialog != null) dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void progress(int progress) {
        binding.confirm.setText(String.format(Locale.getDefault(), "%1$d%%", progress));
    }

    @Override
    public void error(String message) {
        Notify.show(message);
        dismiss();
    }

    @Override
    public void success(File file) {
        FileUtil.openFile(getFile());
        dismiss();
    }
}
