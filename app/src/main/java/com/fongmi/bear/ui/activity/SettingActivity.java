package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.ApiConfig;
import com.fongmi.bear.bean.Site;
import com.fongmi.bear.databinding.ActivitySettingBinding;
import com.fongmi.bear.databinding.DialogConfigBinding;
import com.fongmi.bear.databinding.DialogSiteBinding;
import com.fongmi.bear.net.Callback;
import com.fongmi.bear.ui.presenter.SitePresenter;
import com.fongmi.bear.utils.Notify;
import com.fongmi.bear.utils.Prefers;
import com.fongmi.bear.utils.ResUtil;

public class SettingActivity extends BaseActivity {

    private ActivitySettingBinding mBinding;

    public static void start(Activity activity) {
        activity.startActivityForResult(new Intent(activity, SettingActivity.class), 1000);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mBinding.home.setText(ApiConfig.get().getHome().getName());
        mBinding.url.setText(Prefers.getUrl());
    }

    @Override
    protected void initEvent() {
        mBinding.config.setOnClickListener(this::showConfig);
        mBinding.site.setOnClickListener(this::showSite);
    }

    private void showConfig(View view) {
        DialogConfigBinding bindingDialog = DialogConfigBinding.inflate(LayoutInflater.from(this));
        bindingDialog.url.setText(Prefers.getUrl());
        bindingDialog.url.setSelection(bindingDialog.url.getText().length());
        AlertDialog dialog = Notify.show(this, bindingDialog.getRoot(), (dialogInterface, i) -> {
            Prefers.putUrl(bindingDialog.url.getText().toString().trim());
            mBinding.url.setText(Prefers.getUrl());
            Notify.progress(this);
            loadConfig();
        });
        bindingDialog.url.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            return true;
        });
    }

    private void loadConfig() {
        ApiConfig.get().loadConfig(new Callback() {
            @Override
            public void success() {
                mBinding.home.setText(ApiConfig.get().getHome().getName());
                setResult(RESULT_OK);
                Notify.dismiss();
            }

            @Override
            public void error(String msg) {
                Notify.dismiss();
                Notify.show(msg);
            }
        });
    }

    private void showSite(View view) {
        if (ApiConfig.get().getSites().isEmpty()) return;
        int position = ApiConfig.get().getSites().indexOf(ApiConfig.get().getHome());
        DialogSiteBinding bindingDialog = DialogSiteBinding.inflate(LayoutInflater.from(this));
        SitePresenter presenter = new SitePresenter();
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
        adapter.addAll(0, ApiConfig.get().getSites());
        presenter.setOnClickListener(item -> setSite(adapter, item));
        bindingDialog.recycler.setVerticalSpacing(ResUtil.dp2px(16));
        bindingDialog.recycler.setAdapter(new ItemBridgeAdapter(adapter));
        bindingDialog.recycler.scrollToPosition(position);
        Notify.show(this, bindingDialog.getRoot());
    }

    public void setSite(ArrayObjectAdapter adapter, Site item) {
        for (int i = 0; i < adapter.size(); i++) ((Site) adapter.get(i)).setActivated(item);
        adapter.notifyArrayItemRangeChanged(0, adapter.size());
        mBinding.home.setText(item.getName());
        ApiConfig.get().setHome(item);
        setResult(RESULT_OK);
        Notify.dismiss();
    }
}
