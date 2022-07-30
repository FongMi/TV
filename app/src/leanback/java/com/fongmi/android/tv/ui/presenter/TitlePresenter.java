package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.databinding.AdapterTitleBinding;
import com.fongmi.android.tv.utils.ResUtil;

public class TitlePresenter extends Presenter {

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new TitlePresenter.ViewHolder(AdapterTitleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        TitlePresenter.ViewHolder holder = (TitlePresenter.ViewHolder) viewHolder;
        holder.binding.text.setText(ResUtil.getString((int) object));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterTitleBinding binding;

        public ViewHolder(@NonNull AdapterTitleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}