package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.databinding.AdapterFilterBinding;

public class FilterPresenter extends Presenter {

    private OnClickListener mListener;
    private final String mKey;

    public FilterPresenter(String key) {
        mKey = key;
    }

    public interface OnClickListener {
        void onItemClick(String key, Filter.Value item);
    }

    public void setOnClickListener(OnClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterFilterBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Filter.Value item = (Filter.Value) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(item.getN());
        holder.binding.text.setActivated(item.isActivated());
        setOnClickListener(holder, view -> mListener.onItemClick(mKey, item));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterFilterBinding binding;

        public ViewHolder(@NonNull AdapterFilterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}