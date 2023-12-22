package com.fongmi.android.tv.utils;

import android.net.Uri;

import androidx.media3.common.util.UriUtil;

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

    public static String convert(String baseUrl, String path) {
        if (path.startsWith("clan")) return fixUrl(path);
        if (path.startsWith("assets")) return convert(path);
        return path.isEmpty() ? "" : UriUtil.resolve(baseUrl, path);
    }

    public static String convert(String url) {
        String host = host(url);
        String scheme = scheme(url);
        if ("file".equals(scheme)) return Server.get().getAddress(url);
        if ("local".equals(scheme)) return Server.get().getAddress(host);
        if ("assets".equals(scheme)) return Server.get().getAddress(url.substring(9));
        if ("proxy".equals(scheme)) return url.replace("proxy://", Server.get().getAddress("proxy?"));
        return url;
    }

    public static String fixUrl(String url) {
        if (url.contains("/localhost/")) url = url.replace("/localhost/", "/");
        if (url.startsWith("clan")) url = url.replace("clan", "file");
        return url;
    }

    public static String fixHeader(String key) {
        if (key.equalsIgnoreCase(HttpHeaders.USER_AGENT)) return HttpHeaders.USER_AGENT;
        if (key.equalsIgnoreCase(HttpHeaders.REFERER)) return HttpHeaders.REFERER;
        if (key.equalsIgnoreCase(HttpHeaders.COOKIE)) return HttpHeaders.COOKIE;
        return key;
    }
}
