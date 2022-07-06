package com.fongmi.bear.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.bear.bean.Vod;
import com.fongmi.bear.databinding.AdapterItemBinding;

public class ItemPresenter extends Presenter {

    private OnClickListener mListener;

    public interface OnClickListener {
        void onItemClick(Vod.Flag.Episode item);
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
        if (object instanceof Vod.Flag) {
            holder.binding.text.setText(((Vod.Flag) object).getFlag());
        } else if (object instanceof Vod.Flag.Episode) {
            holder.binding.text.setText(((Vod.Flag.Episode) object).getName());
        } else {
            holder.binding.text.setText(object.toString());
        }
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