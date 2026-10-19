package com.fongmi.android.tv.ui.dialog;

import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogSingleInputBinding;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.impl.SubtitleListener;
import com.fongmi.android.tv.setting.SubtitleSetting;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class SubtitleApiDialog extends BaseAlertDialog {

    private DialogSingleInputBinding binding;

    public static void show(Fragment fragment) {
        new SubtitleApiDialog().show(fragment.getChildFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogSingleInputBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setView(getBinding().getRoot());
    }

    @Override
    protected void initView() {
        String text;
        binding.text.setHint(R.string.subtitle_search_token_hint);
        binding.text.setText(text = SubtitleSetting.getEffectiveToken());
        binding.text.setSelection(TextUtils.isEmpty(text) ? 0 : text.length());
    }

    @Override
    protected void initEvent() {
        binding.phone.setOnClickListener(view -> LocalConnectDialog.show(requireActivity(), "settings"));
        binding.positive.setOnClickListener(this::onPositive);
        binding.negative.setOnClickListener(this::onNegative);
        binding.text.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) binding.positive.performClick();
            return true;
        });
    }

    private void onPositive(View view) {
        getListener().setSubtitleToken(binding.text.getText().toString().trim());
        dismiss();
    }

    private SubtitleListener getListener() {
        Fragment parent = getParentFragment();
        if (parent instanceof SubtitleListener listener) return listener;
        return (SubtitleListener) requireActivity();
    }

    private void onNegative(View view) {
        dismiss();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        if (event.type() != ServerEvent.Type.SETTING) return;
        binding.text.setText(event.text());
        binding.positive.performClick();
    }

    @Override
    public void onStart() {
        super.onStart();
        setWidth(0.55f);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
}
