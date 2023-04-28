package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterFlagBinding;

public class FlagPresenter extends Presenter {

    private final OnClickListener mListener;

    public FlagPresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {
        void onItemClick(Vod.Flag item);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterFlagBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Vod.Flag item = (Vod.Flag) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(item.getShow());
        holder.binding.text.setActivated(item.isActivated());
        setOnClickListener(holder, view -> mListener.onItemClick(item));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterFlagBinding binding;

        public ViewHolder(@NonNull AdapterFlagBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}