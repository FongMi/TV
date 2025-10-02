package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterSearchBinding;
import com.fongmi.android.tv.utils.ImgUtil;

public class SearchAdapter extends BaseDiffAdapter<Vod, SearchAdapter.ViewHolder> {

    private final OnClickListener listener;

    public SearchAdapter(OnClickListener listener) {
        this.listener = listener;
    }

    public interface OnClickListener {

        void onItemClick(Vod item);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterSearchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vod item = getItem(position);
        holder.binding.name.setText(item.getVodName());
        holder.binding.site.setText(item.getSiteName());
        holder.binding.remark.setText(item.getVodRemarks());
        holder.binding.site.setVisibility(item.getSiteVisible());
        holder.binding.remark.setVisibility(item.getRemarkVisible());
        holder.binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        ImgUtil.load(item.getVodName(), item.getVodPic(), holder.binding.image);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterSearchBinding binding;

        ViewHolder(@NonNull AdapterSearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
