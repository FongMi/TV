package com.fongmi.android.tv.utils;

import android.net.Uri;

import com.github.catvod.utils.Crypto;

import java.io.File;

public record TorrentUrl(String id, String name, int index) {

    private static final String PREFIX = "magnet://file?";

    public TorrentUrl {
        if (id == null || id.isEmpty() || name == null || name.isEmpty() || index < 0) throw new IllegalArgumentException("Invalid torrent selection");
    }

    public static boolean match(String url) {
        return url.regionMatches(true, 0, PREFIX, 0, PREFIX.length());
    }

    public static TorrentUrl from(String url) {
        if (!match(url)) throw new IllegalArgumentException("Invalid torrent selection");
        Uri uri = Uri.parse(url);
        String id = uri.getQueryParameter("id");
        String name = uri.getQueryParameter("name");
        int index = Integer.parseInt(uri.getQueryParameter("index"));
        return new TorrentUrl(id, name, index);
    }

    public static TorrentUrl fromLegacy(String url, String id) {
        Uri uri = Uri.parse(url);
        if (!"magnet".equalsIgnoreCase(uri.getScheme()) || uri.getPath() == null || uri.getPath().isEmpty()) return null;
        File parent = new File(uri.getPath()).getParentFile();
        if (parent == null || !parent.getName().equals(Crypto.md5(id))) return null;
        String name = uri.getQueryParameter("name");
        String index = uri.getQueryParameter("index");
        if (name == null || index == null) return null;
        return new TorrentUrl(id, name, Integer.parseInt(index));
    }

    public static String original(String url) {
        return match(url) ? from(url).id() : url;
    }

    public static String restore(String saved, String id) {
        if (match(saved)) return original(saved).equals(id) ? saved : null;
        TorrentUrl legacy = fromLegacy(saved, id);
        return legacy == null ? null : legacy.url();
    }

    public String url() {
        return new Uri.Builder().scheme("magnet").authority("file").appendQueryParameter("id", id).appendQueryParameter("name", name).appendQueryParameter("index", String.valueOf(index)).build().toString();
    }
}
