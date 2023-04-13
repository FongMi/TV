package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

public class HistoryPresenter extends Presenter {

    private final OnClickListener mListener;
    private int width, height;
    private boolean delete;

    public HistoryPresenter(OnClickListener listener) {
        this.mListener = listener;
        setLayoutSize();
    }

    public interface OnClickListener {

        void onItemClick(History item);

        void onItemDelete(History item);

        boolean onLongClick();
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    private void setLayoutSize() {
        int space = ResUtil.dp2px(48) + ResUtil.dp2px(16 * (Product.getColumn() - 1));
        int base = ResUtil.getScreenWidth() - space;
        width = base / Product.getColumn();
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
        History item = (History) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        setClickListener(holder.view, item);
        holder.binding.name.setText(item.getVodName());
        holder.binding.site.setText(item.getSiteName());
        holder.binding.site.setVisibility(item.getSiteVisible());
        holder.binding.remark.setVisibility(delete ? View.GONE : View.VISIBLE);
        holder.binding.delete.setVisibility(!delete ? View.GONE : View.VISIBLE);
        holder.binding.remark.setText(ResUtil.getString(R.string.vod_last, item.getVodRemarks()));
        ImgUtil.loadHistory(item.getVodPic(), holder.binding.image);
    }

    private void setClickListener(View root, History item) {
        root.setOnLongClickListener(view -> mListener.onLongClick());
        root.setOnClickListener(view -> {
            if (isDelete()) mListener.onItemDelete(item);
            else mListener.onItemClick(item);
        });
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