package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.utils.ImgUtil;

public class HistoryAdapter extends BaseDiffAdapter<History, HistoryAdapter.ViewHolder> {

    private final OnClickListener listener;
    private int width, height;
    private boolean delete;

    public HistoryAdapter(OnClickListener listener) {
        this.listener = listener;
    }

    public interface OnClickListener {

        void onItemClick(History item);

        void onItemDelete(History item);

        boolean onLongClick();
    }

    public void setSize(int[] size) {
        this.width = size[0];
        this.height = size[1];
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
        notifyItemRangeChanged(0, getItemCount());
    }

    @Override
    public void clear() {
        super.clear();
        setDelete(false);
        History.delete(VodConfig.getCid());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolder holder = new ViewHolder(AdapterVodBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        holder.binding.getRoot().getLayoutParams().width = width;
        holder.binding.image.getLayoutParams().height = height;
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        History item = getItem(position);
        holder.binding.name.setText(item.getVodName());
        holder.binding.site.setText(item.getSiteName());
        holder.binding.remark.setText(item.getVodRemarks());
        holder.binding.site.setVisibility(item.getSiteVisible());
        holder.binding.progress.setMax((int) item.getDuration());
        holder.binding.progress.setProgress((int) item.getPosition(), true);
        holder.binding.remark.setVisibility(delete ? View.GONE : View.VISIBLE);
        holder.binding.delete.setVisibility(!delete ? View.GONE : View.VISIBLE);
        holder.binding.progress.setVisibility(delete ? View.INVISIBLE : View.VISIBLE);
        ImgUtil.load(item.getVodName(), item.getVodPic(), holder.binding.image);
        setClickListener(holder.binding.getRoot(), item);
    }

    private void setClickListener(View root, History item) {
        root.setOnLongClickListener(view -> listener.onLongClick());
        root.setOnClickListener(view -> {
            if (isDelete()) listener.onItemDelete(item);
            else listener.onItemClick(item);
        });
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterVodBinding binding;

        ViewHolder(@NonNull AdapterVodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
