package com.fongmi.android.tv.player.extractor;

import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.utils.Sniffer;
import com.github.catvod.net.OkHttp;
import com.google.common.net.HttpHeaders;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;

public class Youtube implements Source.Extractor {

    @Override
    public boolean match(String scheme, String host) {
        return host.contains("youtube.com");
    }

    @Override
    public String fetch(String url) throws Exception {
        String result = OkHttp.newCall(url, Headers.of(HttpHeaders.USER_AGENT, Sniffer.CHROME)).execute().body().string();
        Pattern pattern = Pattern.compile("hlsManifestUrl\\S*?(https\\S*?\\.m3u8)");
        Matcher matcher = pattern.matcher(result);
        if (!matcher.find()) return "";
        String stable = matcher.group(1);
        result = OkHttp.newCall(stable, Headers.of(HttpHeaders.USER_AGENT, Sniffer.CHROME)).execute().body().string();
        String quality = find(result);
        return quality.isEmpty() ? url : quality;
    }

    private String find(String result) {
        String url = "";
        List<String> items = Arrays.asList("301", "300", "96", "95", "94");
        for (String item : items) if (!(url = find(result, "https:/.*/" + item + "/.*index.m3u8")).isEmpty()) break;
        return url;
    }

    private String find(String result, String rule) {
        Pattern pattern = Pattern.compile(rule);
        Matcher matcher = pattern.matcher(result);
        if (matcher.find()) return matcher.group();
        return "";
    }

    @Override
    public void stop() {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void release() {
    }
}
