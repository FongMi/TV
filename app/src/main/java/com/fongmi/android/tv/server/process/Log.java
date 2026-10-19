package com.fongmi.android.tv.server.process;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

import com.fongmi.android.tv.server.LogSession;
import com.fongmi.android.tv.server.impl.Process;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import okhttp3.HttpUrl;

public class Log implements Process {

    private final LogSession logs;

    public Log() {
        this(LogSession.get());
    }

    public Log(LogSession logs) {
        this.logs = logs;
    }

    @Override
    public boolean isRequest(IHTTPSession session, String url) {
        return url.equals("/log");
    }

    @Override
    public Response doResponse(IHTTPSession session, String url, Map<String, String> files) {
        if (session.getMethod() == Method.POST) return connect(session.getHeaders());
        if (session.getMethod() != Method.GET && session.getMethod() != Method.DELETE) return response(Status.METHOD_NOT_ALLOWED, "GET, POST or DELETE required");
        try {
            if (session.getMethod() == Method.DELETE) return stop(session.getHeaders());
            return readLog(session.getHeaders().get("x-log-code"), session.getParms().get("after"));
        } catch (IllegalStateException e) {
            return response(Status.GONE, e.getMessage());
        } catch (SecurityException e) {
            return response(Status.FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            return response(Status.BAD_REQUEST, e.getMessage());
        }
    }

    private Response readLog(String code, String cursor) {
        LogSession.Snapshot snapshot = logs.read(code, cursor);
        JsonArray lines = new JsonArray();
        for (LogSession.Line line : snapshot.lines()) lines.add(lineInfo(line));
        JsonObject body = new JsonObject();
        body.add("lines", lines);
        body.addProperty("next", snapshot.next());
        body.addProperty("backlog", snapshot.backlog());
        body.addProperty("gap", snapshot.gap());
        body.addProperty("remainingMillis", snapshot.remainingMillis());
        return response(Status.OK, body.toString());
    }

    private JsonObject lineInfo(LogSession.Line line) {
        JsonObject item = new JsonObject();
        item.addProperty("seq", line.seq());
        item.addProperty("text", line.text());
        return item;
    }

    private Response connect(Map<String, String> headers) {
        String type = headers.get("content-type");
        if (type == null || !type.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) return response(Status.UNSUPPORTED_MEDIA_TYPE, "application/json required");
        if (!isSameOrigin(headers)) return response(Status.FORBIDDEN, "Same origin required");
        synchronized (logs) {
            logs.start();
            String code = logs.getCode();
            if (code.isEmpty()) return response(Status.SERVICE_UNAVAILABLE, logs.getError());
            JsonObject body = new JsonObject();
            body.addProperty("code", code);
            return response(Status.OK, body.toString());
        }
    }

    private Response stop(Map<String, String> headers) {
        if (!isSameOrigin(headers)) return response(Status.FORBIDDEN, "Same origin required");
        logs.stop(headers.get("x-log-code"));
        return response(Status.OK, "{}");
    }

    private boolean isSameOrigin(Map<String, String> headers) {
        String origin = headers.get("origin");
        if (origin == null) return true;
        HttpUrl source = HttpUrl.parse(origin);
        String host = headers.get("host");
        HttpUrl target = host == null ? null : HttpUrl.parse("http://" + host);
        return source != null && target != null && source.equals(target);
    }

    private Response response(Status status, String text) {
        Response response = newFixedLengthResponse(status, status == Status.OK ? "application/json; charset=utf-8" : "text/plain; charset=utf-8", text);
        response.addHeader("Cache-Control", "no-store");
        return response;
    }
}
