package com.fongmi.android.tv.utils;

import android.graphics.drawable.Drawable;
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

    private static int getWidth() {
        float thumbnail = 1 - Prefers.getThumbnail() * 0.3f;
        return ResUtil.dp2px((int) Math.ceil(300 * thumbnail));
    }

    private static int getHeight() {
        float thumbnail = 1 - Prefers.getThumbnail() * 0.3f;
        return ResUtil.dp2px((int) Math.ceil(400 * thumbnail));
    }

    public static void load(String url, ImageView view) {
        Glide.with(App.get()).load(url).error(R.drawable.ic_img_error).placeholder(R.drawable.ic_img_loading).into(view);
    }

    public static void load(String vodName, String vodPic, ImageView view) {
        Glide.with(App.get()).load(vodPic).override(getWidth(), getHeight()).signature(new ObjectKey(vodPic + "_" + Prefers.getThumbnail())).placeholder(R.drawable.ic_img_loading).listener(getListener(vodName, view)).into(view);
    }

    private static RequestListener<Drawable> getListener(String vodName, ImageView view) {
        return new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                ImgUtil.onLoadFailed(vodName, view);
                return true;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                return false;
            }
        };
    }

    private static void onLoadFailed(String vodName, ImageView view) {
        String text = vodName.isEmpty() ? "" : vodName.substring(0, 1);
        if (text.isEmpty()) {
            view.setImageResource(R.drawable.ic_img_error);
            view.setScaleType(ImageView.ScaleType.CENTER);
        } else {
            view.setImageDrawable(TextDrawable.builder().buildRect(text, ColorGenerator.MATERIAL.getColor(text)));
            view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
    }
}
