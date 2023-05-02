package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.databinding.AdapterTypeBinding;
import com.fongmi.android.tv.utils.ResUtil;

public class TypePresenter extends Presenter {

    private final OnClickListener mListener;

    public TypePresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onItemClick(Class item);

        boolean onLongClick(Class item);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterTypeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Class item = (Class) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(item.getTypeName());
        holder.binding.text.setCompoundDrawablePadding(ResUtil.dp2px(4));
        holder.binding.text.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, getIcon(item), 0);
        holder.view.setOnLongClickListener(view -> mListener.onLongClick(item));
        setOnClickListener(holder, view -> mListener.onItemClick(item));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    private int getIcon(Class item) {
        return item.getFilter() == null ? 0 : item.getFilter() ? R.drawable.ic_vod_filter_off : R.drawable.ic_vod_filter_on;
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterTypeBinding binding;

        public ViewHolder(@NonNull AdapterTypeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}