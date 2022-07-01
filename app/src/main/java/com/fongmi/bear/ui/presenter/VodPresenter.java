package com.fongmi.bear.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.bear.bean.Func;
import com.fongmi.bear.bean.Vod;
import com.fongmi.bear.databinding.AdapterVodBinding;
import com.fongmi.bear.utils.Utils;

public class VodPresenter extends Presenter {

    private OnClickListener mListener;

    public interface OnClickListener {
        void onItemClick(Func item);
    }

    public void setOnClickListener(OnClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterVodBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Vod item = (Vod) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.name.setText(item.getVodName());
        holder.binding.remark.setText(item.getVodRemarks());
        holder.binding.remark.setVisibility(item.getRemarkVisible());
        Utils.loadImage(item.getVodPic(), holder.binding.image);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterVodBinding binding;

        public ViewHolder(@NonNull AdapterVodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}