package com.fongmi.android.tv.ui.holder;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.fongmi.android.tv.App;
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
        Glide.with(App.get()).load(ImgUtil.getUrl(item.getVodPic())).dontAnimate().listener(getListener(binding.image)).into(binding.image);
    }

    private RequestListener<Drawable> getListener(ImageView view) {
        return new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                view.setScaleType(ImageView.ScaleType.CENTER);
                view.setImageResource(R.drawable.ic_img_error);
                return true;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                return false;
            }
        };
    }
}
