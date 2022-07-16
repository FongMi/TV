package com.fongmi.bear.player;

import android.net.Uri;

import com.fongmi.bear.App;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import java.util.Map;

public class ExoUtil {

    public static MediaSource getSource(Map<String, String> headers, String url) {
        Uri videoUri = Uri.parse(url);
        DataSource.Factory factory = getFactory(headers, url);
        MediaItem mediaItem = new MediaItem.Builder().setUri(videoUri).build();
        int type = Util.inferContentType(videoUri);
        if (type == C.CONTENT_TYPE_HLS || url.contains(".php") || url.contains(".m3u8")) {
            return new HlsMediaSource.Factory(factory).createMediaSource(mediaItem);
        } else if (type == C.CONTENT_TYPE_DASH) {
            return new DashMediaSource.Factory(factory).createMediaSource(mediaItem);
        } else if (type == C.CONTENT_TYPE_SS) {
            return new SsMediaSource.Factory(factory).createMediaSource(mediaItem);
        } else if (type == C.CONTENT_TYPE_RTSP) {
            return new RtspMediaSource.Factory().createMediaSource(mediaItem);
        } else {
            return new ProgressiveMediaSource.Factory(factory).createMediaSource(mediaItem);
        }
    }

    private static DataSource.Factory getFactory(Map<String, String> headers, String url) {
        HttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory().setDefaultRequestProperties(headers).setAllowCrossProtocolRedirects(true);
        return url.startsWith("rtmp") ? new RtmpDataSource.Factory() : new DefaultDataSource.Factory(App.get(), httpDataSourceFactory);
    }
}
