package com.fongmi.android.tv.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.Excludes;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.avif.AvifByteBufferBitmapDecoder;
import com.bumptech.glide.integration.avif.AvifGlideModule;
import com.bumptech.glide.integration.avif.AvifStreamBitmapDecoder;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.github.catvod.net.OkHttp;

import java.io.InputStream;
import java.nio.ByteBuffer;

@GlideModule
@Excludes(AvifGlideModule.class)
public class OkGlideModule extends AppGlideModule {

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        builder.setLogLevel(Log.ERROR);
    }

    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, Registry registry) {
        AvifByteBufferBitmapDecoder byteBufferBitmapDecoder = new AvifByteBufferBitmapDecoder(glide.getBitmapPool());
        AvifStreamBitmapDecoder streamBitmapDecoder = new AvifStreamBitmapDecoder(registry.getImageHeaderParsers(), byteBufferBitmapDecoder, glide.getArrayPool());
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(OkHttp.client()));
        registry.append(ByteBuffer.class, Bitmap.class, byteBufferBitmapDecoder);
        registry.append(InputStream.class, Bitmap.class, streamBitmapDecoder);
    }
}