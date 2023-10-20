package com.fongmi.android.tv.utils;

import android.net.Uri;

import androidx.media3.common.util.UriUtil;

import com.fongmi.android.tv.server.Server;
import com.github.catvod.utils.Util;
import com.google.common.net.HttpHeaders;

public class UrlUtil {

    public static Uri uri(String url) {
        return Uri.parse(url.trim().replace("\\", ""));
    }

    public static String checkClan(String url) {
        if (url.contains("/localhost/")) url = url.replace("/localhost/", "/");
        if (url.startsWith("clan")) url = url.replace("clan", "file");
        return url;
    }

    public static String convert(String baseUrl, String path) {
        if (path.startsWith("clan")) return checkClan(path);
        return path.isEmpty() ? "" : UriUtil.resolve(baseUrl, path);
    }

    public static String convert(String url) {
        String host = Util.host(url);
        String scheme = Util.scheme(url);
        if ("file".equals(scheme)) return Server.get().getAddress(url);
        if ("local".equals(scheme)) return Server.get().getAddress(host);
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
