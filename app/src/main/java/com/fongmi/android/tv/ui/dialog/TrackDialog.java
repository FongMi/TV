package com.fongmi.android.tv.ui.dialog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Tracks;
import androidx.media3.ui.DefaultTrackNameProvider;
import androidx.media3.ui.TrackNameProvider;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.databinding.DialogTrackBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.track.TrackUtil;
import com.fongmi.android.tv.ui.adapter.TrackAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public final class TrackDialog extends BaseBottomSheetDialog implements TrackAdapter.OnClickListener {

    private final TrackNameProvider provider;
    private TrackAdapter adapter;
    private DialogTrackBinding binding;
    private PlayerManager player;
    private Uri pendingSubtitle;
    private int type;

    public TrackDialog() {
        this.provider = new DefaultTrackNameProvider(App.get().getResources());
    }

    public static TrackDialog create() {
        return new TrackDialog();
    }

    public TrackDialog type(int type) {
        Bundle arguments = new Bundle();
        arguments.putInt("type", type);
        setArguments(arguments);
        return this;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        type = requireArguments().getInt("type");
        if (savedInstanceState != null) pendingSubtitle = savedInstanceState.getParcelable("subtitle");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("subtitle", pendingSubtitle);
    }

    public void show(FragmentActivity activity) {
        if (activity.getSupportFragmentManager().isStateSaved()) return;
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof TrackDialog) return;
        show(activity.getSupportFragmentManager(), null);
    }

    private boolean hasChoose() {
        return type == C.TRACK_TYPE_TEXT && player.isVod();
    }

    private boolean hasSearch() {
        return type == C.TRACK_TYPE_TEXT && player.isVod();
    }

    private boolean hasSetting() {
        return type == C.TRACK_TYPE_AUDIO || type == C.TRACK_TYPE_VIDEO || type == C.TRACK_TYPE_TEXT;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogTrackBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        PlaybackDialog.observe(this, this::bindPlayer);
    }

    private void bindPlayer(PlayerManager player) {
        this.player = player;
        if (player == null) return;
        adapter = new TrackAdapter(this);
        setRecyclerView();
        binding.search.setVisibility(hasSearch() ? View.VISIBLE : View.GONE);
        binding.choose.setVisibility(hasChoose() ? View.VISIBLE : View.GONE);
        binding.setting.setVisibility(hasSetting() ? View.VISIBLE : View.GONE);
        binding.title.setText(ResUtil.getStringArray(R.array.select_track)[type - 1]);
        applyPendingSubtitle();
    }

    @Override
    protected void initEvent() {
        binding.search.setOnClickListener(this::onSearch);
        binding.choose.setOnClickListener(this::onChoose);
        binding.setting.setOnClickListener(this::onSetting);
    }

    private void setRecyclerView() {
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(adapter.addAll(getTrack()));
        int selected = adapter.getSelected();
        binding.recycler.post(() -> binding.recycler.scrollToPosition(selected));
        binding.recycler.setVisibility(adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
    }

    private void onSearch(View view) {
        FragmentActivity activity = requireActivity();
        dismissNow();
        SubtitleSearchDialog.create().show(activity);
    }

    private void onChoose(View view) {
        FileChooser.from(launcher).show(new String[]{MimeTypes.APPLICATION_SUBRIP, MimeTypes.TEXT_SSA, MimeTypes.TEXT_VTT, MimeTypes.APPLICATION_TTML, "audio/*", "text/*", "application/octet-stream"});
        player.pause();
    }

    private void onSetting(View view) {
        FragmentActivity activity = requireActivity();
        dismissNow();
        showSetting(activity);
    }

    private void showSetting(FragmentActivity activity) {
        switch (type) {
            case C.TRACK_TYPE_AUDIO -> AudioSettingDialog.create().show(activity);
            case C.TRACK_TYPE_VIDEO -> VideoSettingDialog.create().show(activity);
            case C.TRACK_TYPE_TEXT -> SubtitleSettingDialog.create().show(activity);
        }
    }

    private List<Track> getTrack() {
        List<Track> items = new ArrayList<>();
        addTrack(items);
        return items;
    }

    private void addTrack(List<Track> items) {
        List<Tracks.Group> groups = player.getCurrentTracks().getGroups();
        for (int i = 0; i < groups.size(); i++) {
            Tracks.Group trackGroup = groups.get(i);
            if (trackGroup.getType() != type) continue;
            for (int j = 0; j < trackGroup.length; j++) {
                Format format = trackGroup.getTrackFormat(j);
                String name = provider.getTrackName(format);
                Track item = new Track(type, name, TrackUtil.describeFormat(format));
                item.setSelected(trackGroup.isTrackSelected(j));
                items.add(item);
            }
        }
    }

    @Override
    public void onItemClick(Track item) {
        player.setTrack(item.key(player.getKey()).save());
        dismiss();
    }

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> FileChooser.getUri(result, this::setSubtitle));

    private void setSubtitle(Uri uri) {
        if (!isAdded()) return;
        pendingSubtitle = uri;
        applyPendingSubtitle();
    }

    private void applyPendingSubtitle() {
        if (pendingSubtitle == null || !PlaybackDialog.isCurrentPlayer(this, player)) return;
        Uri uri = pendingSubtitle;
        pendingSubtitle = null;
        player.setSub(Sub.from(FileUtil.getDisplayName(uri), uri.toString()));
        dismiss();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        adapter = null;
        player = null;
        super.onDestroyView();
    }
}
