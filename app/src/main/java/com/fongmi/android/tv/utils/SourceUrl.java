package com.fongmi.android.tv.utils;

import android.net.Uri;

public record SourceUrl(String id, String target) {

    private static final String PREFIX = "source://episode?";

    public SourceUrl {
        if (id == null || id.isEmpty() || target == null || target.isEmpty()) throw new IllegalArgumentException("Invalid source selection");
    }

    public static boolean match(String url) {
        return url.regionMatches(true, 0, PREFIX, 0, PREFIX.length());
    }

    public static SourceUrl from(String url) {
        if (!match(url)) throw new IllegalArgumentException("Invalid source selection");
        Uri uri = Uri.parse(url);
        return new SourceUrl(uri.getQueryParameter("id"), uri.getQueryParameter("target"));
    }

    public static boolean isResolved(String url) {
        return match(url) || TorrentUrl.match(url);
    }

    public static String original(String url) {
        return match(url) ? from(url).id() : TorrentUrl.original(url);
    }

    public static boolean same(String first, String second) {
        if (match(first) && match(second)) return from(first).equals(from(second));
        return TorrentUrl.match(first) && TorrentUrl.match(second) && TorrentUrl.from(first).equals(TorrentUrl.from(second));
    }

    public static String restore(String saved, String id) {
        if (match(saved)) return from(saved).id().equals(id) ? saved : null;
        return TorrentUrl.restore(saved, id);
    }

    public String url() {
        return new Uri.Builder().scheme("source").authority("episode").appendQueryParameter("id", id).appendQueryParameter("target", target).build().toString();
    }
}
