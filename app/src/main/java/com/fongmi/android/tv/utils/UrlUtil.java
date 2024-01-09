package com.fongmi.android.tv.utils;

import android.net.Uri;

import com.fongmi.android.tv.server.Server;
import com.google.common.net.HttpHeaders;

public class UrlUtil {

    public static Uri uri(String url) {
        return Uri.parse(url.trim().replace("\\", ""));
    }

    public static String scheme(String url) {
        return url == null ? "" : scheme(Uri.parse(url));
    }

    public static String scheme(Uri uri) {
        String scheme = uri.getScheme();
        return scheme == null ? "" : scheme.toLowerCase().trim();
    }

    public static String host(String url) {
        return url == null ? "" : host(Uri.parse(url));
    }

    public static String host(Uri uri) {
        String host = uri.getHost();
        return host == null ? "" : host.toLowerCase().trim();
    }

    public static String path(Uri uri) {
        String path = uri.getPath();
        return path == null ? "" : path.trim();
    }

    public static String convert(String url) {
        String scheme = scheme(url);
        if ("local".equals(scheme)) return url.replace("local://", Server.get().getAddress(""));
        if ("assets".equals(scheme)) return url.replace("assets://", Server.get().getAddress(""));
        if ("file".equals(scheme)) return url.replace("file://", Server.get().getAddress("file/"));
        if ("proxy".equals(scheme)) return url.replace("proxy://", Server.get().getAddress("proxy?"));
        return url;
    }

    public static String fixHeader(String key) {
        if (key.equalsIgnoreCase(HttpHeaders.USER_AGENT)) return HttpHeaders.USER_AGENT;
        if (key.equalsIgnoreCase(HttpHeaders.REFERER)) return HttpHeaders.REFERER;
        if (key.equalsIgnoreCase(HttpHeaders.COOKIE)) return HttpHeaders.COOKIE;
        return key;
    }
}
