package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterEpisodeBinding;

public class EpisodePresenter extends Presenter {

    private final OnClickListener mListener;
    private int nextFocus;

    public EpisodePresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {
        void onItemClick(Vod.Flag.Episode item);
    }

    public void setNextFocusDown(int nextFocus) {
        this.nextFocus = nextFocus;
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterEpisodeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Vod.Flag.Episode item = (Vod.Flag.Episode) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(item.getName());
        holder.binding.text.setMaxEms(Product.getEms());
        holder.binding.text.setNextFocusDownId(nextFocus);
        holder.binding.text.setActivated(item.isActivated());
        setOnClickListener(holder, view -> mListener.onItemClick(item));
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