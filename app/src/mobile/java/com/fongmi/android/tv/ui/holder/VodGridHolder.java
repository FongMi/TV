package com.fongmi.android.tv.ui.holder;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterVodGridBinding;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.utils.ImgUtil;

public class VodGridHolder extends RecyclerView.ViewHolder {

    private final VodAdapter.OnClickListener listener;
    private final AdapterVodGridBinding binding;

    public VodGridHolder(@NonNull AdapterVodGridBinding binding, VodAdapter.OnClickListener listener) {
        super(binding.getRoot());
        this.binding = binding;
        this.listener = listener;
    }

    public void initView(Vod item) {
        binding.name.setText(item.getVodName());
        binding.year.setText(item.getVodYear());
        binding.site.setText(item.getSiteName());
        binding.remark.setText(item.getVodRemarks());
        binding.site.setVisibility(item.getSiteVisible());
        binding.year.setVisibility(item.getYearVisible());
        binding.remark.setVisibility(item.getRemarkVisible());
        binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        binding.getRoot().setOnLongClickListener(v -> listener.onLongClick(item));
        ImgUtil.load(item.getVodPic(), binding.image);
    }
}
