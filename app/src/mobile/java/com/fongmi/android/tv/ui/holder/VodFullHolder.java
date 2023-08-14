package com.fongmi.android.tv.ui.holder;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fongmi.android.tv.R;
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

    @Override
    public void initView(Vod item) {
        ImgUtil.load(item.getVodPic(), binding.image, new CustomTarget<>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                binding.image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                binding.image.setImageBitmap(resource);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                binding.image.setScaleType(ImageView.ScaleType.CENTER);
                binding.image.setImageResource(R.drawable.ic_img_error);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        });
    }
}
