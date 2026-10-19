package com.fongmi.android.tv.utils;

import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;

import com.fongmi.android.tv.api.config.RuleConfig;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Rule;
import com.fongmi.android.tv.player.extractor.Source;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Trans;
import com.github.catvod.utils.UriUtil;
import com.github.catvod.utils.Util;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sniffer {

    public static final Pattern CLICKER = Pattern.compile("\\[a=cr:(\\{.*?\\})\\/](.*?)\\[\\/a]");
    public static final Pattern AI_PUSH = Pattern.compile("(https?|ftp|thunder|magnet|ed2k|video|webview|push):\\S+", Pattern.CASE_INSENSITIVE);
    public static final Pattern SNIFFER = Pattern.compile("https?://[^\\s]{12,}\\.(?:m3u8|mp4|mkv|flv|mp3|m4a|aac|mpd)|https?://.*?video/tos[^\\s]*|rtmp:[^\\s]+");

    public static String getUrl(String text) {
        if (Source.isPush(text) || "video".equals(UriUtil.scheme(text)) || UrlUtil.isWebView(text) || Json.isObj(text)) return text;
        Matcher m = AI_PUSH.matcher(text);
        if (!m.find()) return text;
        String url = m.group(0);
        if (text.contains("$") && (text.substring(0, m.start()).contains("$") || m.find())) return text;
        return url;
    }

    public static boolean isVideoFormat(String url) {
        return isVideoFormat(url, getRule(UriUtil.uri(url)));
    }

    static boolean isVideoFormat(String url, Rule rule) {
        for (String exclude : rule.getExclude()) if (url.contains(exclude)) return false;
        for (Pattern exclude : rule.getExcludePatterns()) if (exclude.matcher(url).find()) return false;
        for (String regex : rule.getRegex()) if (url.contains(regex)) return true;
        for (Pattern regex : rule.getRegexPatterns()) if (regex.matcher(url).find()) return true;
        String path = UriUtil.stripQueryAndFragment(url);
        if (url.contains("url=http") || url.contains("v=http") || path.contains(".html")) return false;
        return SNIFFER.matcher(url).find();
    }

    public static List<String> getScript(Uri uri) {
        return getRule(uri).getScript();
    }

    public static List<String> getClick(Uri uri) {
        return getRule(uri).getClick();
    }

    private static Rule getRule(Uri uri) {
        if (uri.getHost() == null) return Rule.empty();
        String hosts = TextUtils.join(",", Arrays.asList(UriUtil.host(uri), UriUtil.host(uri.getQueryParameter("url"))));
        for (Rule rule : RuleConfig.get().getRules()) for (String host : rule.getHosts()) if (Util.containOrMatch(hosts, host)) return rule;
        return Rule.empty();
    }

    public static SpannableStringBuilder buildClickable(String text, Function<Result, ClickableSpan> factory) {
        SpannableStringBuilder span = new SpannableStringBuilder();
        Matcher matcher = CLICKER.matcher(text);
        int last = 0;
        while (matcher.find()) {
            span.append(text, last, matcher.start());
            appendClickable(span, matcher, factory);
            last = matcher.end();
        }
        span.append(text, last, text.length());
        return span;
    }

    private static void appendClickable(SpannableStringBuilder span, Matcher matcher, Function<Result, ClickableSpan> factory) {
        int start = span.length();
        span.append(Trans.s2t(matcher.group(2).trim()));
        span.setSpan(factory.apply(Result.type(matcher.group(1))), start, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}
