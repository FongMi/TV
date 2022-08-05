package com.fongmi.android.tv.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.databinding.ViewProgressBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class Notify {

    private AlertDialog mDialog;
    private Toast mToast;

    private static class Loader {
        static volatile Notify INSTANCE = new Notify();
    }

    private static Notify get() {
        return Loader.INSTANCE;
    }

    public static void show(int resId) {
        if (resId != 0) show(ResUtil.getString(resId));
    }

    public static void show(String text) {
        get().makeText(text);
    }

    public static void show(Context context, View view) {
        get().mDialog = new MaterialAlertDialogBuilder(context).setView(view).create();
        get().mDialog.getWindow().setDimAmount(0);
        DisplayMetrics metrics = ResUtil.getDisplayMetrics();
        WindowManager.LayoutParams params = get().mDialog.getWindow().getAttributes();
        params.width = (int) (metrics.widthPixels * 0.3f);
        get().mDialog.getWindow().setAttributes(params);
        get().mDialog.show();
    }

    public static void progress(Context context) {
        ViewProgressBinding binding = ViewProgressBinding.inflate(LayoutInflater.from(context));
        get().mDialog = new MaterialAlertDialogBuilder(context).setView(binding.getRoot()).create();
        get().mDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        get().mDialog.show();
    }

    public static void dismiss() {
        if (get().mDialog != null && get().mDialog.isShowing()) get().mDialog.dismiss();
    }

    private void makeText(String message) {
        if (mToast != null) mToast.cancel();
        if (TextUtils.isEmpty(message)) return;
        mToast = Toast.makeText(App.get(), message, Toast.LENGTH_LONG);
        mToast.show();
    }
}
