package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.AdapterSiteBinding;

public class SitePresenter extends Presenter {

    private final OnClickListener mListener;
    private boolean search;
    private boolean change;

    public SitePresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public SitePresenter search(boolean search) {
        this.search = search;
        return this;
    }

    public SitePresenter change(boolean change) {
        this.change = change;
        return this;
    }

    public interface OnClickListener {

        void onTextClick(Site item);

        void onSearchClick(Site item);

        void onChangeClick(Site item);

        boolean onSearchLongClick(Site item);

        boolean onChangeLongClick(Site item);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterSiteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Site item = (Site) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(item.getName());
        holder.binding.text.setEnabled(!search || change);
        holder.binding.text.setFocusable(!search || change);
        holder.binding.text.setSelected(item.isActivated());
        holder.binding.text.setActivated(item.isActivated());
        holder.binding.search.setImageResource(item.getSearchIcon());
        holder.binding.change.setImageResource(item.getChangeIcon());
        holder.binding.search.setVisibility(search ? View.VISIBLE : View.GONE);
        holder.binding.change.setVisibility(change ? View.VISIBLE : View.GONE);
        holder.binding.text.setOnClickListener(v -> mListener.onTextClick(item));
        holder.binding.search.setOnClickListener(v -> mListener.onSearchClick(item));
        holder.binding.change.setOnClickListener(v -> mListener.onChangeClick(item));
        holder.binding.search.setOnLongClickListener(v -> mListener.onSearchLongClick(item));
        holder.binding.change.setOnLongClickListener(v -> mListener.onChangeLongClick(item));
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