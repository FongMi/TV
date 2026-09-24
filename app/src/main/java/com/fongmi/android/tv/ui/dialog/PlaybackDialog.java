package com.fongmi.android.tv.ui.dialog;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.ui.activity.PlaybackActivity;

final class PlaybackDialog {

    private PlaybackDialog() {
    }

    static void observe(Fragment fragment, Observer<PlayerManager> observer) {
        PlaybackActivity activity = (PlaybackActivity) fragment.requireActivity();
        View view = fragment.requireView();
        view.setVisibility(View.INVISIBLE);
        activity.getPlaybackPlayerState().observe(fragment.getViewLifecycleOwner(), player -> {
            view.setVisibility(player == null ? View.INVISIBLE : View.VISIBLE);
            observer.onChanged(player);
        });
    }
}
