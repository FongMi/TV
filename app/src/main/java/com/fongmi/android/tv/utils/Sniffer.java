package com.fongmi.android.tv.utils;

import android.net.Uri;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Rule;
import com.github.catvod.crawler.SpiderDebug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Sniffer {

    public static final String CHROME = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36";
    public static final Pattern RULE = Pattern.compile("http((?!http).){12,}?\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg|m4a|mp3)\\?.*|http((?!http).){12,}\\.(m3u8|mp4|flv|avi|mkv|rm|wmv|mpg|m4a|mp3)|http((?!http).)*?video/tos*");

    private static boolean matchOrContain(String url) {
        for (String regex : getRegex(Uri.parse(url))) return Pattern.compile(regex).matcher(url).find() || url.contains(regex);
        return false;
    }

    private static boolean match(String url) {
        for (String regex : getRegex()) return Pattern.compile(regex).matcher(url).find();
        return false;
    }

    public static boolean isVideoFormat(String url) {
        return isVideoFormat(url, new HashMap<>());
    }

    public static boolean isVideoFormat(String url, Map<String, String> headers) {
        SpiderDebug.log(url);
        if (matchOrContain(url)) return true;
        if (headers.containsKey("Accept") && headers.get("Accept").startsWith("image")) return false;
        if (url.contains("url=http") || url.contains("v=http") || url.contains(".css") || url.contains(".html")) return false;
        return match(url) || RULE.matcher(url).find();
    }

    public static boolean isAds(Uri uri) {
        for (String regex : getRegex(uri)) if (regex.contains("#EXTINF")) return true;
        return false;
    }

    public static List<String> getRegex() {
        List<String> regex = new ArrayList<>();
        for (Rule rule : ApiConfig.get().getRules()) for (String host : rule.getHosts()) if (host.equals("*")) regex.addAll(rule.getRegex());
        return regex;
    }

    public static List<String> getRegex(Uri uri) {
        if (uri.getHost() == null) return Collections.emptyList();
        for (Rule rule : ApiConfig.get().getRules()) for (String host : rule.getHosts()) if (uri.getHost().contains(host)) return rule.getRegex();
        return Collections.emptyList();
    }
}
