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
import com.fongmi.android.tv.databinding.DialogEpisodeGridBinding;
import com.fongmi.android.tv.ui.fragment.EpisodeFragment;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class EpisodeGridDialog extends BaseDialog {

    private DialogEpisodeGridBinding binding;
    private List<Vod.Flag.Episode> episodes;
    private final List<String> titles;
    private boolean reverse;
    private int spanCount;
    private int itemCount;

    public static EpisodeGridDialog create() {
        return new EpisodeGridDialog();
    }

    public EpisodeGridDialog() {
        this.titles = new ArrayList<>();
        this.spanCount = 5;
    }

    public EpisodeGridDialog reverse(boolean reverse) {
        this.reverse = reverse;
        return this;
    }

    public EpisodeGridDialog episodes(List<Vod.Flag.Episode> episodes) {
        this.episodes = episodes;
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogEpisodeGridBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setSpanCount();
        setTitles();
        setPager();
    }

    private void setSpanCount() {
        int total = 0;
        for (Vod.Flag.Episode item : episodes) total += item.getName().length();
        int offset = (int) Math.ceil((double) total / episodes.size());
        if (offset >= 12) spanCount = 1;
        else if (offset >= 8) spanCount = 2;
        else if (offset >= 4) spanCount = 3;
        else if (offset >= 2) spanCount = 4;
        itemCount = spanCount * 10;
    }

    private void setTitles() {
        if (reverse) for (int i = episodes.size(); i > 0; i -= itemCount) titles.add(i + " - " + Math.max(i - itemCount - 1, 1));
        else for (int i = 0; i < episodes.size(); i += itemCount) titles.add((i + 1) + " - " + Math.min(i + itemCount, episodes.size()));
    }

    private void setPager() {
        binding.pager.setAdapter(new PageAdapter(getActivity()));
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(titles.get(position))).attach();
        setCurrentPage();
    }

    private void setCurrentPage() {
        for (int i = 0; i < episodes.size(); i++) {
            if (episodes.get(i).isActivated()) {
                binding.pager.setCurrentItem(i / itemCount);
                break;
            }
        }
    }

    class PageAdapter extends FragmentStateAdapter {

        public PageAdapter(@NonNull FragmentActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return EpisodeFragment.newInstance(spanCount, episodes.subList(position * itemCount, Math.min(position * itemCount + itemCount, episodes.size())));
        }

        @Override
        public int getItemCount() {
            return titles.size();
        }
    }
}
