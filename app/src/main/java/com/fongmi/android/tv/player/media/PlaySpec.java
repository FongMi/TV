package com.fongmi.android.tv.player.media;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaMetadata;

import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.bean.Drm;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.utils.UrlUtil;
import com.google.common.net.HttpHeaders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaySpec {

    private Map<String, String> headers;
    private final List<Danmaku> danmakus;
    private MediaMetadata metadata;
    private List<Sub> subs;
    private String format;
    private String key;
    private String url;
    private Drm drm;

    private PlaySpec(String key, String url, Map<String, String> headers, String format, Drm drm, List<Sub> subs, List<Danmaku> danmakus, MediaMetadata metadata) {
        this.key = key;
        this.url = url;
        this.drm = drm;
        this.subs = subs;
        this.format = format;
        this.headers = headers;
        this.danmakus = initDanmakus(danmakus);
        this.metadata = metadata;
    }

    private static List<Danmaku> initDanmakus(List<Danmaku> items) {
        List<Danmaku> danmakus = items == null ? new ArrayList<>() : new ArrayList<>(items);
        danmakus.forEach(item -> item.setSelected(false));
        if (!danmakus.isEmpty()) danmakus.get(0).setSelected(true);
        return danmakus;
    }

    public static PlaySpec from(String key, String url, Map<String, String> headers, MediaMetadata metadata) {
        return new PlaySpec(key, url, headers, null, null, null, null, metadata);
    }

    public static PlaySpec from(Result result, String key, MediaMetadata metadata) {
        return new PlaySpec(key, result.getRealUrl(), result.getHeader(), result.getFormat(), result.getDrm(), result.getSubs(), result.getDanmaku(), metadata);
    }

    public static PlaySpec fromParse(Result result, String key, MediaMetadata metadata) {
        return new PlaySpec(key, null, null, result.getFormat(), result.getDrm(), result.getSubs(), result.getDanmaku(), metadata);
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
        return Collections.unmodifiableList(danmakus);
    }

    @Nullable
    public Danmaku getSelectedDanmaku() {
        return danmakus.stream().filter(Danmaku::isSelected).findFirst().orElse(null);
    }

    public MediaMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(MediaMetadata metadata) {
        this.metadata = metadata;
    }

    public PlaySpec checkUa() {
        if (headers == null) headers = new HashMap<>();
        if (headers.keySet().stream().noneMatch(HttpHeaders.USER_AGENT::equalsIgnoreCase)) headers.put(HttpHeaders.USER_AGENT, Setting.getUa().isEmpty() ? MediaItemFactory.getDefaultUserAgent() : Setting.getUa());
        return this;
    }

    public void setSub(Sub sub) {
        if (subs == null) subs = new ArrayList<>();
        if (sub == null) return;
        subs.remove(sub);
        clearForcedSubtitles();
        subs.add(0, sub);
    }

    private void clearForcedSubtitles() {
        for (Sub sub : subs) if (sub.isForced()) sub.setFlag(C.SELECTION_FLAG_AUTOSELECT);
    }

    public void selectDanmaku(Danmaku item) {
        if (item == null || item.isEmpty()) return;
        int index = danmakus.indexOf(item);
        if (index < 0) danmakus.add(index = 0, item);
        for (int i = 0; i < danmakus.size(); i++) danmakus.get(i).setSelected(i == index);
    }

    public void toggleDanmaku(Danmaku item) {
        if (item == null || item.isEmpty()) return;
        if (item.equals(getSelectedDanmaku())) clearDanmaku();
        else selectDanmaku(item);
    }

    private void clearDanmaku() {
        danmakus.forEach(item -> item.setSelected(false));
    }

    public void addDanmaku(Danmaku item) {
        if (item == null || item.isEmpty() || danmakus.contains(item)) return;
        item.setSelected(false);
        danmakus.add(item);
    }
}
