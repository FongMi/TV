package com.fongmi.android.tv.player.extractor;

import android.net.Uri;

import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.google.common.net.HttpHeaders;

import java.io.File;

import okhttp3.Response;

public class Strm implements Source.Extractor {

    @Override
    public boolean match(Uri uri) {
        return UrlUtil.path(uri).endsWith(".strm");
    }

    @Override
    public String fetch(String url) throws Exception {
        if (url.startsWith("http")) return http(url);
        if (url.startsWith("file")) url = url.substring(7);
        return Path.read(new File(url)).split("\\R", 2)[0];
    }

    private String http(String url) throws Exception {
        try (Response res = OkHttp.newCall(OkHttp.noRedirect(), url).execute()) {
            String content = res.header(HttpHeaders.CONTENT_DISPOSITION, "");
            boolean text = content.contains(".strm") || content.contains(".txt");
            return text ? res.body().string().split("\\R", 2)[0] : url;
        }
    }
}
