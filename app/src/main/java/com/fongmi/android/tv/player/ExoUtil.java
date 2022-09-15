package com.fongmi.android.tv.player;

import android.graphics.Color;
import android.net.Uri;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Result;
import com.github.catvod.crawler.SpiderDebug;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSource;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.CaptionStyleCompat;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.MimeTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExoUtil {

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
        SpiderDebug.log(url);
        Uri videoUri = Uri.parse(url);
        DataSource.Factory factory = getFactory(headers, url);
        MediaItem.Builder builder = new MediaItem.Builder().setUri(videoUri);
        if (url.contains("php") || url.contains("m3u8")) builder.setMimeType(MimeTypes.APPLICATION_M3U8);
        if (config.size() > 0) builder.setSubtitleConfigurations(config);
        return new DefaultMediaSourceFactory(factory).createMediaSource(builder.build());
    }

    private static List<MediaItem.SubtitleConfiguration> getConfig(Result result) {
        if (result.getSub().isEmpty()) return Collections.emptyList();
        List<MediaItem.SubtitleConfiguration> items = new ArrayList<>();
        String[] subs = result.getSub().split("\\$\\$\\$");
        for (String sub : subs) {
            String[] divide = sub.split("#");
            items.add(new MediaItem.SubtitleConfiguration.Builder(Uri.parse(divide[2])).setLabel(divide[0]).setMimeType(divide[1]).build());
        }
        return items;
    }

    private static DataSource.Factory getFactory(Map<String, String> headers, String url) {
        HttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory().setDefaultRequestProperties(headers).setConnectTimeoutMs(10000).setReadTimeoutMs(10000).setAllowCrossProtocolRedirects(true);
        return url.startsWith("rtmp") ? new RtmpDataSource.Factory() : new DefaultDataSource.Factory(App.get(), httpDataSourceFactory);
    }
}
