package com.fongmi.android.tv.utils;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;

public class ImgUtil {

    public static void load(String url, ImageView view) {
        load(url, view, ImageView.ScaleType.CENTER);
    }

    public static void load(String url, ImageView view, ImageView.ScaleType scaleType) {
        view.setScaleType(scaleType);
        if (TextUtils.isEmpty(url)) view.setImageResource(R.drawable.ic_img_error);
        else Glide.with(App.get()).asBitmap().load(getUrl(url)).skipMemoryCache(true).dontAnimate().sizeMultiplier(Prefers.getThumbnail()).signature(new ObjectKey(url + "_" + Prefers.getQuality())).placeholder(R.drawable.ic_img_loading).listener(getListener(view, scaleType)).into(view);
    }

    public static void loadKeep(String url, ImageView view) {
        view.setScaleType(ImageView.ScaleType.CENTER);
        if (TextUtils.isEmpty(url)) view.setImageResource(R.drawable.ic_img_error);
        else Glide.with(App.get()).asBitmap().load(getUrl(url)).error(R.drawable.ic_img_error).placeholder(R.drawable.ic_img_loading).listener(getListener(view)).into(view);
    }

    public static void loadHistory(String url, ImageView view) {
        view.setScaleType(ImageView.ScaleType.CENTER);
        if (TextUtils.isEmpty(url)) view.setImageResource(R.drawable.ic_img_error);
        else Glide.with(App.get()).asBitmap().load(getUrl(url)).error(R.drawable.ic_img_error).placeholder(R.drawable.ic_img_loading).listener(getListener(view)).into(view);
    }

    public static void loadLive(String url, ImageView view) {
        view.setVisibility(TextUtils.isEmpty(url) ? View.GONE : View.VISIBLE);
        if (TextUtils.isEmpty(url)) view.setImageResource(R.drawable.ic_img_empty);
        else Glide.with(App.get()).asBitmap().load(url).skipMemoryCache(true).dontAnimate().signature(new ObjectKey(url)).error(R.drawable.ic_img_empty).into(view);
    }

    public static Object getUrl(String url) {
        String param = null;
        url = Utils.checkProxy(url);
        if (url.startsWith("data:")) return url;
        LazyHeaders.Builder builder = new LazyHeaders.Builder();
        if (url.contains("@Cookie=")) builder.addHeader("Cookie", param = url.split("@Cookie=")[1].split("@")[0]);
        if (url.contains("@Referer=")) builder.addHeader("Referer", param = url.split("@Referer=")[1].split("@")[0]);
        if (url.contains("@User-Agent=")) builder.addHeader("User-Agent", param = url.split("@User-Agent=")[1].split("@")[0]);
        return new GlideUrl(param == null ? url : url.split("@")[0], builder.build());
    }

    private static RequestListener<Bitmap> getListener(ImageView view) {
        return getListener(view, ImageView.ScaleType.CENTER);
    }

    private static RequestListener<Bitmap> getListener(ImageView view, ImageView.ScaleType scaleType) {
        return new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                view.setScaleType(scaleType);
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
