package com.fongmi.android.tv.player;

import android.graphics.Color;
import android.net.Uri;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Result;
import com.google.android.exoplayer2.C;
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
import com.google.common.collect.ImmutableList;

import java.util.Map;

public class ExoUtil {

    public static CaptionStyleCompat getCaptionStyle() {
        return new CaptionStyleCompat(Color.WHITE, Color.TRANSPARENT, Color.TRANSPARENT, CaptionStyleCompat.EDGE_TYPE_OUTLINE, Color.BLACK, null);
    }

    public static MediaSource getSource(Result result) {
        return getSource(result.getHeaders(), result.getPlayUrl() + result.getUrl(), getConfig(result));
    }

    public static MediaSource getSource(Map<String, String> headers, String url) {
        return getSource(headers, url, null);
    }

    private static MediaSource getSource(Map<String, String> headers, String url, MediaItem.SubtitleConfiguration config) {
        Uri videoUri = Uri.parse(url);
        DataSource.Factory factory = getFactory(headers, url);
        MediaItem.Builder builder = new MediaItem.Builder().setUri(videoUri);
        if (url.contains("php") || url.contains("m3u8")) builder.setMimeType(MimeTypes.APPLICATION_M3U8);
        if (config != null) builder.setSubtitleConfigurations(ImmutableList.of(config));
        return new DefaultMediaSourceFactory(factory).createMediaSource(builder.build());
    }

    private static MediaItem.SubtitleConfiguration getConfig(Result result) {
        return result.getSub().isEmpty() ? null : new MediaItem.SubtitleConfiguration.Builder(Uri.parse(result.getSub())).setMimeType(MimeTypes.APPLICATION_SUBRIP).setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build();
    }

    private static DataSource.Factory getFactory(Map<String, String> headers, String url) {
        HttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory().setDefaultRequestProperties(headers).setConnectTimeoutMs(10000).setReadTimeoutMs(10000).setAllowCrossProtocolRedirects(true);
        return url.startsWith("rtmp") ? new RtmpDataSource.Factory() : new DefaultDataSource.Factory(App.get(), httpDataSourceFactory);
    }
}
