package com.fongmi.nodejs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import fi.iki.elonen.NanoHTTPD;

final class NodeBridge extends NanoHTTPD {

    static final long MAX_MESSAGE_BYTES = 4L * 1024 * 1024;
    private static final String MIME_JSON = "application/json; charset=utf-8";
    private static final String HEADER_TOKEN = "x-catvod-token";
    private final Function<String, String> handler;
    private volatile String token;

    NodeBridge(Function<String, String> handler) {
        super("127.0.0.1", 0);
        this.handler = handler;
    }

    static boolean isBodyTooLarge(String body) {
        return body == null || body.getBytes(StandardCharsets.UTF_8).length > MAX_MESSAGE_BYTES;
    }

    static boolean isAuthorized(Map<String, String> headers, String expected) {
        if (expected == null || expected.isEmpty() || headers == null) return false;
        String actual = headers.get(HEADER_TOKEN);
        if (actual == null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (HEADER_TOKEN.equalsIgnoreCase(entry.getKey())) {
                    actual = entry.getValue();
                    break;
                }
            }
        }
        if (actual == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    void open() throws IOException {
        start(1000, false);
        getListeningPort();
    }

    void setToken(String token) {
        this.token = token;
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (session.getMethod() != Method.POST || !"/msg".equals(session.getUri())) return response(Response.Status.NOT_FOUND, "{}");
        if (!isAuthorized(session.getHeaders(), token)) return response(Response.Status.FORBIDDEN, "{\"success\":false,\"code\":-1}");
        if (isTooLarge(session)) return response(Response.Status.PAYLOAD_TOO_LARGE, "{\"success\":false,\"code\":-1}");
        Map<String, String> files = new HashMap<>();
        try {
            session.getHeaders().put("content-type", MIME_JSON);
            session.parseBody(files);
            String body = files.get("postData");
            if (isBodyTooLarge(body)) return response(Response.Status.PAYLOAD_TOO_LARGE, "{\"success\":false,\"code\":-1}");
            return response(Response.Status.OK, handler.apply(body));
        } catch (Exception e) {
            return response(Response.Status.BAD_REQUEST, "{\"success\":false,\"code\":-1}");
        }
    }

    private Response response(Response.Status status, String body) {
        return newFixedLengthResponse(status, MIME_JSON, body);
    }

    private boolean isTooLarge(IHTTPSession session) {
        String value = session.getHeaders().get("content-length");
        if (value == null) return false;
        try {
            return Long.parseLong(value) > MAX_MESSAGE_BYTES;
        } catch (NumberFormatException e) {
            return true;
        }
    }
}
