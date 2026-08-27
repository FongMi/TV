package com.fongmi.android.tv.player.exo;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.libass.LibassPlaybackSession;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.preload.MediaSourceFactorySupplier;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.ts.TsExtractor;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.setting.PreloadSetting;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;

import java.io.File;
import java.util.Map;

public class ExoMediaSourceFactory implements MediaSource.Factory {

    private static final int CACHE_SPACE_PERCENT = 80;

    private static StandaloneDatabaseProvider databaseProvider;
    private static Cache cache;

    private final DefaultMediaSourceFactory defaultMediaSourceFactory;
    private final LibassPlaybackSession libassPlaybackSession;

    private HttpDataSource.Factory httpDataSourceFactory;
    private DataSource.Factory dataSourceFactory;

    private ExoMediaSourceFactory(LibassPlaybackSession libassPlaybackSession) {
        this.libassPlaybackSession = libassPlaybackSession;
        this.defaultMediaSourceFactory = new DefaultMediaSourceFactory(getDataSourceFactory(), createDefaultExtractorsFactory());
    }

    static MediaSourceFactorySupplier supplier(LibassPlaybackSession libassPlaybackSession) {
        return new MediaSourceFactorySupplier() {
            @NonNull
            @Override
            public MediaSourceFactorySupplier setCache(Cache cache) {
                return this;
            }

            @NonNull
            @Override
            public MediaSourceFactorySupplier setDataSourceFactory(DataSource.Factory dataSourceFactory) {
                return this;
            }

            @Override
            public MediaSource.Factory get() {
                return new ExoMediaSourceFactory(libassPlaybackSession);
            }
        };
    }

    static DataSource.Factory createUpstreamDataSourceFactory(Map<String, String> headers) {
        HttpDataSource.Factory factory = new OkHttpDataSource.Factory(OkHttp.player());
        factory.setDefaultRequestProperties(headers);
        return new DefaultDataSource.Factory(App.get(), factory);
    }

    static synchronized Cache getCache() {
        if (cache != null) return cache;
        File dir = Path.exoCache();
        return cache = new SimpleCache(dir, new LeastRecentlyUsedCacheEvictor(getMaxCacheSize(dir)), getDatabaseProvider());
    }

    private static StandaloneDatabaseProvider getDatabaseProvider() {
        if (databaseProvider == null) databaseProvider = new StandaloneDatabaseProvider(App.get());
        return databaseProvider;
    }

    private static long getMaxCacheSize(File dir) {
        long usedBytes = Path.size(dir);
        long availableBytes = Math.max(0, Path.available(dir));
        long storageBudget = (usedBytes + availableBytes) * CACHE_SPACE_PERCENT / 100;
        return Math.min(PreloadSetting.getSizeBytes(), storageBudget);
    }

    @NonNull
    @Override
    public MediaSource.Factory setDrmSessionManagerProvider(@NonNull DrmSessionManagerProvider drmSessionManagerProvider) {
        return this;
    }

    @NonNull
    @Override
    public MediaSource.Factory setLoadErrorHandlingPolicy(@NonNull LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
        return this;
    }

    @NonNull
    @Override
    public @C.ContentType int[] getSupportedTypes() {
        return defaultMediaSourceFactory.getSupportedTypes();
    }

    @NonNull
    @Override
    public MediaSource createMediaSource(@NonNull MediaItem mediaItem) {
        getHttpDataSourceFactory().setDefaultRequestProperties(ExoUtil.extractHeaders(mediaItem));
        if (!libassPlaybackSession.isAvailable()) return defaultMediaSourceFactory.createMediaSource(mediaItem);
        LibassPlaybackSession.MediaComponents components = libassPlaybackSession.createMediaComponents(mediaItem, createDefaultExtractorsFactory());
        return new DefaultMediaSourceFactory(getDataSourceFactory(), components.extractorsFactory).setSubtitleParserFactory(components.subtitleParserFactory).createMediaSource(mediaItem);
    }

    private static ExtractorsFactory createDefaultExtractorsFactory() {
        return new DefaultExtractorsFactory().setTsExtractorTimestampSearchBytes(TsExtractor.DEFAULT_TIMESTAMP_SEARCH_BYTES * 10);
    }

    private DataSource.Factory getDataSourceFactory() {
        if (dataSourceFactory == null) dataSourceFactory = () -> getCacheDataSource(new DefaultDataSource.Factory(App.get(), getHttpDataSourceFactory())).createDataSource();
        return dataSourceFactory;
    }

    private CacheDataSource.Factory getCacheDataSource(DataSource.Factory upstreamFactory) {
        return new CacheDataSource.Factory().setCache(getCache()).setUpstreamDataSourceFactory(upstreamFactory).setCacheWriteDataSinkFactory(null).setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    private HttpDataSource.Factory getHttpDataSourceFactory() {
        if (httpDataSourceFactory == null) httpDataSourceFactory = new OkHttpDataSource.Factory(OkHttp.player());
        return httpDataSourceFactory;
    }
}
