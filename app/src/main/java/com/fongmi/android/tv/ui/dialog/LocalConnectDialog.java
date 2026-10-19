package com.fongmi.android.tv.ui.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogPhoneConnectBinding;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.QRCode;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LocalConnectDialog extends BaseAlertDialog {

    private static final String TAG = "local-connect";

    private DialogPhoneConnectBinding binding;

    public static void show(FragmentActivity activity, String tab) {
        PermissionUtil.requestLocalNetwork(activity, granted -> onPermissionResult(activity, tab, granted));
    }

    private static void onPermissionResult(FragmentActivity activity, String tab, boolean granted) {
        if (!granted || activity.getSupportFragmentManager().isStateSaved() || activity.getSupportFragmentManager().findFragmentByTag(TAG) != null) return;
        if (!Server.get().start()) {
            Notify.show(R.string.server_start_failed);
            return;
        }
        Bundle args = new Bundle();
        args.putString("tab", tab);
        LocalConnectDialog dialog = new LocalConnectDialog();
        dialog.setArguments(args);
        dialog.show(activity.getSupportFragmentManager(), TAG);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogPhoneConnectBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setBackground(new ColorDrawable(Color.TRANSPARENT)).setBackgroundInsetStart(0).setBackgroundInsetEnd(0).setBackgroundInsetTop(0).setBackgroundInsetBottom(0).setView(getBinding().getRoot());
    }

    @Override
    protected void initView() {
        String tab = requireArguments().getString("tab");
        String url = Server.get().getPageAddress(tab);
        binding.getRoot().setImageBitmap(QRCode.getBitmap(url, 220, 4));
    }

    @Override
    public void onStart() {
        super.onStart();
        int size = ResUtil.dp2px(220);
        requireDialog().getWindow().setLayout(size, size);
    }
}
