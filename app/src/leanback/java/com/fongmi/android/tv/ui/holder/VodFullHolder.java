package com.fongmi.android.tv.ui.holder;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterVodFullBinding;
import com.fongmi.android.tv.ui.base.BaseVodHolder;
import com.fongmi.android.tv.utils.ImgUtil;

public class VodFullHolder extends BaseVodHolder {

    private final AdapterVodFullBinding binding;

    public VodFullHolder(@NonNull AdapterVodFullBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public VodFullHolder size(int[] size) {
        binding.getRoot().getLayoutParams().width = size[0];
        binding.getRoot().getLayoutParams().height = size[1];
        return this;
    }

    @Override
    public void initView(Vod item) {
        ImgUtil.load(item.getVodPic(), binding.image);
    }
}
