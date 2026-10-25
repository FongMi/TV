package com.github.catvod.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

final class NetRequest {

    private static final Set<String> METHODS = Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private static final Set<String> POST_TYPES = Set.of("json", "form", "form-data");

    static Request build(String url, NetOptions options, boolean socket) {
        Headers headers = Headers.of(options.getHeader());
        Request.Builder request = new Request.Builder().url(url).headers(headers);
        validateRequestBody(options);
        if (!socket) {
            String method = requestMethod(options);
            request.method(method, requestBody(method, options, headers));
        }
        return withParams(request.build(), options.getParams());
    }

    private static void validateRequestBody(NetOptions options) {
        if (options.getData() != null && options.getRawBody() != null) throw new IllegalArgumentException("data and body are mutually exclusive");
    }

    private static String requestMethod(NetOptions options) {
        String method = options.getMethod().toUpperCase(Locale.ROOT);
        if (method.equals("HEADER")) method = "HEAD";
        if (!METHODS.contains(method)) throw new IllegalArgumentException("Unsupported method");
        return method;
    }

    private static RequestBody requestBody(String method, NetOptions options, Headers headers) {
        JsonElement data = options.getData();
        JsonElement raw = options.getRawBody();
        String postType = options.getPostType();
        if (!POST_TYPES.contains(postType)) throw new IllegalArgumentException("Unsupported postType");
        boolean required = method.equals("POST") || method.equals("PUT") || method.equals("PATCH");
        boolean supplied = data != null || raw != null;
        boolean accepts = !method.equals("GET") && !method.equals("HEAD");
        if (!accepts && supplied) throw new IllegalArgumentException("Method does not accept a body");
        if (data != null && !postType.equals("json") && !data.isJsonObject()) throw new IllegalArgumentException("Form data must be an object");
        if (raw != null && headers.get("Content-Type") == null) throw new IllegalArgumentException("Raw body requires Content-Type");
        if (!accepts || !required && !supplied) return null;
        return encodeBody(options, headers.get("Content-Type"));
    }

    private static Request withParams(Request request, JsonElement params) {
        if (params == null || params.isJsonNull()) return request;
        if (!params.isJsonObject()) throw new IllegalArgumentException("params must be an object");
        HttpUrl.Builder url = request.url().newBuilder();
        for (Map.Entry<String, JsonElement> entry : params.getAsJsonObject().entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonArray()) for (JsonElement item : value.getAsJsonArray()) addParam(url, entry.getKey(), item);
            else addParam(url, entry.getKey(), value);
        }
        return request.newBuilder().url(url.build()).build();
    }

    private static void addParam(HttpUrl.Builder url, String key, JsonElement value) {
        if (value.isJsonNull()) return;
        if (!value.isJsonPrimitive()) throw new IllegalArgumentException("Query values must be scalar");
        url.addQueryParameter(key, value.getAsString());
    }

    private static RequestBody encodeBody(NetOptions options, String contentType) {
        JsonElement data = options.getData();
        JsonElement raw = options.getRawBody();
        if (data == null) return raw == null ? RequestBody.create(new byte[0]) : rawBody(raw, MediaType.get(contentType));
        return switch (options.getPostType()) {
            case "json" -> RequestBody.create(data.toString(), MediaType.get("application/json; charset=utf-8"));
            case "form" -> {
                FormBody.Builder body = new FormBody.Builder();
                NetOptions.toMap(data).forEach(body::add);
                yield body.build();
            }
            case "form-data" -> {
                MultipartBody.Builder body = new MultipartBody.Builder().setType(MultipartBody.FORM);
                NetOptions.toMap(data).forEach(body::addFormDataPart);
                yield body.build();
            }
            default -> throw new IllegalArgumentException("Unsupported postType");
        };
    }

    private static RequestBody rawBody(JsonElement raw, MediaType type) {
        if (raw.isJsonPrimitive()) return RequestBody.create(raw.getAsString(), type);
        if (!raw.isJsonArray()) throw new IllegalArgumentException("Raw body must be text or bytes");
        JsonArray values = raw.getAsJsonArray();
        byte[] bytes = new byte[values.size()];
        for (int i = 0; i < values.size(); i++) bytes[i] = rawByte(values.get(i));
        return RequestBody.create(bytes, type);
    }

    private static byte rawByte(JsonElement item) {
        if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Raw body bytes must be integers from 0 to 255");
        int value;
        try {
            value = item.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException error) {
            throw new IllegalArgumentException("Raw body bytes must be integers from 0 to 255", error);
        }
        if (value < 0 || value > 255) throw new IllegalArgumentException("Raw body bytes must be integers from 0 to 255");
        return (byte) value;
    }
}
