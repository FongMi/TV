package com.fongmi.android.tv.utils;

import android.net.Uri;
import android.text.TextUtils;

import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Rule;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sniffer {

    public static final Pattern CLICKER = Pattern.compile("\\[a=cr:(\\{.*?\\})\\/](.*?)\\[\\/a]");
    public static final Pattern AI_PUSH = Pattern.compile("(https?|thunder|magnet|ed2k|video):\\S+");
    public static final Pattern SNIFFER = Pattern.compile("https?://[^\\s]{12,}\\.(?:m3u8|mp4|mkv|flv|mp3|m4a|aac|mpd)(?:\\?.*)?|https?://.*?video/tos[^\\s]*|rtmp:[^\\s]+");

    public static String getUrl(String text) {
        if (Json.isObj(text) || text.contains("$")) return text;
        Matcher m = AI_PUSH.matcher(text);
        if (m.find()) return m.group(0);
        return text;
    }

    public static boolean isVideoFormat(String url) {
        Rule rule = getRule(UrlUtil.uri(url));
        for (String exclude : rule.getExclude()) if (url.contains(exclude)) return false;
        for (String exclude : rule.getExclude()) if (Pattern.compile(exclude).matcher(url).find()) return false;
        for (String regex : rule.getRegex()) if (url.contains(regex)) return true;
        for (String regex : rule.getRegex()) if (Pattern.compile(regex).matcher(url).find()) return true;
        if (url.contains("url=http") || url.contains("v=http") || url.contains(".html")) return false;
        return SNIFFER.matcher(url).find();
    }

    public static List<String> getScript(Uri uri) {
        return getRule(uri).getScript();
    }

    private static Rule getRule(Uri uri) {
        if (uri.getHost() == null) return Rule.empty();
        String hosts = TextUtils.join(",", Arrays.asList(UrlUtil.host(uri), UrlUtil.host(uri.getQueryParameter("url"))));
        for (Rule rule : VodConfig.get().getRules()) for (String host : rule.getHosts()) if (Util.containOrMatch(hosts, host)) return rule;
        for (Rule rule : LiveConfig.get().getRules()) for (String host : rule.getHosts()) if (Util.containOrMatch(hosts, host)) return rule;
        return Rule.empty();
    }
}
