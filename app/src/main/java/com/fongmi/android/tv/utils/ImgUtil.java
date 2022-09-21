package com.fongmi.android.tv.utils;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;

public class ImgUtil {

    public static void load(String vodPic, ImageView view) {
        if (TextUtils.isEmpty(vodPic)) view.setImageResource(R.drawable.ic_img_error);
        else Glide.with(App.get()).asBitmap().load(vodPic).skipMemoryCache(true).sizeMultiplier(Prefers.getThumbnail()).signature(new ObjectKey(vodPic + "_" + Prefers.getQuality())).placeholder(R.drawable.ic_img_loading).listener(getListener(view)).into(view);
    }

    private static RequestListener<Bitmap> getListener(ImageView view) {
        return new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                view.setScaleType(ImageView.ScaleType.CENTER);
                view.setImageResource(R.drawable.ic_img_error);
                return true;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                return false;
            }
        };
    }
}
