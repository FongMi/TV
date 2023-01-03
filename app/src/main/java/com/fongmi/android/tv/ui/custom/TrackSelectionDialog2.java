package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.databinding.DialogTrackBinding;
import com.fongmi.android.tv.player.ExoUtil;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.ui.adapter.TrackAdapter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class TrackSelectionDialog2 implements TrackAdapter.OnClickListener {

    private final DialogTrackBinding binding;
    private final TrackAdapter adapter;
    private final AlertDialog dialog;
    private int trackType;

    public static TrackSelectionDialog2 create(Activity activity) {
        return new TrackSelectionDialog2(activity);
    }

    public TrackSelectionDialog2(Activity activity) {
        this.binding = DialogTrackBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        this.adapter = new TrackAdapter(this);
    }

    public void show() {
        setRecyclerView();
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.recycler.setAdapter(adapter);
    }

    private void setDialog() {
        if (adapter.getItemCount() == 0) return;
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidthPx() * 0.4f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    public static boolean haveTrack(Players player, int type) {
        if (player.isExo()) {
            return ExoUtil.haveTrack(player.exo().getCurrentTracks(), type);
        } else {
            return player.ijk().haveTrack(type);
        }
    }

    @Override
    public void onItemClick(Track item) {

    }
}