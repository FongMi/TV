package com.github.catvod.utils;

import android.net.Uri;
import android.util.Base64;

import com.google.common.net.HttpHeaders;

import java.net.URI;
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

    public static Context capture(String sourceUrl, Map<String, String> headers, String responseUrl, String authorization) {
        URI source = httpUri(sourceUrl);
        URI response = httpUri(responseUrl);
        if (source == null || response == null || !sameOrigin(source, response)) return null;
        String expected = authorization(headers);
        if (expected != null) return expected.regionMatches(true, 0, "Basic ", 0, 6) && expected.equals(authorization) ? new Context(response, expected, null, null) : null;
        if (source.getUserInfo() != null) return captureCredentials(source, response, authorization);
        String token = Uri.parse(sourceUrl).getQueryParameter("auth");
        return token != null && !token.isEmpty() && token.equals(Uri.parse(responseUrl).getQueryParameter("auth")) ? new Context(response, null, token, null) : null;
    }

    private static Context captureCredentials(URI source, URI response, String authorization) {
        String userInfo = source.getUserInfo();
        if (basic(userInfo).equals(authorization)) return new Context(response, authorization, null, null);
        if (authorization == null || !authorization.regionMatches(true, 0, "Digest ", 0, 7)) return null;
        String username = parseDigest(authorization.substring(7)).get("username");
        return userInfo.split(":", 2)[0].equals(username) ? new Context(response, null, null, source.getRawUserInfo()) : null;
    }

    private static String authorization(Map<String, String> headers) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(header.getKey())) return header.getValue();
        }
        return null;
    }

    private static URI httpUri(String url) {
        if (url == null) return null;
        try {
            URI uri = URI.create(url);
            if (uri.getHost() == null) return null;
            return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()) ? uri : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int port(URI uri) {
        return uri.getPort() != -1 ? uri.getPort() : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static boolean sameOrigin(URI first, URI second) {
        return first.getScheme().equalsIgnoreCase(second.getScheme()) && first.getHost().equalsIgnoreCase(second.getHost()) && port(first) == port(second);
    }

    public static final class Context {

        public static final Context NONE = new Context(null, null, null, null);

        private final URI origin;
        private final String authorization;
        private final String token;
        private final String userInfo;

        private Context(URI origin, String authorization, String token, String userInfo) {
            this.origin = origin;
            this.authorization = authorization;
            this.token = token;
            this.userInfo = userInfo;
        }

        public String getUrl(String url, Map<String, String> headers) {
            if ((token == null && userInfo == null) || !accepts(url, headers)) return url;
            Uri uri = Uri.parse(url);
            if (token != null) return uri.buildUpon().appendQueryParameter("auth", token).build().toString();
            return uri.buildUpon().encodedAuthority(userInfo + "@" + uri.getEncodedAuthority()).build().toString();
        }

        public Map<String, String> getHeaders(String url, Map<String, String> headers) {
            return authorization != null && accepts(url, headers) ? Map.of(HttpHeaders.AUTHORIZATION, authorization) : Map.of();
        }

        private boolean accepts(String url, Map<String, String> headers) {
            if (origin == null || authorization(headers) != null) return false;
            URI target = httpUri(url);
            return target != null && target.getUserInfo() == null && sameOrigin(origin, target) && Uri.parse(url).getQueryParameter("auth") == null;
        }
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
