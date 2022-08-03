package com.fongmi.android.tv.utils;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;

public class ImgUtil {

    public static void load(String vodName, String vodPic, ImageView view) {
        if (TextUtils.isEmpty(vodPic)) {
            String text = vodName.isEmpty() ? "" : vodName.substring(0, 1);
            view.setImageDrawable(TextDrawable.builder().buildRect(text, ColorGenerator.MATERIAL.getColor(text)));
        } else {
            load(vodPic, view);
        }
    }

    public static void load(String url, ImageView view) {
        float thumbnail = 1 - Prefers.getThumbnail() * 0.3f;
        Glide.with(App.get()).load(url).thumbnail(thumbnail).signature(new ObjectKey(url + "_" + thumbnail)).placeholder(R.drawable.ic_img_loading).error(R.drawable.ic_img_error).listener(getListener(view)).into(view);
    }

    private static RequestListener<Drawable> getListener(ImageView view) {
        return new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                view.setScaleType(ImageView.ScaleType.CENTER);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                return false;
            }
        };
    }
}
