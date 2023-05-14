package com.fongmi.android.tv.ui.holder;

import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterVodFolderBinding;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.ImgUtil;

public class VodFolderHolder extends Presenter.ViewHolder {

    private final VodPresenter.OnClickListener listener;
    public final AdapterVodFolderBinding binding;

    public VodFolderHolder(@NonNull AdapterVodFolderBinding binding, VodPresenter.OnClickListener listener) {
        super(binding.getRoot());
        this.binding = binding;
        this.listener = listener;
    }

    public void initView(Vod item) {
        binding.name.setText(item.getVodName());
        binding.remark.setText(item.getVodRemarks());
        binding.remark.setVisibility(item.getRemarkVisible());
        binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        binding.getRoot().setOnLongClickListener(v -> listener.onLongClick(item));
        ImgUtil.load(item.getVodPic(), binding.image, ImageView.ScaleType.FIT_CENTER);
    }
}
