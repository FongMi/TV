package com.github.catvod.utils;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Request;

public class Auth {

    private static final Pattern DIGEST = Pattern.compile("(\\w+)=(?:\"([^\"]*)\"|([^,\\s\"]+))");

    public static String basic(String userInfo) {
        if (!userInfo.contains(":")) userInfo += ":";
        return "Basic " + Base64.encodeToString(userInfo.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    public static String digest(String userInfo, String header, Request request) {
        Map<String, String> params = parseDigest(header.substring(7));
        String[] credentials = userInfo.split(":", 2);
        String username = credentials[0];
        String password = credentials.length > 1 ? credentials[1] : "";
        String realm = params.getOrDefault("realm", "");
        String nonce = params.getOrDefault("nonce", "");
        String opaque = params.get("opaque");
        String uri = digestUri(request);
        String qop = selectQop(params.get("qop"));
        String nc = "00000001";
        String cnonce = newCnonce();
        String ha1 = Crypto.md5(username + ":" + realm + ":" + password);
        String ha2 = Crypto.md5(request.method() + ":" + uri);
        String response = digestResponse(ha1, ha2, nonce, nc, cnonce, qop);
        return buildHeader(username, realm, nonce, uri, nc, cnonce, qop, response, opaque);
    }

    private static String digestUri(Request request) {
        String query = request.url().encodedQuery();
        String path = request.url().encodedPath();
        return query != null ? path + "?" + query : path;
    }

    private static String digestResponse(String ha1, String ha2, String nonce, String nc, String cnonce, String qop) {
        return qop.isEmpty() ? Crypto.md5(ha1 + ":" + nonce + ":" + ha2) : Crypto.md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + ha2);
    }

    private static String buildHeader(String username, String realm, String nonce, String uri, String nc, String cnonce, String qop, String response, String opaque) {
        List<String> fields = new ArrayList<>();
        fields.add("username=\"" + username + "\"");
        fields.add("realm=\"" + realm + "\"");
        fields.add("nonce=\"" + nonce + "\"");
        fields.add("uri=\"" + uri + "\"");
        boolean hasQop = !qop.isEmpty();
        if (hasQop) fields.add("cnonce=\"" + cnonce + "\"");
        if (hasQop) fields.add("nc=" + nc);
        if (hasQop) fields.add("qop=" + qop);
        fields.add("response=\"" + response + "\"");
        if (opaque != null) fields.add("opaque=\"" + opaque + "\"");
        return "Digest " + String.join(", ", fields);
    }

    private static String newCnonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String selectQop(String qop) {
        if (qop == null || qop.isEmpty()) return "";
        for (String option : qop.split(",")) if ("auth".equalsIgnoreCase(option.trim())) return "auth";
        return "";
    }

    private static Map<String, String> parseDigest(String header) {
        Map<String, String> params = new HashMap<>();
        Matcher matcher = DIGEST.matcher(header.trim());
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            if (value != null) params.put(key, value.trim());
        }
        return params;
    }
}
