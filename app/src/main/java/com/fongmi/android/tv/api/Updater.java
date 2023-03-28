package com.fongmi.android.tv.api;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.Github;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogUpdateBinding;
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
        return Github.get().getBranchPath(branch, "/release/" + BuildConfig.FLAVOR_mode + "-" + branch + ".json");
    }

    private String getApk() {
        return Github.get().getBranchPath(branch, "/release/" + BuildConfig.FLAVOR_mode + "-" + BuildConfig.FLAVOR_api + ".apk");
    }

    private Updater() {
        this.branch = Github.RELEASE;
    }

    public Updater force() {
        Notify.show(R.string.update_check);
        Prefers.putUpdate(true);
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
        return (branch.equals(Github.DEV) ? !name.equals(BuildConfig.VERSION_NAME) : code > BuildConfig.VERSION_CODE) && Prefers.getUpdate();
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
        check().create(activity, ResUtil.getString(R.string.update_version, version)).show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this::confirm);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(this::cancel);
        binding.desc.setText(desc);
    }

    private AlertDialog create(Activity activity, String title) {
        return dialog = new MaterialAlertDialogBuilder(activity).setTitle(title).setView(binding.getRoot()).setPositiveButton(R.string.update_confirm, null).setNegativeButton(R.string.dialog_negative, null).setCancelable(false).create();
    }

    private void cancel(View view) {
        Prefers.putUpdate(false);
        dialog.dismiss();
    }

    private void confirm(View view) {
        Download.create(getApk(), getFile(), this).start();
        view.setEnabled(false);
    }

    private void dismiss() {
        try {
            if (dialog != null) dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void progress(int progress) {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(String.format(Locale.getDefault(), "%1$d%%", progress));
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
