package com.fongmi.bear.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.bear.bean.Vod;
import com.fongmi.bear.databinding.AdapterVodBinding;
import com.fongmi.bear.utils.ResUtil;
import com.fongmi.bear.utils.Utils;

public class VodPresenter extends Presenter {

    private OnClickListener mListener;
    private final int mCount;

    public VodPresenter(int count) {
        mCount = count;
    }

    public interface OnClickListener {
        void onItemClick(Vod item);
    }

    public void setOnClickListener(OnClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        ViewHolder holder = new ViewHolder(AdapterVodBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        holder.binding.getRoot().getLayoutParams().width = ResUtil.dp2px(mCount == 5 ? 150 : 144);
        holder.binding.getRoot().getLayoutParams().height = ResUtil.dp2px(mCount == 5 ? 200 : 192);
        return holder;
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Vod item = (Vod) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.name.setText(item.getVodName());
        holder.binding.remark.setText(item.getVodRemarks());
        holder.binding.remark.setVisibility(item.getRemarkVisible());
        Utils.loadImage(item.getVodPic(), holder.binding.image);
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