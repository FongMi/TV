package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogUaBinding;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.impl.UaCallback;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.utils.QRCode;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.utils.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class UaDialog implements DialogInterface.OnDismissListener {

    private final DialogUaBinding binding;
    private final UaCallback callback;
    private final AlertDialog dialog;
    private boolean append;

    public static UaDialog create(FragmentActivity activity) {
        return new UaDialog(activity);
    }

    public UaDialog(FragmentActivity activity) {
        this.callback = (UaCallback) activity;
        this.binding = DialogUaBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        this.append = true;
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * 0.55f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.setOnDismissListener(this);
        dialog.show();
    }

    private void initView() {
        String text = Setting.getUa();
        String address = Server.get().getAddress();
        binding.text.setText(text);
        binding.code.setImageBitmap(QRCode.getBitmap(address, 200, 0));
        binding.text.setSelection(TextUtils.isEmpty(text) ? 0 : text.length());
        binding.info.setText(ResUtil.getString(R.string.push_info, address).replace("，", "\n"));
    }

    private void initEvent() {
        EventBus.getDefault().register(this);
        binding.positive.setOnClickListener(this::onPositive);
        binding.negative.setOnClickListener(this::onNegative);
        binding.text.addTextChangedListener(new CustomTextListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                detect(s.toString());
            }
        });
        binding.text.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) binding.positive.performClick();
            return true;
        });
    }

    private void detect(String s) {
        if (append && s.equalsIgnoreCase("c")) {
            append = false;
            binding.text.setText(Util.CHROME);
        } else if (append && s.equalsIgnoreCase("o")) {
            append = false;
            binding.text.setText(okhttp3.internal.Util.userAgent);
        } else if (s.length() > 1) {
            append = false;
        } else if (s.length() == 0) {
            append = true;
        }
    }

    private void onPositive(View view) {
        callback.setUa(binding.text.getText().toString().trim());
        dialog.dismiss();
    }

    private void onNegative(View view) {
        dialog.dismiss();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        if (event.getType() != ServerEvent.Type.SETTING) return;
        binding.text.setText(event.getText());
        binding.positive.performClick();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        EventBus.getDefault().unregister(this);
    }
}
