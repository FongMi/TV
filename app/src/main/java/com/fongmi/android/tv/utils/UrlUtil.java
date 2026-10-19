package com.fongmi.android.tv.utils;

import android.content.ContentResolver;
import android.net.Uri;
import android.text.TextUtils;

import com.fongmi.android.tv.server.Server;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.UriUtil;
import com.google.common.net.HttpHeaders;

import java.io.File;

public class UrlUtil {

    private static final String WEBVIEW_PREFIX = "webview://";

    public static boolean isWebView(String url) {
        return url != null && url.regionMatches(true, 0, WEBVIEW_PREFIX, 0, WEBVIEW_PREFIX.length());
    }

    public static String unwrapWebView(String url) {
        return isWebView(url) ? url.substring(WEBVIEW_PREFIX.length()) : "";
    }

    public static String toLocalUrl(Uri uri) {
        String path = uri.getPath();
        String root = Path.rootPath();
        if (!ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme()) || TextUtils.isEmpty(path) || !path.startsWith(root + File.separator)) return uri.toString();
        return "file://" + path.substring(root.length() + 1);
    }

    public static String convert(String url) {
        String scheme = UriUtil.scheme(url);
        String prefix = scheme + "://";
        return switch (scheme) {
            case "assets" -> url.replace(prefix, Server.get().getAddress("/"));
            case "proxy" -> url.replace(prefix, Server.get().getAddress("/proxy?"));
            case "file" -> Server.get().getAddress("/file/") + Uri.encode(url.substring((prefix).length()), "/");
            default -> url;
        };
    }

    public static String fixHeader(String key) {
        if (HttpHeaders.USER_AGENT.equalsIgnoreCase(key) || "ua".equalsIgnoreCase(key)) return HttpHeaders.USER_AGENT;
        if (HttpHeaders.REFERER.equalsIgnoreCase(key)) return HttpHeaders.REFERER;
        if (HttpHeaders.COOKIE.equalsIgnoreCase(key)) return HttpHeaders.COOKIE;
        return key;
    }
}
