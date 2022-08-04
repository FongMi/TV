package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

public class VodPresenter extends Presenter {

    private final OnClickListener mListener;
    private int width, height;

    public VodPresenter(OnClickListener listener, int columns) {
        this.mListener = listener;
        setLayoutSize(columns);
    }

    public interface OnClickListener {
        void onItemClick(Vod item);
    }

    private void setLayoutSize(int columns) {
        int space = ResUtil.dp2px(16) * (columns - 1) + ResUtil.dp2px(48);
        int base = ResUtil.getScreenWidthPx() - space;
        width = base / columns;
        height = (int) (width / 0.75f);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        ViewHolder holder = new ViewHolder(AdapterVodBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        holder.binding.getRoot().getLayoutParams().width = width;
        holder.binding.getRoot().getLayoutParams().height = height;
        return holder;
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Vod item = (Vod) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.name.setText(item.getVodName());
        holder.binding.remark.setText(item.getVodRemarks());
        holder.binding.remark.setVisibility(item.getRemarkVisible());
        ImgUtil.load(item.getVodName(), item.getVodPic(), holder.binding.image);
        setOnClickListener(holder, view -> mListener.onItemClick(item));
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