package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.databinding.AdapterEpisodeBinding;
import com.fongmi.android.tv.utils.ResUtil;

public class EpisodePresenter extends Presenter {

    private final OnClickListener listener;
    private final int maxWidth;
    private int nextFocusDown;
    private int nextFocusUp;

    public EpisodePresenter(OnClickListener listener) {
        this.listener = listener;
        this.maxWidth = ResUtil.getScreenWidth() - ResUtil.dp2px(48);
    }

    public interface OnClickListener {
        void onItemClick(Episode item);
    }

    public void setNextFocusDown(int nextFocus) {
        this.nextFocusDown = nextFocus;
    }

    public void setNextFocusUp(int nextFocus) {
        this.nextFocusUp = nextFocus;
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterEpisodeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Episode item = (Episode) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setMaxWidth(maxWidth);
        holder.binding.text.setNextFocusUpId(nextFocusUp);
        holder.binding.text.setNextFocusDownId(nextFocusDown);
        holder.binding.text.setActivated(item.isActivated());
        holder.binding.text.setText(item.getDesc().concat(item.getName()));
        setOnClickListener(holder, view -> listener.onItemClick(item));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterEpisodeBinding binding;

        public ViewHolder(@NonNull AdapterEpisodeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}