package com.fongmi.android.tv.utils;

import androidx.media3.common.util.UriUtil;

import com.github.catvod.net.OkHttp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;

public class M3U8 {

    public static String get(String url, Headers headers) throws Exception {
        String result = OkHttp.newCall(url, headers).execute().body().string();
        Matcher matcher = Pattern.compile("#EXT-X-STREAM-INF(.*)\\n?(.*)").matcher(result);
        if (matcher.find() && matcher.groupCount() > 1) return get(UriUtil.resolve(url, matcher.group(2)), headers);
        StringBuilder sb = new StringBuilder();
        for (String line : result.split("\n")) sb.append(shouldResolve(line) ? UriUtil.resolve(url, line) : line).append("\n");
        return sb.toString();
    }

    private static boolean shouldResolve(String line) {
        return !line.startsWith("#") && !line.startsWith("http");
    }
}
