package com.fongmi.android.tv.utils;

import androidx.media3.common.util.UriUtil;

import com.github.catvod.net.OkHttp;
import com.google.common.net.HttpHeaders;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;

public class M3U8 {

    private static final Pattern REGEX_URI = Pattern.compile("URI=\"(.+?)\"");
    private static final String TAG_KEY = "#EXT-X-KEY";

    public static String get(String url, Map<String, String> headers) throws Exception {
        String result = OkHttp.newCall(url, getHeader(headers)).execute().body().string();
        Matcher matcher = Pattern.compile("#EXT-X-STREAM-INF(.*)\\n?(.*)").matcher(result);
        if (matcher.find() && matcher.groupCount() > 1) return get(UriUtil.resolve(url, matcher.group(2)), headers);
        StringBuilder sb = new StringBuilder();
        for (String line : result.split("\n")) sb.append(shouldResolve(line) ? resolve(url, line) : line).append("\n");
        return sb.toString();
    }

    private static Headers getHeader(Map<String, String> headers) {
        Headers.Builder builder = new Headers.Builder();
        if (headers.containsKey(HttpHeaders.USER_AGENT)) builder.add(HttpHeaders.USER_AGENT, Objects.requireNonNull(headers.get(HttpHeaders.USER_AGENT)));
        if (headers.containsKey(HttpHeaders.REFERER)) builder.add(HttpHeaders.REFERER, Objects.requireNonNull(headers.get(HttpHeaders.REFERER)));
        return builder.build();
    }

    private static boolean shouldResolve(String line) {
        return (!line.startsWith("#") && !line.startsWith("http")) || line.startsWith(TAG_KEY);
    }

    private static String resolve(String base, String line) {
        if (line.startsWith(TAG_KEY)) {
            Matcher matcher = REGEX_URI.matcher(line);
            String value = matcher.find() ? matcher.group(1) : null;
            return value == null ? line : line.replace(value, UriUtil.resolve(base, value));
        } else {
            return UriUtil.resolve(base, line);
        }
    }
}
