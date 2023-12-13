package com.fongmi.android.tv.utils;

import android.net.Uri;

import androidx.media3.common.util.UriUtil;

import com.github.catvod.net.OkHttp;
import com.google.common.net.HttpHeaders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.Response;

public class M3U8 {

    private static final String TAG_DISCONTINUITY = "#EXT-X-DISCONTINUITY";
    private static final String TAG_MEDIA_DURATION = "#EXTINF";
    private static final String TAG_ENDLIST = "#EXT-X-ENDLIST";
    private static final String TAG_KEY = "#EXT-X-KEY";

    private static final Pattern REGEX_X_DISCONTINUITY = Pattern.compile("#EXT-X-DISCONTINUITY[\\s\\S]*?(?=#EXT-X-DISCONTINUITY|$)");
    private static final Pattern REGEX_MEDIA_DURATION = Pattern.compile(TAG_MEDIA_DURATION + ":([\\d\\.]+)\\b");
    private static final Pattern REGEX_URI = Pattern.compile("URI=\"(.+?)\"");

    public static String get(String url, Map<String, String> headers) {
        try {
            Response response = OkHttp.newCall(url, getHeader(headers)).execute();
            if (response.header(HttpHeaders.ACCEPT_RANGES) != null) return "";
            String result = response.body().string();
            Matcher matcher = Pattern.compile("#EXT-X-STREAM-INF(.*)\\n?(.*)").matcher(result);
            if (matcher.find() && matcher.groupCount() > 1) return get(UriUtil.resolve(url, matcher.group(2)), headers);
            StringBuilder sb = new StringBuilder();
            for (String line : result.split("\n")) sb.append(shouldResolve(line) ? resolve(url, line) : line).append("\n");
            List<String> ads = Sniffer.getRegex(Uri.parse(url));
            return clean(sb.toString(), ads);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String clean(String line, List<String> ads) {
        boolean scan = false;
        for (String ad : ads) {
            if (ad.contains(TAG_DISCONTINUITY) || ad.contains(TAG_MEDIA_DURATION)) line = line.replaceAll(ad, "");
            else if (isDouble(ad)) scan = true;
        }
        return scan ? scan(line, ads) : line;
    }

    private static String scan(String line, List<String> ads) {
        Matcher m1 = REGEX_X_DISCONTINUITY.matcher(line);
        while (m1.find()) {
            String group = m1.group();
            BigDecimal t = BigDecimal.ZERO;
            Matcher m2 = REGEX_MEDIA_DURATION.matcher(group);
            while (m2.find()) t = t.add(new BigDecimal(m2.group(1)));
            for (String ad : ads) if (t.toString().startsWith(ad)) line = line.replace(group.replace(TAG_ENDLIST, ""), "");
        }
        return line;
    }

    private static Headers getHeader(Map<String, String> headers) {
        Headers.Builder builder = new Headers.Builder();
        for (Map.Entry<String, String> header : headers.entrySet()) if (header.getKey().equalsIgnoreCase(HttpHeaders.USER_AGENT) || header.getKey().equalsIgnoreCase(HttpHeaders.REFERER) || header.getKey().equalsIgnoreCase(HttpHeaders.COOKIE)) builder.add(header.getKey(), header.getValue());
        builder.add(HttpHeaders.RANGE, "bytes=0-");
        return builder.build();
    }

    private static boolean isDouble(String ad) {
        try {
            return Double.parseDouble(ad) > 0;
        } catch (Exception e) {
            return false;
        }
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
