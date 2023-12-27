package com.fongmi.android.tv.utils;

import android.net.Uri;

import androidx.media3.common.util.UriUtil;

import com.github.catvod.net.OkHttp;
import com.google.common.net.HttpHeaders;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
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
        class Slice{
            private String text;
            private BigDecimal time;
            private String prefix;

            Slice(String text){
                this.text = text;

                BigDecimal t = BigDecimal.ZERO;
                Matcher m2 = REGEX_MEDIA_DURATION.matcher(text);
                while (m2.find()) t = t.add(new BigDecimal(m2.group(1)));
                this.time = t;

                int prefixLen = 0;
                String[] lines = text.split("\n");
                for (String line : lines) {
                    if (!line.startsWith("#")) {
                        if (prefixLen == 0 || (line.length() < prefixLen  && line.strip().length() > 0)) {
                            prefixLen = line.length();
                        }
                    }
                }
                prefixLen -= 7;
                String prefix = null;
                while(prefixLen > 0) {
                    boolean redo = false;
                    for (String line : lines) {
                        if (!line.startsWith("#")) {
                            if (prefix == null) {
                                prefix = line.substring(0, prefixLen);
                            } else {
                                if (!line.startsWith(prefix)) {
                                    redo = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (redo) {
                        prefixLen -= 1;
                        prefix = null;
                    } else {
                        break;
                    }
                }
                this.prefix = prefix;
            }

            public String getPrefix() {
                return this.prefix;
            }

            public BigDecimal getTime() {
                return this.time;
            }

            public String getText() {
                return this.text;
            }
        }

        double adTimeLong = 0.0;
        for (String ad : ads) {
            if (isDouble(ad)) {
                double t = Double.parseDouble(ad);
                if (adTimeLong < t) {
                    adTimeLong = t;
                }
            }
        }

        ArrayList<Slice> slices = new ArrayList<>();
        HashMap<String, BigDecimal> slicesTime = new HashMap<>();
        int idx = 0;
        while (idx < line.length()) {
            int i = line.indexOf(TAG_DISCONTINUITY, idx + 1);
            Slice slice;
            if (i == -1) {
                slice = new Slice(line.substring(idx));
                idx = line.length();
            } else {
                slice = new Slice(line.substring(idx, i));
                idx = i;
            }
            slices.add(slice);
            if (slice.getPrefix() != null) {
                BigDecimal t = slicesTime.get(slice.getPrefix());
                if (t == null) {
                    slicesTime.put(slice.getPrefix(), slice.getTime());
                } else {
                    slicesTime.put(slice.getPrefix(), t.add(slice.getTime()));
                }
            }
        }

        ArrayList<Slice> listOk = slices;
        for (String key : slicesTime.keySet()) {
            BigDecimal t = slicesTime.get(key);
            if (new BigDecimal(adTimeLong).compareTo(t) >= 0) {
                ArrayList<Slice> list = new ArrayList<>();
                for (Slice slice : listOk) {
                    if (!key.equals(slice.getPrefix())) {
                        list.add(slice);
                    }
                }
                listOk = list;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (Slice slice : listOk) {
            sb.append(slice.getText());
        }
        return sb.toString();
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
