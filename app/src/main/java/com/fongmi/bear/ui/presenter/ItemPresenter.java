package com.fongmi.bear.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.bear.bean.Class;
import com.fongmi.bear.databinding.AdapterItemBinding;

public class ItemPresenter extends Presenter {

    private OnClickListener mListener;

    public interface OnClickListener {
        void onItemClick(Class item);
    }

    public void setOnClickListener(OnClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(object.toString());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterItemBinding binding;

        public ViewHolder(@NonNull AdapterItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}