package com.fongmi.android.tv.player.extractor;

import android.content.ContentResolver;
import android.net.Uri;

import com.fongmi.android.tv.utils.FileUtil;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.UriUtil;

import java.util.Locale;

import okhttp3.Response;
import okhttp3.ResponseBody;

public class Strm implements Source.Extractor {

    private static final long MAX_STRM_BYTES = 64 * 1024;

    @Override
    public boolean match(Uri uri) {
        String name = ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme()) ? FileUtil.getDisplayName(uri) : UriUtil.path(uri);
        return name.toLowerCase(Locale.ROOT).endsWith(".strm");
    }

    @Override
    public String fetch(String url) throws Exception {
        String scheme = UriUtil.scheme(url);
        if (scheme.equals("http") || scheme.equals("https")) return http(url);
        return Path.readOrThrow(Path.open(UriUtil.uri(url), "Unable to open STRM file")).split("\\R", 2)[0];
    }

    private String http(String url) throws Exception {
        try (Response res = OkHttp.newCall(OkHttp.noRedirect(), url).execute()) {
            if (!res.isSuccessful()) return url;
            try (ResponseBody body = res.peekBody(MAX_STRM_BYTES)) {
                if (body.contentLength() >= MAX_STRM_BYTES) return url;
                return parseTarget(body.string(), url);
            }
        }
    }

    private String parseTarget(String content, String url) {
        if (content.startsWith("\uFEFF")) content = content.substring(1);
        String line = content.split("\\R", 2)[0].trim();
        return line.matches("[a-zA-Z][a-zA-Z0-9+.-]*:\\S+") ? line : url;
    }
}
