package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fongmi.android.tv.utils.Util;

public final class CloudflareDialog {

    private final DialogInterface.OnDismissListener listener;
    private final FrameLayout container;
    private final Dialog dialog;
    private final View content;

    public static CloudflareDialog create(@NonNull Activity activity, @NonNull View content, @NonNull DialogInterface.OnDismissListener listener) {
        return new CloudflareDialog(activity, content, listener);
    }

    private CloudflareDialog(Activity activity, View content, DialogInterface.OnDismissListener listener) {
        this.listener = listener;
        this.container = new FrameLayout(activity);
        this.dialog = new Dialog(activity);
        this.content = content;
        init();
    }

    private void init() {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(container);
        dialog.setOnDismissListener(this::onDismiss);
        container.setBackgroundColor(Color.WHITE);
        container.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ViewCompat.setOnApplyWindowInsetsListener(container, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });
    }

    public CloudflareDialog show() {
        dialog.show();
        Window window = dialog.getWindow();
        if (window == null) return this;
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        if (Util.isLeanback()) Util.hideSystemUI(window);
        ViewCompat.requestApplyInsets(container);
        return this;
    }

    public void dismiss() {
        dialog.setOnDismissListener(null);
        detachContent();
        dialog.dismiss();
    }

    private void onDismiss(DialogInterface dialog) {
        detachContent();
        listener.onDismiss(dialog);
    }

    private void detachContent() {
        if (content.getParent() == container) container.removeView(content);
    }
}
