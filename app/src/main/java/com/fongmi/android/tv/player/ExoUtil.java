package com.fongmi.android.tv.player;

import android.graphics.Color;
import android.net.Uri;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.utils.FileUtil;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.database.DatabaseProvider;
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.CaptionStyleCompat;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.MimeTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExoUtil {

    private static DatabaseProvider database;
    private static Cache cache;

    public static CaptionStyleCompat getCaptionStyle() {
        return new CaptionStyleCompat(Color.WHITE, Color.TRANSPARENT, Color.TRANSPARENT, CaptionStyleCompat.EDGE_TYPE_OUTLINE, Color.BLACK, null);
    }

    public static MediaSource getSource(Result result) {
        return getSource(result.getHeaders(), result.getPlayUrl() + result.getUrl(), getConfig(result));
    }

    public static MediaSource getSource(Map<String, String> headers, String url) {
        return getSource(headers, url, Collections.emptyList());
    }

    private static MediaSource getSource(Map<String, String> headers, String url, List<MediaItem.SubtitleConfiguration> config) {
        MediaItem.Builder builder = new MediaItem.Builder().setUri(Uri.parse(url));
        if (url.contains("php") || url.contains("m3u8")) builder.setMimeType(MimeTypes.APPLICATION_M3U8);
        if (config.size() > 0) builder.setSubtitleConfigurations(config);
        return new DefaultMediaSourceFactory(getDataSourceFactory(headers)).createMediaSource(builder.build());
    }

    private static List<MediaItem.SubtitleConfiguration> getConfig(Result result) {
        if (result.getSub().isEmpty()) return Collections.emptyList();
        List<MediaItem.SubtitleConfiguration> items = new ArrayList<>();
        String[] subs = result.getSub().split("\\$\\$\\$");
        for (String sub : subs) {
            String[] divide = sub.split("#");
            items.add(new MediaItem.SubtitleConfiguration.Builder(Uri.parse(divide[2])).setLabel(divide[0]).setMimeType(divide[1]).setLanguage("zh").build());
        }
        return items;
    }

    private static synchronized DataSource.Factory getHttpDataSourceFactory(Map<String, String> headers) {
        return new DefaultHttpDataSource.Factory().setDefaultRequestProperties(headers).setConnectTimeoutMs(5000).setReadTimeoutMs(5000).setAllowCrossProtocolRedirects(true);
    }

    private static synchronized DataSource.Factory getDataSourceFactory(Map<String, String> headers) {
        DefaultDataSource.Factory upstreamFactory = new DefaultDataSource.Factory(App.get(), getHttpDataSourceFactory(headers));
        return buildReadOnlyCacheDataSource(upstreamFactory, getCache());
    }

    private static CacheDataSource.Factory buildReadOnlyCacheDataSource(DataSource.Factory upstreamFactory, Cache cache) {
        return new CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(upstreamFactory).setCacheWriteDataSinkFactory(null).setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    private static synchronized DatabaseProvider getDatabase() {
        if (database == null) database = new StandaloneDatabaseProvider(App.get());
        return database;
    }

    private static synchronized Cache getCache() {
        if (cache == null) cache = new SimpleCache(FileUtil.getCacheDir(), new NoOpCacheEvictor(), getDatabase());
        return cache;
    }
}
