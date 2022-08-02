package com.fongmi.android.tv.utils;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.signature.ObjectKey;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;

public class ImgUtil {

    public static void load(String vodName, String vodPic, ImageView view) {
        if (TextUtils.isEmpty(vodPic)) {
            String text = vodName.isEmpty() ? "" : vodName.substring(0, 1);
            view.setImageDrawable(TextDrawable.builder().buildRect(text, ColorGenerator.MATERIAL.getColor(text)));
        } else {
            ImgUtil.load(vodPic, view);
        }
    }

    public static void load(String url, ImageView view) {
        float thumbnail = 1 - Prefers.getThumbnail() * 0.3f;
        Glide.with(App.get()).load(url).thumbnail(thumbnail).signature(new ObjectKey(url + "_" + thumbnail)).placeholder(R.drawable.ic_img_loading).into(customTarget(view));
    }

    private static CustomTarget<Drawable> customTarget(ImageView view) {
        return new CustomTarget<>() {
            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                view.setScaleType(ImageView.ScaleType.CENTER);
                view.setImageResource(R.drawable.ic_img_error);
            }

            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                view.setImageDrawable(resource);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                view.setScaleType(ImageView.ScaleType.CENTER);
                view.setImageDrawable(null);
            }
        };
    }
}
