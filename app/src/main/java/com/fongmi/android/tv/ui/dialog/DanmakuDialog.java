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
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.databinding.DialogDanmakuBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.setting.DanmakuSetting;
import com.fongmi.android.tv.ui.adapter.DanmakuAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.FileUtil;

public final class DanmakuDialog extends BaseBottomSheetDialog implements DanmakuAdapter.OnClickListener {

    private DanmakuAdapter adapter;
    private DialogDanmakuBinding binding;
    private PlayerManager player;
    private Uri pendingDanmaku;

    public static DanmakuDialog create() {
        return new DanmakuDialog();
    }

    public void show(FragmentActivity activity) {
        if (activity.getSupportFragmentManager().isStateSaved()) return;
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof DanmakuDialog) return;
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) pendingDanmaku = savedInstanceState.getParcelable("danmaku");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("danmaku", pendingDanmaku);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogDanmakuBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        PlaybackDialog.observe(this, player -> {
            this.player = player;
            if (player == null) return;
            adapter = new DanmakuAdapter(this);
            binding.recycler.setAdapter(adapter.addAll(player.getDanmakus()));
            binding.recycler.scrollToPosition(adapter.getSelected());
            binding.recycler.setVisibility(adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
            binding.search.setVisibility(player.getMetadata() == null || DanmakuSetting.getEffectiveApiUrl().isEmpty() ? View.GONE : View.VISIBLE);
            applyPendingDanmaku();
        });
    }

    @Override
    protected void initEvent() {
        binding.search.setOnClickListener(this::onSearch);
        binding.choose.setOnClickListener(this::onChoose);
        binding.setting.setOnClickListener(this::onSetting);
    }

    private void onSearch(View view) {
        DanmakuSearchDialog.create().show(requireActivity());
        dismiss();
    }

    private void onChoose(View view) {
        FileChooser.from(launcher).show(new String[]{"text/*"});
        player.pause();
    }

    private void onSetting(View view) {
        DanmakuSettingDialog.create().show(getActivity());
        dismiss();
    }

    @Override
    public void onItemClick(Danmaku item) {
        player.toggleDanmaku(item);
        dismiss();
    }

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> FileChooser.getUri(result, this::setDanmaku));

    private void setDanmaku(Uri uri) {
        if (!isAdded()) return;
        pendingDanmaku = uri;
        applyPendingDanmaku();
    }

    private void applyPendingDanmaku() {
        if (pendingDanmaku == null || !PlaybackDialog.isCurrentPlayer(this, player)) return;
        Uri uri = pendingDanmaku;
        pendingDanmaku = null;
        player.setDanmaku(Danmaku.from(FileUtil.getDisplayName(uri), uri.toString()));
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
