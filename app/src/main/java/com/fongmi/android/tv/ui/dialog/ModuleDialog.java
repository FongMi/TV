package com.fongmi.android.tv.ui.dialog;

import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.databinding.DialogModuleBinding;
import com.fongmi.android.tv.model.ModuleViewModel;
import com.fongmi.android.tv.ui.activity.RestartActivity;
import com.fongmi.android.tv.utils.ModuleUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.bassaer.library.MDColor;
import com.github.catvod.utils.RuntimeModule;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.concurrent.TimeUnit;

public class ModuleDialog extends BaseAlertDialog {

    private DialogModuleBinding binding;
    private ModuleViewModel model;
    private boolean downloadPaused;
    private long progressTime;
    private long progressBytes;
    private long transferSpeed;

    public static ModuleDialog create(int index) {
        Bundle args = new Bundle();
        args.putInt("index", index);
        ModuleDialog dialog = new ModuleDialog();
        dialog.setArguments(args);
        return dialog;
    }

    public void show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.isStateSaved()) return;
        for (Fragment fragment : manager.getFragments()) if (fragment instanceof ModuleDialog) return;
        showNow(manager, null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) downloadPaused = savedInstanceState.getBoolean("downloadPaused");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("downloadPaused", downloadPaused);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogModuleBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        MaterialAlertDialogBuilder builder = builder().setView(getBinding().getRoot());
        return Util.isLeanback() ? builder : builder.setPositiveButton(R.string.module_test, null).setNegativeButton(R.string.module_remove, (dialog, which) -> model.remove());
    }

    @Override
    protected void initView() {
        int index = requireArguments().getInt("index");
        model = ModuleViewModel.get(requireActivity(), index == 0 ? RuntimeModule.BDJ : RuntimeModule.values()[index - 1]);
        binding.title.setText(model.getTitle());
        model.getState().observe(this, this::render);
        model.getHealth().observe(this, health -> render(model.getState().getValue()));
    }

    @Override
    protected void initEvent() {
        binding.pause.setOnClickListener(this::onPauseDownload);
        if (Util.isLeanback()) {
            getAction().setOnClickListener(view -> onPositive());
            getRemove().setOnClickListener(this::onRemove);
        }
    }

    private void onRemove(View view) {
        model.remove();
        dismiss();
    }

    private void onPauseDownload(View view) {
        if (downloadPaused) {
            resetTransfer();
            model.install();
        } else {
            downloadPaused = true;
            model.cancel();
        }
    }

    private void resetTransfer() {
        downloadPaused = false;
        progressTime = 0;
        transferSpeed = 0;
    }

    private View getRemove() {
        if (Util.isLeanback()) return binding.getRoot().findViewById(R.id.remove);
        if (!(getDialog() instanceof AlertDialog dialog) || !dialog.isShowing()) return null;
        return dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
    }

    private View getAction() {
        if (Util.isLeanback()) return binding.getRoot().findViewById(R.id.test);
        if (!(getDialog() instanceof AlertDialog dialog) || !dialog.isShowing()) return null;
        return dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    }

    private void onPositive() {
        if (model.getState().getValue().canRestart()) RestartActivity.start(requireActivity());
        else model.checkRuntime();
    }

    private void render(ModuleViewModel.State state) {
        if (state.phase() == ModuleViewModel.Phase.INSTALLING && state.downloaded() < 0) resetTransfer();
        ModuleViewModel.Status runtime = state.runtime();
        boolean installed = runtime != null && runtime.installedVersion() != null;
        ModuleViewModel.Health health = model.getHealth().getValue();
        String error = state.error() == null ? health.error() : state.error();
        binding.status.setText(state.phase() == ModuleViewModel.Phase.TESTING ? model.getRuntimeText(runtime) : model.getStatusText(state));
        renderTransfer(state);
        renderHealth(health, installed);
        binding.version.setVisibility(installed ? View.VISIBLE : View.GONE);
        if (installed) binding.version.setText(getString(R.string.module_version, runtime.installedVersion()));
        binding.progress.setContentDescription(getString(model.getStatusText(state)));
        ModuleUtil.renderProgress(binding.progress, state, state.isBusy());
        binding.error.setVisibility(error == null ? View.GONE : View.VISIBLE);
        if (error != null) binding.error.setText(getString(R.string.module_failed, error));
        renderActions(state, installed, runtime != null && runtime.restartRequired());
    }

    private void renderTransfer(ModuleViewModel.State state) {
        boolean downloading = state.phase() == ModuleViewModel.Phase.INSTALLING && state.total() > 0 && state.downloaded() >= 0 && state.downloaded() < state.total();
        boolean resumable = downloadPaused && state.canInstall();
        if (resumable) binding.status.setText(R.string.transfer_paused);
        updateTransferSpeed(state.downloaded(), downloading);
        String detail = downloading ? Updater.transferText(state.total(), state.downloaded(), transferSpeed) : "";
        binding.transfer.setText(detail);
        binding.transfer.setVisibility(detail.isEmpty() ? View.GONE : View.VISIBLE);
        binding.pause.setText(downloadPaused ? R.string.transfer_resume : R.string.pause);
        binding.pause.setVisibility(downloading || resumable || downloadPaused && state.phase() == ModuleViewModel.Phase.CANCELLING ? View.VISIBLE : View.GONE);
        binding.pause.setEnabled(resumable || downloading && !downloadPaused);
    }

    private void updateTransferSpeed(long downloaded, boolean downloading) {
        if (downloading) {
            long now = System.nanoTime();
            if (progressTime == 0) {
                progressTime = now;
                progressBytes = downloaded;
            } else if (now - progressTime >= TimeUnit.MILLISECONDS.toNanos(500)) {
                transferSpeed = (long) ((double) Math.max(0, downloaded - progressBytes) * TimeUnit.SECONDS.toNanos(1) / (now - progressTime));
                progressTime = now;
                progressBytes = downloaded;
            }
        } else {
            progressTime = 0;
        }
    }

    private void renderHealth(ModuleViewModel.Health health, boolean installed) {
        Rect bounds = new Rect();
        String status = binding.status.getText().toString();
        binding.status.getPaint().getTextBounds(status, 0, status.length(), bounds);
        binding.health.setBaseline(Math.round((binding.health.getLayoutParams().height - bounds.top - bounds.bottom) / 2f));
        boolean passed = health.phase() == ModuleViewModel.HealthPhase.PASSED;
        binding.health.setVisibility(installed && (passed || health.phase() == ModuleViewModel.HealthPhase.FAILED) ? View.VISIBLE : View.INVISIBLE);
        binding.health.setImageResource(passed ? R.drawable.ic_action_check : R.drawable.ic_module_info);
        binding.health.setImageTintList(ColorStateList.valueOf(passed ? MDColor.GREEN_700 : MDColor.RED_600));
        binding.health.setContentDescription(getString(model.getHealthText(health)));
    }

    private void renderActions(ModuleViewModel.State state, boolean installed, boolean restart) {
        boolean remove = state.canRemove();
        View button = getRemove();
        if (button == null) return;
        TextView action = (TextView) getAction();
        boolean focused = binding.getRoot().hasFocus() || button.hasFocus() || action.hasFocus();
        boolean removeFocused = button.hasFocus();
        boolean pauseFocused = binding.pause.hasFocus();
        button.setVisibility(remove ? View.VISIBLE : View.GONE);
        action.setVisibility(installed || restart ? View.VISIBLE : View.GONE);
        action.setText(restart ? R.string.module_restart_action : R.string.module_test);
        action.setEnabled(state.canTest() || state.canRestart());
        if (Util.isLeanback()) {
            ((View) action.getParent()).setVisibility(installed || restart || remove ? View.VISIBLE : View.GONE);
            binding.getRoot().setFocusable(!action.isEnabled() && !remove);
            if (focused && action.isEnabled() && !removeFocused && !pauseFocused) {
                action.requestFocus();
            } else if (focused && !binding.getRoot().hasFocus() && !button.hasFocus() && !action.hasFocus()) {
                if (action.isEnabled()) action.requestFocus();
                else if (remove) button.requestFocus();
                else binding.getRoot().requestFocus();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!Util.isLeanback()) getAction().setOnClickListener(view -> onPositive());
        render(model.getState().getValue());
        if (Util.isLeanback()) {
            if (binding.pause.getVisibility() == View.VISIBLE && binding.pause.isEnabled()) binding.pause.requestFocus();
            else if (getAction().isEnabled()) getAction().requestFocus();
            else if (model.getState().getValue().canRemove()) getRemove().requestFocus();
            else binding.getRoot().requestFocus();
        }
    }

    @Override
    public void onDestroyView() {
        model.getState().removeObservers(this);
        model.getHealth().removeObservers(this);
        binding.progress.setWaveSpeed(0);
        binding = null;
        model = null;
        super.onDestroyView();
    }
}
