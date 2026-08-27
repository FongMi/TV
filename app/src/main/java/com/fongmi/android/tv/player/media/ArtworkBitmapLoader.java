package com.fongmi.android.tv.player.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.media3.common.util.BitmapLoader;
import androidx.media3.datasource.DataSourceBitmapLoader;
import androidx.media3.session.MediaSession;

import com.bumptech.glide.load.model.GlideUrl;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.net.OkHttp;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public final class ArtworkBitmapLoader implements BitmapLoader {

    private static final long MAX_ARTWORK_BYTES = 16L * 1024 * 1024;

    private final DataSourceBitmapLoader decoder;

    public ArtworkBitmapLoader(Context context) {
        decoder = new DataSourceBitmapLoader.Builder(context).setMaximumOutputDimension(MediaSession.getBitmapDimensionLimit(context) * 2 - 1).build();
    }

    @Override
    public boolean supportsMimeType(@NonNull String mimeType) {
        return decoder.supportsMimeType(mimeType);
    }

    @NonNull
    @Override
    public ListenableFuture<Bitmap> decodeBitmap(@NonNull byte[] data) {
        return decoder.decodeBitmap(data);
    }

    @NonNull
    @Override
    public ListenableFuture<Bitmap> loadBitmap(@NonNull Uri uri) {
        try {
            Object model = ImgUtil.getUrl(uri.toString());
            if (!(model instanceof GlideUrl url)) return decoder.loadBitmap(uri);
            String scheme = UrlUtil.scheme(url.toStringUrl());
            if (!"http".equals(scheme) && !"https".equals(scheme)) return decoder.loadBitmap(uri);
            return Futures.transformAsync(loadBytes(url), data -> decoder.decodeBitmap(Objects.requireNonNull(data)), MoreExecutors.directExecutor());
        } catch (RuntimeException error) {
            return Futures.immediateFailedFuture(error);
        }
    }

    private static ListenableFuture<byte[]> loadBytes(@NonNull GlideUrl url) {
        Call call = OkHttp.newCall(url.toStringUrl(), url.getHeaders());
        SettableFuture<byte[]> future = SettableFuture.create();
        future.addListener(() -> {
            if (future.isCancelled()) call.cancel();
        }, MoreExecutors.directExecutor());
        call.enqueue(new ArtworkCallback(future));
        return future;
    }

    private static byte[] readBytes(@NonNull Response response) throws IOException {
        if (!response.isSuccessful()) throw new IOException("HTTP " + response.code() + " " + response.message());
        if (response.body().contentLength() > MAX_ARTWORK_BYTES) throw new IOException("Artwork size limit exceeded");
        byte[] data = ByteStreams.toByteArray(ByteStreams.limit(response.body().byteStream(), MAX_ARTWORK_BYTES + 1));
        if (data.length == 0) throw new IOException("Empty artwork");
        if (data.length > MAX_ARTWORK_BYTES) throw new IOException("Artwork size limit exceeded");
        return data;
    }

    private record ArtworkCallback(SettableFuture<byte[]> future) implements Callback {

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException error) {
            future.setException(error);
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) {
            try (response) {
                if (!future.isCancelled()) future.set(readBytes(response));
            } catch (IOException error) {
                future.setException(error);
            }
        }
    }
}
