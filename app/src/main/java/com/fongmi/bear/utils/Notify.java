package com.fongmi.bear.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.bear.App;
import com.fongmi.bear.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class Notify {

    private Toast mToast;

    private static class Loader {
        static volatile Notify INSTANCE = new Notify();
    }

    private static Notify get() {
        return Loader.INSTANCE;
    }

    public static void show(int resId) {
        show(Utils.getString(resId));
    }

    public static void show(String text) {
        get().makeText(text);
    }

    public static void show(Context context, View view, DialogInterface.OnClickListener listener) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(context).setView(view).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, listener).show();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void makeText(String message) {
        if (mToast != null) mToast.cancel();
        mToast = Toast.makeText(App.get(), message, Toast.LENGTH_LONG);
        mToast.show();
    }
}
