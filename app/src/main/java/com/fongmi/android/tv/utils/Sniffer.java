package com.fongmi.android.tv.utils;

import android.net.Uri;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Rule;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Sniffer {

    public static final String CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36";
    public static final Pattern RULE = Pattern.compile("http((?!http).){12,}?\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg|m4a|mp3)\\?.*|http((?!http).){12,}\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg|m4a|mp3)|http((?!http).)*?video/tos*");

    public static boolean isVideoFormat(String url) {
        return isVideoFormat(url, new HashMap<>());
    }

    public static boolean isVideoFormat(String url, Map<String, String> headers) {
        if (matchOrContain(url)) return true;
        if (headers.containsKey("Accept") && headers.get("Accept").startsWith("image")) return false;
        if (url.contains("url=http") || url.contains("v=http") || url.contains(".css") || url.contains(".html")) return false;
        return match(url) || RULE.matcher(url).find();
    }

    private static boolean matchOrContain(String url) {
        Uri uri = Uri.parse(url);
        for (Rule rule : ApiConfig.get().getRules()) for (String host : rule.getHosts()) if (uri.getHost().contains(host)) for (String regex : rule.getRegex()) return Pattern.compile(regex).matcher(url).find() || url.contains(regex);
        return false;
    }

    private static boolean match(String url) {
        for (Rule rule : ApiConfig.get().getRules()) for (String host : rule.getHosts()) if (host.equals("*")) for (String regex : rule.getRegex()) return Pattern.compile(regex).matcher(url).find();
        return false;
    }
}
