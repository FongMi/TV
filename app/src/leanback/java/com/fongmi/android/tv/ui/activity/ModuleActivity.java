package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.ActivityModuleBinding;
import com.fongmi.android.tv.databinding.ItemModuleBinding;
import com.fongmi.android.tv.model.ModuleViewModel;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.ModuleDialog;
import com.fongmi.android.tv.utils.ModuleUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.RuntimeInstaller;
import com.github.catvod.utils.RuntimeModule;

public class ModuleActivity extends BaseActivity {

    private ActivityModuleBinding mBinding;
    private ModuleViewModel[] models;
    private ItemModuleBinding[] items;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, ModuleActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityModuleBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        models = new ModuleViewModel[]{ModuleViewModel.get(this, RuntimeModule.BDJ), ModuleViewModel.get(this, RuntimeModule.PYTHON), ModuleViewModel.get(this, RuntimeModule.NODEJS), ModuleViewModel.get(this, RuntimeModule.QUICKJS), ModuleViewModel.get(this, RuntimeModule.THUNDER), ModuleViewModel.get(this, RuntimeModule.MPV)};
        items = new ItemModuleBinding[]{mBinding.bdj, mBinding.python, mBinding.nodejs, mBinding.quickjs, mBinding.thunder, mBinding.mpv};
        for (int i = 0; i < items.length; i++) bind(items[i], models[i], i);
        mBinding.check.requestFocus();
    }

    @Override
    protected void initEvent() {
        mBinding.check.setOnClickListener(this::onCheck);
    }

    private void onCheck(View view) {
        if (ModuleUtil.isBusy(models)) return;
        for (ModuleViewModel model : models) model.check();
    }

    private void bind(ItemModuleBinding binding, ModuleViewModel model, int index) {
        binding.getRoot().setOnClickListener(view -> select(index));
        binding.icon.setImageResource(model.getIcon());
        binding.title.setText(model.getTitle());
        model.getState().observe(this, state -> render(binding, model, state));
    }

    private void select(int index) {
        ModuleViewModel model = models[index];
        ModuleViewModel.State state = model.getState().getValue();
        if (state.isPending()) confirmInstallation(model, state);
        else if (state.canInstall()) model.install();
        else ModuleDialog.create(index).show(this);
    }

    private void confirmInstallation(ModuleViewModel model, ModuleViewModel.State state) {
        Notify.show(model.getStatusText(state));
        RuntimeInstaller.confirmInstallation(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (ModuleViewModel model : models) model.refresh();
    }

    @Override
    protected void onDestroy() {
        for (ItemModuleBinding item : items) item.progress.setWaveSpeed(0);
        super.onDestroy();
    }

    private void render(ItemModuleBinding binding, ModuleViewModel model, ModuleViewModel.State state) {
        ModuleUtil.render(binding, model, state);
        mBinding.check.setClickable(!ModuleUtil.isBusy(models));
    }
}
