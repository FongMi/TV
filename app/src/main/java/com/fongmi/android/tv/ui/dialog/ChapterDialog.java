package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.media3.common.MediaChapter;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogChapterBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.ui.adapter.ChapterAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;

public final class ChapterDialog extends BaseBottomSheetDialog implements ChapterAdapter.OnClickListener {

    private ChapterAdapter adapter;
    private DialogChapterBinding binding;
    private PlayerManager player;

    public static ChapterDialog create() {
        return new ChapterDialog();
    }

    public void show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.isStateSaved()) return;
        for (Fragment fragment : manager.getFragments()) if (fragment instanceof ChapterDialog) return;
        show(manager, null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogChapterBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.title.setText(R.string.dialog_select_chapter);
        PlaybackDialog.observe(this, player -> {
            this.player = player;
            if (player == null) return;
            adapter = new ChapterAdapter(this);
            binding.recycler.setAdapter(adapter.addAll(player.getCurrentMediaChapters()));
            binding.recycler.scrollToPosition(adapter.getSelected());
            binding.recycler.setVisibility(adapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onItemClick(MediaChapter item) {
        player.selectChapter(item);
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
