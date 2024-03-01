package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogMenuBinding;
import com.fongmi.android.tv.ui.activity.HistoryActivity;
import com.fongmi.android.tv.ui.activity.HomeActivity;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MenuDialog {
    private final DialogMenuBinding binding;
    private final AlertDialog dialog;

    private final Activity activity;
    private String[] homeMenuKey;

    public static MenuDialog create(Activity activity) {
        return new MenuDialog(activity);
    }

    public MenuDialog(Activity activity) {
        this.activity = activity;
        this.binding = DialogMenuBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
    }

    public void show() {
        initView();
        initEvent();
    }

    private void initView() {
        setTextView();
        setDialog();
        binding.site.requestFocus();
    }

    private void initEvent() {
        binding.site.setOnClickListener(this::showSiteDialog);
        binding.settingVodHistory.setOnClickListener(this::showSettingVodHistory);
        binding.history.setOnClickListener(this::startHistory);
    }

    private void setTextView() {
        homeMenuKey = ResUtil.getStringArray(R.array.select_home_menu_key);
        binding.siteText.setText(homeMenuKey[1]);
        binding.settingVodHistoryText.setText(homeMenuKey[2]);
        binding.historyText.setText(homeMenuKey[3]);
    }

    private void setDialog() {
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * 0.4f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void showSiteDialog(View view) {
        dialog.dismiss();
        if (activity instanceof HomeActivity) ((HomeActivity) activity).showDialog();
    }

    private void showSettingVodHistory(View view) {
        dialog.dismiss();
        if (activity instanceof HomeActivity) ((HomeActivity) activity).showSettingVodHistory();
    }

    private void startHistory(View view) {
        dialog.dismiss();
        HistoryActivity.start(activity);
    }
}
