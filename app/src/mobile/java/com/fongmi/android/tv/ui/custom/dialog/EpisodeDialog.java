package com.fongmi.android.tv.ui.custom.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.DialogEpisodeBinding;
import com.fongmi.android.tv.ui.fragment.EpisodeFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class EpisodeDialog extends BaseDialog {

    private List<Vod.Flag.Episode> episodes;
    private DialogEpisodeBinding binding;
    private final List<String> titles;
    private boolean reverse;

    public static EpisodeDialog create() {
        return new EpisodeDialog();
    }

    public EpisodeDialog() {
        this.titles = new ArrayList<>();
    }

    public EpisodeDialog reverse(boolean reverse) {
        this.reverse = reverse;
        return this;
    }

    public EpisodeDialog episodes(List<Vod.Flag.Episode> episodes) {
        this.episodes = episodes;
        setTitles();
        return this;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogEpisodeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.pager.setAdapter(new PageAdapter(getActivity()));
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(titles.get(position))).attach();
    }

    private void setTitles() {
        if (reverse) for (int i = episodes.size() + 1; i > 0; i -= 50) titles.add((i - 1) + "-" + Math.max(i - 50, 1));
        else for (int i = 0; i < episodes.size(); i += 50) titles.add((i + 1) + "-" + Math.min(i + 50, episodes.size()));
    }

    @Override
    protected void setBehavior(BottomSheetDialog dialog) {
    }

    class PageAdapter extends FragmentStateAdapter {

        public PageAdapter(@NonNull FragmentActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return EpisodeFragment.newInstance(episodes.subList(position * 50, Math.min(position * 50 + 50, episodes.size())));
        }

        @Override
        public int getItemCount() {
            return titles.size();
        }
    }
}
