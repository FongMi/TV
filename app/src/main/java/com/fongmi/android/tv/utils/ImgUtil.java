package com.fongmi.android.tv.utils;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.github.catvod.utils.Json;
import com.google.common.net.HttpHeaders;

import java.util.Map;

import jahirfiquitiva.libs.textdrawable.TextDrawable;

public class ImgUtil {

    private static ObjectKey getSignature(String url) {
        return new ObjectKey(url + "_" + Setting.getQuality());
    }

    public static void load(String url, int error, CustomTarget<Drawable> target) {
        if (TextUtils.isEmpty(url)) target.onLoadFailed(ResUtil.getDrawable(error));
        else Glide.with(App.get()).asDrawable().load(getUrl(url)).error(error).skipMemoryCache(true).dontAnimate().signature(getSignature(url)).into(target);
    }

    public static void rect(String text, String url, ImageView view) {
        load(text, url, view, ImageView.ScaleType.CENTER, true);
    }

    public static void oval(String text, String url, ImageView view) {
        load(text, url, view, ImageView.ScaleType.CENTER, false);
    }

    public static void load(String text, String url, ImageView view, ImageView.ScaleType scaleType, boolean rect) {
        view.setScaleType(scaleType);
        if (!TextUtils.isEmpty(url)) Glide.with(App.get()).asBitmap().load(getUrl(url)).placeholder(R.drawable.ic_img_loading).skipMemoryCache(true).dontAnimate().sizeMultiplier(Setting.getThumbnail()).signature(getSignature(url)).listener(getListener(view, scaleType)).into(view);
        else if (!text.isEmpty()) view.setImageDrawable(getTextDrawable(text.substring(0, 1), rect));
        else view.setImageResource(R.drawable.ic_img_error);
    }

    public static void loadVod(String text, String url, ImageView view) {
        view.setScaleType(ImageView.ScaleType.CENTER);
        if (!TextUtils.isEmpty(url)) Glide.with(App.get()).asBitmap().load(getUrl(url)).placeholder(R.drawable.ic_img_loading).listener(getListener(view)).into(view);
        else if (!text.isEmpty()) view.setImageDrawable(getTextDrawable(text.substring(0, 1), true));
        else view.setImageResource(R.drawable.ic_img_error);
    }

    public static void loadLive(String url, ImageView view) {
        view.setVisibility(TextUtils.isEmpty(url) ? View.GONE : View.VISIBLE);
        if (TextUtils.isEmpty(url)) view.setImageResource(R.drawable.ic_img_empty);
        else Glide.with(App.get()).asBitmap().load(getUrl(url)).error(R.drawable.ic_img_empty).skipMemoryCache(true).dontAnimate().signature(getSignature(url)).into(view);
    }

    private static Drawable getTextDrawable(String text, boolean rect) {
        TextDrawable.Builder builder = new TextDrawable.Builder().withBorder(ResUtil.dp2px(2), ColorGenerator.get700(text));
        if (rect) return builder.buildRoundRect(text, ColorGenerator.get400(text), ResUtil.dp2px(8));
        return builder.buildRound(text, ColorGenerator.get400(text));
    }

    public static Object getUrl(String url) {
        String param = null;
        url = UrlUtil.convert(url);
        if (url.startsWith("data:")) return url;
        LazyHeaders.Builder builder = new LazyHeaders.Builder();
        if (url.contains("@Headers=")) addHeader(builder, param = url.split("@Headers=")[1].split("@")[0]);
        if (url.contains("@Cookie=")) builder.addHeader(HttpHeaders.COOKIE, param = url.split("@Cookie=")[1].split("@")[0]);
        if (url.contains("@Referer=")) builder.addHeader(HttpHeaders.REFERER, param = url.split("@Referer=")[1].split("@")[0]);
        if (url.contains("@User-Agent=")) builder.addHeader(HttpHeaders.USER_AGENT, param = url.split("@User-Agent=")[1].split("@")[0]);
        url = param == null ? url : url.split("@")[0];
        return TextUtils.isEmpty(url) ? null : new GlideUrl(url, builder.build());
    }

    private static void addHeader(LazyHeaders.Builder builder, String header) {
        Map<String, String> map = Json.toMap(Json.parse(header));
        for (Map.Entry<String, String> entry : map.entrySet()) builder.addHeader(UrlUtil.fixHeader(entry.getKey()), entry.getValue());
    }

    private static RequestListener<Bitmap> getListener(ImageView view) {
        return getListener(view, ImageView.ScaleType.CENTER);
    }

    private static RequestListener<Bitmap> getListener(ImageView view, ImageView.ScaleType scaleType) {
        return new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Bitmap> target, boolean isFirstResource) {
                view.setImageResource(R.drawable.ic_img_error);
                view.setScaleType(scaleType);
                return true;
            }

            @Override
            public boolean onResourceReady(@NonNull Bitmap resource, @NonNull Object model, Target<Bitmap> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                return false;
            }
        };
    }
}
