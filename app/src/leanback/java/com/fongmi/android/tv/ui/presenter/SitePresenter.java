package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.AdapterSiteBinding;

public class SitePresenter extends Presenter {

    private final OnClickListener mListener;

    public SitePresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onTextClick(Site item);

        void onSearchClick(Site item);

        void onFilterClick(Site item);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterSiteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Site item = (Site) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(item.getActivatedName());
        holder.binding.filter.setImageResource(item.getFilterIcon());
        holder.binding.search.setImageResource(item.getSearchIcon());
        holder.binding.text.setOnClickListener(v -> mListener.onTextClick(item));
        holder.binding.search.setOnClickListener(v -> mListener.onSearchClick(item));
        holder.binding.filter.setOnClickListener(v -> mListener.onFilterClick(item));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterSiteBinding binding;

        public ViewHolder(@NonNull AdapterSiteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}