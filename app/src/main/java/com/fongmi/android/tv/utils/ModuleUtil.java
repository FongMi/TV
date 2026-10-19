package com.fongmi.android.tv.utils;

import android.content.Context;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ItemModuleBinding;
import com.fongmi.android.tv.model.ModuleViewModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Arrays;

public class ModuleUtil {

    public static boolean isBusy(ModuleViewModel[] models) {
        return Arrays.stream(models).anyMatch(model -> model.getState().getValue().isBusy());
    }

    public static void render(ItemModuleBinding binding, ModuleViewModel model, ModuleViewModel.State state) {
        View root = binding.getRoot();
        Context context = root.getContext();
        ModuleViewModel.Status runtime = state.runtime();
        boolean install = state.canInstall();
        boolean testing = state.phase() == ModuleViewModel.Phase.TESTING;
        boolean busy = state.isBusy() && !testing;
        String status = context.getString(testing ? model.getRuntimeText(runtime) : model.getStatusText(state));
        binding.status.setImageResource(getStatusIcon(state));
        binding.status.setContentDescription(install ? context.getString(runtime.updateAvailable() ? R.string.module_update : R.string.module_download) : status);
        binding.status.setVisibility(busy ? View.GONE : View.VISIBLE);
        binding.progress.setContentDescription(status);
        ViewCompat.setStateDescription(root, status);
        bindCancel(root, model, state.phase() == ModuleViewModel.Phase.READY);
        renderProgress(binding.progress, state, busy);
    }

    private static void bindCancel(View root, ModuleViewModel model, boolean pending) {
        root.setOnLongClickListener(pending ? view -> {
            model.cancel();
            return true;
        } : null);
        root.setLongClickable(pending);
        if (pending) ViewCompat.replaceAccessibilityAction(root, AccessibilityActionCompat.ACTION_LONG_CLICK, root.getContext().getString(R.string.module_cancel), (view, arguments) -> view.performLongClick());
        else ViewCompat.removeAccessibilityAction(root, AccessibilityActionCompat.ACTION_LONG_CLICK.getId());
    }

    private static int getStatusIcon(ModuleViewModel.State state) {
        ModuleViewModel.Status runtime = state.runtime();
        if (state.canInstall()) return runtime.updateAvailable() ? R.drawable.ic_module_update : R.drawable.ic_sync_download;
        return runtime != null && runtime.abi() != null && runtime.installedVersion() != null && !runtime.restartRequired() && !state.isPending() ? R.drawable.ic_action_check : R.drawable.ic_module_info;
    }

    public static void renderProgress(CircularProgressIndicator progress, ModuleViewModel.State state, boolean visible) {
        boolean downloading = state.phase() == ModuleViewModel.Phase.INSTALLING && state.total() > 0 && state.downloaded() >= 0 && state.downloaded() < state.total();
        progress.setIndeterminate(!downloading);
        progress.setWaveSpeed(downloading ? progress.getWavelengthDeterminate() : 0);
        if (downloading) progress.setProgressCompat((int) (100L * state.downloaded() / state.total()), true);
        progress.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
