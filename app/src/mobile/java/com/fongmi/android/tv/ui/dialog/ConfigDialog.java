package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.DialogConfigBinding;
import com.fongmi.android.tv.impl.ConfigListener;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.UrlUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ConfigDialog extends BaseAlertDialog {

    private DialogConfigBinding binding;
    private boolean append = true;
    private boolean edit;
    private String ori;
    private int type;

    public static ConfigDialog create() {
        return new ConfigDialog();
    }

    public ConfigDialog vod() {
        type = 0;
        return this;
    }

    public ConfigDialog live() {
        type = 1;
        return this;
    }

    public ConfigDialog wall() {
        type = 2;
        return this;
    }

    public ConfigDialog edit() {
        edit = true;
        return this;
    }

    public void show(Fragment fragment) {
        show(fragment.getChildFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogConfigBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setTitle(type == 0 ? R.string.setting_vod : type == 1 ? R.string.setting_live : R.string.setting_wall).setView(getBinding().getRoot()).setPositiveButton(edit ? R.string.dialog_edit : R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, null);
    }

    @Override
    protected void initView() {
        binding.name.setText(getConfig().getName());
        binding.url.setText(ori = getConfig().getUrl());
        binding.input.setVisibility(edit ? View.VISIBLE : View.GONE);
        binding.url.setSelection(TextUtils.isEmpty(ori) ? 0 : ori.length());
    }

    @Override
    protected void initEvent() {
        binding.choose.setEndIconOnClickListener(this::onChoose);
        binding.url.addTextChangedListener(new CustomTextListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                detect(s.toString());
            }
        });
        binding.url.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) onPositive(null, 0);
            return true;
        });
    }

    private Config getConfig() {
        return switch (type) {
            case 0 -> VodConfig.get().getConfig();
            case 1 -> LiveConfig.get().getConfig();
            case 2 -> WallConfig.get().getConfig();
            default -> null;
        };
    }

    private void onChoose(View view) {
        FileChooser.from(launcher).show();
    }

    private void detect(String s) {
        if (append && "h".equalsIgnoreCase(s)) {
            append = false;
            binding.url.append("ttp://");
        } else if (append && "f".equalsIgnoreCase(s)) {
            append = false;
            binding.url.append("ile://");
        } else if (append && "a".equalsIgnoreCase(s)) {
            append = false;
            binding.url.append("ssets://");
        } else if (s.length() > 1) {
            append = false;
        } else if (s.isEmpty()) {
            append = true;
        }
    }

    private void onPositive(DialogInterface dialog, int which) {
        String url = binding.url.getText().toString().trim();
        String name = binding.name.getText().toString().trim();
        if (edit) Config.find(ori, type).url(url).name(name).update();
        if (url.isEmpty()) Config.delete(ori, type);
        ((ConfigListener) requireParentFragment()).setConfig(Config.find(url, type));
        dismiss();
    }

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> FileChooser.getUri(result, this::setConfig));

    private void setConfig(Uri uri) {
        if (!isAdded()) return;
        ((ConfigListener) requireParentFragment()).setConfig(Config.find(UrlUtil.toLocalUrl(uri), type));
        dismiss();
    }
}
