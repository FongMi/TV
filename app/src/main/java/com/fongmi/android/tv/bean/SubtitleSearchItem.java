package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.fongmi.android.tv.player.track.TrackUtil;
import com.fongmi.android.tv.utils.SubtitleArchive;
import com.fongmi.android.tv.utils.UrlUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class SubtitleSearchItem {

    private final int id;
    private final String name;
    private final String url;
    private final String lang;

    private SubtitleSearchItem(int id, String name, String url, String lang) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.lang = lang;
    }

    public static SubtitleSearchItem from(int id, String name, String url, String lang) {
        return new SubtitleSearchItem(id, name, url, lang);
    }

    public static List<SubtitleSearchItem> fromFiles(List<File> files, String lang) {
        List<SubtitleSearchItem> items = new ArrayList<>();
        for (File file : files) items.add(new SubtitleSearchItem(0, file.getName(), file.getAbsolutePath(), lang));
        return items;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? String.valueOf(id) : name;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : url;
    }

    public String getLang() {
        return TextUtils.isEmpty(lang) ? "" : lang;
    }

    public String getText() {
        if (TextUtils.isEmpty(getLang())) return getName();
        return getName() + " - " + getLang();
    }

    public boolean hasUrl() {
        return !getUrl().isEmpty();
    }

    public boolean isZip() {
        return SubtitleArchive.isZip(getName(), getUrl());
    }

    public boolean isRemote() {
        String scheme = UrlUtil.scheme(getUrl());
        return scheme.equals("http") || scheme.equals("https");
    }

    public Sub toSub() {
        return Sub.from(getName(), getUrl(), getLang(), TrackUtil.getSubtitleMimeType(getUrl()));
    }
}
