package com.fongmi.android.tv.utils;

import android.text.TextUtils;

import androidx.media3.common.util.UriUtil;

import com.fongmi.android.tv.server.Server;
import com.github.catvod.utils.Util;

public class UrlUtil {

    public static String checkClan(String text) {
        if (text.contains("/localhost/")) text = text.replace("/localhost/", "/");
        if (text.startsWith("clan")) text = text.replace("clan", "file");
        return text;
    }

    public static String convert(String baseUrl, String text) {
        if (TextUtils.isEmpty(text)) return "";
        if (text.startsWith("clan")) return checkClan(text);
        return UriUtil.resolve(baseUrl, text);
    }

    public static String convert(String url) {
        String host = Util.host(url);
        String scheme = Util.scheme(url);
        if ("file".equals(scheme)) return Server.get().getAddress(url);
        if ("local".equals(scheme)) return Server.get().getAddress(host);
        if ("proxy".equals(scheme)) return url.replace("proxy://", Server.get().getAddress("proxy?"));
        return url;
    }

}
