package com.fongmi.android.tv.player.engine;

import android.net.Uri;

import androidx.media3.common.MediaMetadata;

import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.bean.Drm;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.player.PlayerHelper;
import com.fongmi.android.tv.utils.UrlUtil;
import com.google.common.net.HttpHeaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaySpec {

    private Map<String, String> headers;
    private List<Danmaku> danmakus;
    private MediaMetadata metadata;
    private List<Sub> subs;
    private String format;
    private String key;
    private String url;
    private Drm drm;

    public static PlaySpec from(String key, String url, Map<String, String> headers, MediaMetadata metadata) {
        return new PlaySpec(key, url, headers, null, null, null, null, metadata);
    }

    public static PlaySpec from(Result result, String key, MediaMetadata metadata) {
        return new PlaySpec(key, result.getRealUrl(), result.getHeader(), result.getFormat(), result.getDrm(), result.getSubs(), result.getDanmaku(), metadata);
    }

    public static PlaySpec fromParse(Result result, String key, MediaMetadata metadata) {
        return new PlaySpec(key, null, null, result.getFormat(), result.getDrm(), result.getSubs(), result.getDanmaku(), metadata);
    }

    private PlaySpec(String key, String url, Map<String, String> headers, String format, Drm drm, List<Sub> subs, List<Danmaku> danmakus, MediaMetadata metadata) {
        this.key = key;
        this.url = url;
        this.drm = drm;
        this.subs = subs;
        this.format = format;
        this.headers = headers;
        this.danmakus = danmakus;
        this.metadata = metadata;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Uri getUri() {
        return UrlUtil.uri(url);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Drm getDrm() {
        return drm;
    }

    public List<Sub> getSubs() {
        return subs;
    }

    public List<Danmaku> getDanmakus() {
        return danmakus;
    }

    public MediaMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(MediaMetadata metadata) {
        this.metadata = metadata;
    }

    public PlaySpec checkUa() {
        if (headers == null) headers = new HashMap<>();
        if (headers.keySet().stream().noneMatch(HttpHeaders.USER_AGENT::equalsIgnoreCase)) headers.put(HttpHeaders.USER_AGENT, Setting.getUa().isEmpty() ? PlayerHelper.getDefaultUa() : Setting.getUa());
        return this;
    }

    public void setSub(Sub sub) {
        if (subs == null) subs = new ArrayList<>();
        if (sub != null && !subs.contains(sub)) subs.add(0, sub);
    }

    public void setDanmaku(Danmaku item) {
        if (danmakus == null) danmakus = new ArrayList<>();
        if (!item.isEmpty() && !danmakus.contains(item)) danmakus.add(0, item);
        danmakus.forEach(danmaku -> danmaku.setSelected(danmaku.getUrl().equals(item.getUrl())));
    }
}
