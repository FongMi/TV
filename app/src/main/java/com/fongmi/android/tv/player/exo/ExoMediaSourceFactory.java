package com.fongmi.android.tv.player.exo;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.LocalClearKeyLicense;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.ProgressiveIsoCache;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider;
import androidx.media3.exoplayer.libass.LibassPlaybackSession;
import androidx.media3.exoplayer.source.ConcatenatingMediaSource2;
import androidx.media3.exoplayer.source.ContentTypeResolvingMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.preload.MediaSourceFactorySupplier;
import androidx.media3.exoplayer.text.SubtitleTranscript;
import androidx.media3.exoplayer.text.SubtitleTranscriptParserFactory;
import androidx.media3.exoplayer.text.SubtitleTranscriptSession;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.mkv.MatroskaExtractor;
import androidx.media3.extractor.text.SubtitleParser;
import androidx.media3.extractor.ts.TsExtractor;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.media.ConcatenatingUrl;
import com.fongmi.android.tv.setting.PreloadSetting;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Auth;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.UriUtil;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ExoMediaSourceFactory implements MediaSource.Factory {

    private static final int CACHE_SPACE_PERCENT = 80;

    private static StandaloneDatabaseProvider databaseProvider;
    private static Cache cache;

    private final DefaultMediaSourceFactory defaultMediaSourceFactory;
    private final LibassPlaybackSession libassPlaybackSession;
    private final SubtitleTranscriptSession subtitleTranscriptSession;

    private ExoMediaSourceFactory(LibassPlaybackSession libassPlaybackSession, SubtitleTranscriptSession subtitleTranscriptSession) {
        this.libassPlaybackSession = libassPlaybackSession;
        this.subtitleTranscriptSession = subtitleTranscriptSession;
        this.defaultMediaSourceFactory = new DefaultMediaSourceFactory(App.get(), createDefaultExtractorsFactory());
    }

    static MediaSourceFactorySupplier supplier(LibassPlaybackSession libassPlaybackSession, SubtitleTranscriptSession subtitleTranscriptSession) {
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
                return new ExoMediaSourceFactory(libassPlaybackSession, subtitleTranscriptSession);
            }
        };
    }

    static DataSource.Factory createUpstreamDataSourceFactory(Map<String, String> headers, Auth.Context auth) {
        HttpDataSource.Factory factory = new OkHttpDataSource.Factory(OkHttp.player(auth));
        factory.setDefaultRequestProperties(headers == null ? Map.of() : headers);
        return new DefaultDataSource.Factory(App.get(), factory);
    }

    public static DataSource.Factory createDataSourceFactory(Map<String, String> headers) {
        return createDataSourceFactory(headers, null);
    }

    public static DataSource.Factory createDataSourceFactory(Map<String, String> headers, Auth.Context auth) {
        Cache sharedCache = getCache();
        CacheDataSource.Factory factory = new CacheDataSource.Factory().setCache(sharedCache).setUpstreamDataSourceFactory(createUpstreamDataSourceFactory(headers, auth)).setCacheWriteDataSinkFactory(null).setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
        return ProgressiveIsoCache.withDiskCache(factory, sharedCache);
    }

    public static synchronized Cache getCache() {
        if (cache != null) return cache;
        File dir = Path.exoCache();
        return cache = new SimpleCache(dir, new LeastRecentlyUsedCacheEvictor(getMaxCacheSize(dir)), getDatabaseProvider());
    }

    private static StandaloneDatabaseProvider getDatabaseProvider() {
        if (databaseProvider == null) databaseProvider = new StandaloneDatabaseProvider(App.get());
        return databaseProvider;
    }

    private static long getMaxCacheSize(File dir) {
        long availableBytes = Math.max(0, Path.available(dir));
        long maxCacheBytes = PreloadSetting.getSizeBytes();
        if (availableBytes >= maxCacheBytes * 100 / CACHE_SPACE_PERCENT) return maxCacheBytes;
        long usedBytes = Path.size(dir);
        long storageBudget = (usedBytes + availableBytes) * CACHE_SPACE_PERCENT / 100;
        return Math.min(maxCacheBytes, storageBudget);
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
        List<ConcatenatingUrl.Segment> segments = ConcatenatingUrl.parse(mediaItem.localConfiguration.uri.toString());
        MediaSource.Factory factory = createMediaSourceFactory(mediaItem);
        if (segments.isEmpty()) return factory.createMediaSource(mediaItem);
        ConcatenatingMediaSource2.Builder builder = new ConcatenatingMediaSource2.Builder().setMediaItem(mediaItem);
        for (ConcatenatingUrl.Segment segment : segments) builder.add(factory.createMediaSource(mediaItem.buildUpon().setUri(UriUtil.uri(segment.url())).build()), segment.durationMs());
        return builder.build();
    }

    private MediaSource.Factory createMediaSourceFactory(MediaItem mediaItem) {
        SubtitleTranscript transcript = subtitleTranscriptSession.forMediaItem(mediaItem);
        ExtractorsFactory extractorsFactory = createDefaultExtractorsFactory().setMp4ClearKeys(
                LocalClearKeyLicense.parse(mediaItem.localConfiguration.drmConfiguration));
        SubtitleParser.Factory subtitleParserFactory;
        if (libassPlaybackSession.isAvailable()) {
            LibassPlaybackSession.MediaComponents components = libassPlaybackSession.createMediaComponents(mediaItem, extractorsFactory, transcript);
            extractorsFactory = components.extractorsFactory;
            subtitleParserFactory = components.subtitleParserFactory;
        } else subtitleParserFactory = new SubtitleTranscriptParserFactory(transcript);
        DataSource.Factory dataSourceFactory = createDataSourceFactory(ExoUtil.extractHeaders(mediaItem), ExoUtil.extractAuth(mediaItem));
        MediaSource.Factory factory = new DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory).setSubtitleParserFactory(subtitleParserFactory);
        return new ContentTypeResolvingMediaSource.Factory(factory, dataSourceFactory);
    }

    static DefaultExtractorsFactory createDefaultExtractorsFactory() {
        return new DefaultExtractorsFactory().setTsExtractorTimestampSearchBytes(TsExtractor.DEFAULT_TIMESTAMP_SEARCH_BYTES * 10).setMatroskaExtractorFlagsForHttpSources(MatroskaExtractor.FLAG_DEFER_SEEK_FOR_CUES);
    }
}
