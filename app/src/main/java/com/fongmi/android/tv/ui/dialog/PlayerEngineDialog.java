package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogPlayerEngineBinding;
import com.fongmi.android.tv.playback.PlaybackAction;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.ui.activity.PlaybackActivity;

public final class PlayerEngineDialog extends BaseBottomSheetDialog {

    private DialogPlayerEngineBinding binding;
    private PlayerManager player;

    public static void setText(TextView view) {
        setText(view, null);
    }

    public static void setText(TextView view, PlayerManager player) {
        if (view == null) return;
        view.setText(PlaybackAction.getEngineText(player));
    }

    public static void show(FragmentActivity activity) {
        if (activity.getSupportFragmentManager().isStateSaved()) return;
        for (Fragment fragment : activity.getSupportFragmentManager().getFragments()) if (fragment instanceof PlayerEngineDialog) return;
        new PlayerEngineDialog().show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogPlayerEngineBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        PlaybackDialog.observe(this, player -> {
            this.player = player;
            if (player == null) return;
            setSelected();
            getSelectedView().requestFocus();
        });
    }

    @Override
    protected void initEvent() {
        binding.debug.setOnClickListener(this::selectDebug);
        binding.other.setOnClickListener(this::selectOther);
        binding.exo.setOnClickListener(view -> selectEngine(PlayerSetting.ENGINE_EXO));
        binding.mpv.setOnClickListener(view -> selectEngine(PlayerSetting.ENGINE_MPV));
    }

    private void selectDebug(View view) {
        PlaybackActivity activity = getPlaybackActivity();
        if (activity == null) return;
        activity.toggleDebugView();
        dismiss();
    }

    private void selectOther(View view) {
        PlaybackActivity activity = getPlaybackActivity();
        if (activity != null) activity.onChoose();
        dismiss();
    }

    private void selectEngine(int engine) {
        player.setEngine(engine);
        dismiss();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        player = null;
        super.onDestroyView();
    }

    private void setSelected() {
        int engine = PlaybackAction.getEngine(player);
        binding.exo.setSelected(engine == PlayerSetting.ENGINE_EXO);
        binding.mpv.setSelected(engine == PlayerSetting.ENGINE_MPV);
    }

    private View getSelectedView() {
        return PlaybackAction.getEngine(player) == PlayerSetting.ENGINE_MPV ? binding.mpv : binding.exo;
    }

    private PlaybackActivity getPlaybackActivity() {
        FragmentActivity activity = getActivity();
        return activity instanceof PlaybackActivity owner ? owner : null;
    }
}
