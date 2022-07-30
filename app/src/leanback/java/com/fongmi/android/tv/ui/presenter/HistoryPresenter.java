package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

public class HistoryPresenter extends Presenter {

    private OnClickListener mListener;
    private int width, height;

    public HistoryPresenter(int columns) {
        setLayoutSize(columns);
    }

    public interface OnClickListener {
        void onItemClick(History item);
    }

    public void setOnClickListener(OnClickListener listener) {
        this.mListener = listener;
    }

    private void setLayoutSize(int columns) {
        int space = ResUtil.dp2px(16) * (columns - 1) + ResUtil.dp2px(48);
        int base = ResUtil.getScreenWidthPx() - space;
        width = (int) base / columns;
        height = (int) (width / 0.75);
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
        History item = (History) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.name.setText(item.getVodName());
        holder.binding.site.setVisibility(View.VISIBLE);
        holder.binding.site.setText(ApiConfig.get().getSite(item.getSiteKey()).getName());
        holder.binding.remark.setText(ResUtil.getString(R.string.vod_last, item.getVodRemarks()));
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