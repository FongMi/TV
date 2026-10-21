package com.fongmi.android.tv.player.extractor;

import android.content.ContentResolver;
import android.net.Uri;

import com.fongmi.android.tv.utils.FileUtil;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.UriUtil;
import com.google.common.net.HttpHeaders;

import okhttp3.Response;

public class Strm implements Source.Extractor {

    @Override
    public boolean match(Uri uri) {
        String name = ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme()) ? FileUtil.getDisplayName(uri) : UriUtil.path(uri);
        return name.endsWith(".strm");
    }

    @Override
    public String fetch(String url) throws Exception {
        if (url.startsWith("http")) return http(url);
        return Path.readOrThrow(Path.open(UriUtil.uri(url), "Unable to open STRM file")).split("\\R", 2)[0];
    }

    private String http(String url) throws Exception {
        try (Response res = OkHttp.newCall(OkHttp.noRedirect(), url).execute()) {
            String content = res.header(HttpHeaders.CONTENT_DISPOSITION, "");
            boolean text = content.contains(".strm") || content.contains(".txt");
            return text ? res.body().string().split("\\R", 2)[0] : url;
        }
    }
}
