package com.github.catvod.utils;

import android.util.Base64;

import com.github.catvod.net.OkHttp;
import com.google.common.net.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

public class Decoder {

    private static final Pattern JS_URI = Pattern.compile("\"(\\.|\\.\\.)/(.?|.+?)\\.js\\?(.?|.+?)\"");
    private static final Pattern BASE64_PREFIX = Pattern.compile("[A-Za-z0-9]{8}\\*\\*");

    public static String getJson(String url, String tag) throws Exception {
        return load(url, tag).text();
    }

    public static Content load(String url, String tag) throws Exception {
        return load(url, tag, null);
    }

    public static Content load(String url, String tag, Auth.Context auth) throws Exception {
        Request request = new Request.Builder().url(url).tag(tag).tag(Auth.Context.class, auth).build();
        try (Response res = OkHttp.client().newCall(request).execute()) {
            HttpUrl httpUrl = res.request().url();
            int size = HttpUrl.parse(url).querySize();
            if (httpUrl.querySize() == size) url = httpUrl.toString();
            String text = verify(url, res.body().string());
            String authorization = res.isSuccessful() ? res.request().header(HttpHeaders.AUTHORIZATION) : null;
            return new Content(text, httpUrl.toString(), authorization, res.isSuccessful());
        }
    }

    public record Content(String text, String url, String authorization, boolean successful) {
    }

    private static String verify(String url, String data) throws Exception {
        if (data.isEmpty()) throw new Exception();
        if (Json.isObj(data)) return fix(url, data);
        if (data.contains("**")) data = base64(data);
        if (data.startsWith("2423")) data = cbc(data.replaceAll("\\s+", ""));
        return fix(url, data);
    }

    private static String fix(String url, String data) {
        Matcher matcher = JS_URI.matcher(data);
        while (matcher.find()) data = replace(url, data, matcher.group());
        if (data.contains("../")) data = data.replace("../", UriUtil.resolve(url, "../"));
        if (data.contains("./")) data = data.replace("./", UriUtil.resolve(url, "./"));
        if (data.contains("__JS1__")) data = data.replace("__JS1__", "./");
        if (data.contains("__JS2__")) data = data.replace("__JS2__", "../");
        return data;
    }

    private static String replace(String url, String data, String ext) {
        String t = ext.replace("\"./", "\"" + UriUtil.resolve(url, "./"));
        t = t.replace("\"../", "\"" + UriUtil.resolve(url, "../"));
        t = t.replace("./", "__JS1__").replace("../", "__JS2__");
        return data.replace(ext, t);
    }

    private static String cbc(String data) throws Exception {
        String decode = new String(Util.hex2byte(data), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        String key = padEnd(decode.substring(decode.indexOf("$#") + 2, decode.indexOf("#$")));
        String iv = padEnd(decode.substring(decode.length() - 13));
        data = data.substring(data.indexOf("2324") + 4, data.length() - 26);
        byte[] decrypted = Crypto.decryptAesCbc(Util.hex2byte(data), key.getBytes(StandardCharsets.UTF_8), iv.getBytes(StandardCharsets.UTF_8));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static String base64(String data) {
        Matcher matcher = BASE64_PREFIX.matcher(data);
        if (!matcher.find() || matcher.end() == data.length()) return data;
        String encoded = data.substring(matcher.end());
        return new String(Base64.decode(encoded, Base64.DEFAULT), StandardCharsets.UTF_8);
    }

    private static String padEnd(String key) {
        return key + "0000000000000000".substring(key.length());
    }
}
