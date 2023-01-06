package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.databinding.DialogTrackBinding;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.ui.adapter.TrackAdapter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.exoplayer2.Tracks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.misc.IjkTrackInfo;

public final class TrackSelectionDialog2 implements TrackAdapter.OnClickListener {

    private final DialogTrackBinding binding;
    private final TrackNameProvider provider;
    private final TrackAdapter adapter;
    private final AlertDialog dialog;
    private Players player;
    private int type;

    public static TrackSelectionDialog2 create(Activity activity) {
        return new TrackSelectionDialog2(activity);
    }

    public TrackSelectionDialog2(Activity activity) {
        this.binding = DialogTrackBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        this.adapter = new TrackAdapter(this);
        this.provider = new TrackNameProvider();
    }

    public TrackSelectionDialog2 type(int type) {
        this.type = type;
        return this;
    }

    public TrackSelectionDialog2 player(Players player) {
        this.player = player;
        return this;
    }

    public void show() {
        setRecyclerView();
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.recycler.setAdapter(adapter.addAll(getTrack()));
        binding.recycler.scrollToPosition(adapter.getSelected());
    }

    private void setDialog() {
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidthPx() * 0.4f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private List<Track> getTrack() {
        List<Track> items = new ArrayList<>();
        if (player.isExo()) addExoTrack(items);
        if (player.isIjk()) addIjkTrack(items);
        return items;
    }

    private void addExoTrack(List<Track> items) {
        for (Tracks.Group trackGroup : player.exo().getCurrentTracks().getGroups()) {
            if (trackGroup.getType() != type) continue;
            for (int i = 0; i < trackGroup.length; i++) {
                items.add(new Track(player.getPlayer(), provider.getTrackName(trackGroup.getTrackFormat(i))));
            }
        }
    }

    private void addIjkTrack(List<Track> items) {
        int track = player.ijk().getSelectedTrack(type);
        IjkTrackInfo[] trackInfos = player.ijk().getTrackInfo();
        for (int index = 0; index < trackInfos.length; index++) {
            IjkTrackInfo trackInfo = trackInfos[index];
            if (trackInfo.getTrackType() != type) continue;
            Track item = new Track(player.getPlayer(), provider.getTrackName(trackInfo));
            item.setSelected(track == index);
            item.setIndex(index);
            items.add(item);
        }
    }

    @Override
    public void onItemClick(Track item) {
        if (player.isExo()) {

        }
        if (player.isIjk()) {
            player.ijk().selectTrack(item.getIndex());
        }
        dialog.dismiss();
    }
}