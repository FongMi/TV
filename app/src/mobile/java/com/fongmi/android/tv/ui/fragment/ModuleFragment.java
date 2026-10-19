package com.fongmi.android.tv.ui.fragment;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.FragmentModuleBinding;
import com.fongmi.android.tv.databinding.ItemModuleBinding;
import com.fongmi.android.tv.model.ModuleViewModel;
import com.fongmi.android.tv.ui.activity.HomeActivity;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.dialog.ModuleDialog;
import com.fongmi.android.tv.utils.ModuleUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.RuntimeInstaller;
import com.github.catvod.utils.RuntimeModule;

public class ModuleFragment extends BaseFragment {

    private FragmentModuleBinding mBinding;
    private ModuleViewModel[] models;
    private ItemModuleBinding[] items;

    public static ModuleFragment newInstance() {
        return new ModuleFragment();
    }

    private HomeActivity getRoot() {
        return (HomeActivity) requireActivity();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentModuleBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mBinding.toolbar.getMenu().add(R.string.module_check).setIcon(R.drawable.ic_setting_refresh).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        mBinding.toolbar.setOnMenuItemClickListener(this::onCheck);
        models = new ModuleViewModel[]{ModuleViewModel.get(getRoot(), RuntimeModule.BDJ), ModuleViewModel.get(getRoot(), RuntimeModule.PYTHON), ModuleViewModel.get(getRoot(), RuntimeModule.NODEJS), ModuleViewModel.get(getRoot(), RuntimeModule.QUICKJS), ModuleViewModel.get(getRoot(), RuntimeModule.THUNDER), ModuleViewModel.get(getRoot(), RuntimeModule.MPV)};
        items = new ItemModuleBinding[]{mBinding.bdj, mBinding.python, mBinding.nodejs, mBinding.quickjs, mBinding.thunder, mBinding.mpv};
        for (int i = 0; i < items.length; i++) bind(items[i], models[i], i);
    }

    private boolean onCheck(MenuItem item) {
        for (ModuleViewModel model : models) model.check();
        return true;
    }

    private void bind(ItemModuleBinding binding, ModuleViewModel model, int index) {
        binding.icon.setImageResource(model.getIcon());
        binding.title.setText(model.getTitle());
        binding.getRoot().setOnClickListener(view -> select(index));
        model.getState().observe(getViewLifecycleOwner(), state -> render(binding, model, state));
    }

    private void select(int index) {
        ModuleViewModel model = models[index];
        ModuleViewModel.State state = model.getState().getValue();
        if (state.isPending()) confirmInstallation(model, state);
        else if (state.canInstall()) model.install();
        else ModuleDialog.create(index).show(getRoot());
    }

    private void confirmInstallation(ModuleViewModel model, ModuleViewModel.State state) {
        Notify.show(model.getStatusText(state));
        RuntimeInstaller.confirmInstallation(getRoot());
    }

    private void render(ItemModuleBinding binding, ModuleViewModel model, ModuleViewModel.State state) {
        ModuleUtil.render(binding, model, state);
        mBinding.toolbar.getMenu().getItem(0).setEnabled(!ModuleUtil.isBusy(models));
    }

    private void refresh() {
        if (mBinding != null && !isHidden()) for (ModuleViewModel model : models) model.refresh();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onDestroyView() {
        for (ItemModuleBinding item : items) item.progress.setWaveSpeed(0);
        mBinding = null;
        models = null;
        items = null;
        super.onDestroyView();
    }
}
