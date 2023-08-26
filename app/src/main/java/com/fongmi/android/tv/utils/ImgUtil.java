package com.fongmi.android.tv.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
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
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.github.catvod.utils.Json;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public class ImgUtil {

    public static void load(String url, ImageView view) {
        load(url, view, ImageView.ScaleType.CENTER);
    }

    public static void load(String url, int error, CustomTarget<Drawable> target) {
        if (TextUtils.isEmpty(url)) target.onLoadFailed(ResUtil.getDrawable(error));
        else Glide.with(App.get()).load(getUrl(url)).error(error).dontAnimate().into(target);
    }

    public static void load(String url, ImageView view, CustomTarget<Bitmap> target) {
        if (TextUtils.isEmpty(url)) view.setImageResource(R.drawable.ic_img_error);
        else Glide.with(App.get()).asBitmap().load(getUrl(url)).error(R.drawable.ic_img_error).placeholder(R.drawable.ic_img_loading).skipMemoryCache(true).dontAnimate().signature(new ObjectKey(url + "_" + Setting.getQuality())).into(target);
    }

    public static void load(String url, ImageView view, ImageView.ScaleType scaleType) {
        view.setScaleType(scaleType);
        if (TextUtils.isEmpty(url)) view.setImageResource(R.drawable.ic_img_error);
        else Glide.with(App.get()).asBitmap().load(getUrl(url)).placeholder(R.drawable.ic_img_loading).skipMemoryCache(true).dontAnimate().sizeMultiplier(Setting.getThumbnail()).signature(new ObjectKey(url + "_" + Setting.getQuality())).listener(getListener(view, scaleType)).into(view);
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
        else Glide.with(App.get()).asBitmap().load(url).error(R.drawable.ic_img_empty).skipMemoryCache(true).dontAnimate().signature(new ObjectKey(url)).into(view);
    }

    public static Object getUrl(String url) {
        String param = null;
        url = Utils.convert(url);
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
        Map<String, String> map = Json.toMap(JsonParser.parseString(header));
        for (Map.Entry<String, String> entry : map.entrySet()) builder.addHeader(replace(entry.getKey()), entry.getValue());
    }

    private static String replace(String key) {
        if (key.equals("user-agent")) return HttpHeaders.USER_AGENT;
        if (key.equals("referer")) return HttpHeaders.REFERER;
        if (key.equals("cookie")) return HttpHeaders.COOKIE;
        return key;
    }

    public static byte[] resize(byte[] bytes) {
        Bitmap bitmap = crop(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    private static Bitmap crop(Bitmap source) {
        int newWidth = ResUtil.getScreenWidth();
        int newHeight = ResUtil.getScreenHeight();
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        float xScale = (float) newWidth / sourceWidth;
        float yScale = (float) newHeight / sourceHeight;
        float scale = Math.max(xScale, yScale);
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;
        float left = (newWidth - scaledWidth) / 2;
        float top = (newHeight - scaledHeight) / 2;
        RectF rectF = new RectF(left, top, left + scaledWidth, top + scaledHeight);
        Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(source, null, rectF, null);
        return dest;
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
