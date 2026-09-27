package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
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
import androidx.annotation.StringRes;
import androidx.core.view.OneShotPreDrawListener;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.DecoderMode;
import androidx.media3.common.text.SubtitleSelectionState;
import androidx.media3.ui.DefaultTrackNameProvider;
import androidx.media3.ui.TrackNameProvider;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.databinding.DialogDecoderModeBinding;
import com.fongmi.android.tv.databinding.DialogTrackBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.track.TrackUtil;
import com.fongmi.android.tv.setting.DecodeSetting;
import com.fongmi.android.tv.ui.adapter.TrackAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

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
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.isStateSaved()) return;
        for (Fragment fragment : manager.getFragments()) if (fragment instanceof TrackDialog) return;
        showNow(manager, null);
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
        int actionVisibility = type == C.TRACK_TYPE_TEXT && player.isVod() ? View.VISIBLE : View.GONE;
        binding.search.setVisibility(actionVisibility);
        binding.choose.setVisibility(actionVisibility);
        binding.setting.setVisibility(hasSetting() ? View.VISIBLE : View.GONE);
        binding.setting.setContentDescription(getString(switch (type) {
            case C.TRACK_TYPE_AUDIO -> R.string.audio_setting;
            case C.TRACK_TYPE_VIDEO -> R.string.video_setting;
            default -> R.string.subtitle_setting;
        }));
        binding.content.setVisibility(type == C.TRACK_TYPE_TEXT ? View.VISIBLE : View.GONE);
        binding.title.setText(ResUtil.getStringArray(R.array.select_track)[type - 1]);
        DecoderMode mode = player.getDecoderMode(type);
        binding.decoder.setVisibility(mode == null ? View.GONE : View.VISIBLE);
        if (mode != null) {
            String description = getString(R.string.track_decoder_mode, DecodeSetting.getDecoderModeText(mode));
            binding.decoder.setContentDescription(description);
        }
        applyPendingSubtitle();
    }

    @Override
    protected void initEvent() {
        binding.search.setOnClickListener(this::onSearch);
        binding.choose.setOnClickListener(this::onChoose);
        binding.setting.setOnClickListener(this::onSetting);
        binding.content.setOnClickListener(this::onContent);
        binding.decoder.setOnClickListener(this::onDecoder);
    }

    private void onDecoder(View view) {
        if (player.getDecoderMode(type) == null || player.getSupportedDecoderModes(type).isEmpty()) return;
        if (getChildFragmentManager().findFragmentByTag("decoder") != null) return;
        new DecoderSheet().showNow(getChildFragmentManager(), "decoder");
    }

    public static final class DecoderSheet extends BaseBottomSheetDialog {

        private DialogDecoderModeBinding binding;

        @Override
        public void onStart() {
            super.onStart();
            ((TrackDialog) requireParentFragment()).requireDialog().hide();
        }

        @Override
        public void onDismiss(@NonNull DialogInterface dialog) {
            super.onDismiss(dialog);
            if (getParentFragment() instanceof TrackDialog owner && owner.isAdded() && !owner.isRemoving() && owner.getDialog() != null) owner.requireDialog().show();
        }

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = DialogDecoderModeBinding.inflate(inflater, container, false);
        }

        @Override
        protected void initView() {
            PlaybackDialog.observe(this, player -> {
                if (player == null) return;
                TrackDialog owner = (TrackDialog) requireParentFragment();
                List<DecoderMode> modes = player.getSupportedDecoderModes(owner.type);
                DecoderMode current = player.getDecoderMode(owner.type);
                binding.title.setText(owner.type == C.TRACK_TYPE_AUDIO ? R.string.player_audio_decoder_mode : R.string.player_video_decoder_mode);
                bind(binding.auto, DecoderMode.AUTO, modes, current, owner);
                bind(binding.hardware, DecoderMode.HARDWARE, modes, current, owner);
                bind(binding.software, DecoderMode.SOFTWARE, modes, current, owner);
                bind(binding.ffmpeg, DecoderMode.FFMPEG, modes, current, owner);
            });
        }

        @Override
        public void onDestroyView() {
            binding = null;
            super.onDestroyView();
        }

        private void bind(View view, DecoderMode mode, List<DecoderMode> modes, DecoderMode current, TrackDialog owner) {
            view.setVisibility(modes.contains(mode) ? View.VISIBLE : View.GONE);
            view.setSelected(mode == current);
            view.setOnClickListener(v -> {
                owner.player.setDecoderMode(owner.type, mode);
                owner.dismiss();
            });
            if (mode == current && view.getVisibility() == View.VISIBLE) view.requestFocus();
        }
    }

    private void setRecyclerView() {
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(adapter.addAll(getTrack()));
        binding.recycler.setVisibility(adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
        if (adapter.getItemCount() == 0) return;
        int selected = adapter.getSelected();
        // Focus before drawing when the selected item is ready.
        binding.recycler.getLayoutManager().scrollToPosition(selected);
        if (Util.isLeanback()) {
            binding.getRoot().setFocusableInTouchMode(true);
            binding.getRoot().requestFocus();
            OneShotPreDrawListener.add(binding.recycler, () -> {
                View view = binding.recycler.getLayoutManager().findViewByPosition(selected);
                if (view == null || !view.requestFocus()) binding.recycler.scrollToPosition(selected);
                binding.getRoot().setFocusableInTouchMode(false);
                binding.getRoot().setFocusable(false);
            });
        }
    }

    private void onSearch(View view) {
        FragmentActivity activity = requireActivity();
        dismissNow();
        SubtitleSearchDialog.create().show(activity);
    }

    private void onContent(View view) {
        FragmentActivity activity = requireActivity();
        dismissNow();
        PlaybackContentDialog.create().type(PlaybackContentDialog.SUBTITLE).show(activity);
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

    private List<TrackAdapter.TrackItem> getTrack() {
        List<TrackAdapter.TrackItem> items = new ArrayList<>();
        SubtitleSelectionState subtitleState = type == C.TRACK_TYPE_TEXT ? player.getSubtitleSelectionState() : SubtitleSelectionState.EMPTY;
        int ordinal = 0;
        for (Tracks.Group trackGroup : player.getCurrentTracks().getGroups()) {
            if (trackGroup.getType() != type) continue;
            for (int j = 0; j < trackGroup.length; j++) {
                Format format = trackGroup.getTrackFormat(j);
                boolean selected = trackGroup.isTrackSelected(j);
                String name = provider.getTrackName(format);
                Track track = TrackUtil.createTrack(type, name, format, ordinal++);
                track.setSelected(selected);
                int role = selected ? getSubtitleRole(subtitleState, trackGroup, j) : 0;
                track.setRole(role == R.string.subtitle_secondary_role ? Track.ROLE_SECONDARY : Track.ROLE_PRIMARY);
                items.add(new TrackAdapter.TrackItem(track, role));
            }
        }
        return items;
    }

    @StringRes
    private static int getSubtitleRole(SubtitleSelectionState state, Tracks.Group group, int trackIndex) {
        if (state.activeSecondarySelection == null) return 0;
        TrackSelectionOverride selection = new TrackSelectionOverride(group.getMediaTrackGroup(), trackIndex);
        if (state.isPrimary(selection)) return R.string.subtitle_primary_role;
        return state.isActiveSecondary(selection) ? R.string.subtitle_secondary_role : 0;
    }

    @Override
    public void onItemClick(Track item) {
        player.setTrack(item.key(player.getKey()));
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
