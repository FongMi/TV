package com.fongmi.android.tv.server;

import android.net.Uri;
import android.util.Base64;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.server.process.Action;
import com.fongmi.android.tv.server.process.Local;
import com.fongmi.android.tv.server.process.Process;
import com.fongmi.android.tv.utils.M3U8;
import com.fongmi.android.tv.utils.Sniffer;
import com.github.catvod.Init;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;

public class Nano extends NanoHTTPD {

    private List<Process> process;

    public Nano(int port) {
        super(port);
        addProcess();
    }

    private void addProcess() {
        process = new ArrayList<>();
        process.add(new Action());
        process.add(new Local());
    }

    public static Response success() {
        return success("OK");
    }

    public static Response success(String text) {
        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, text);
    }

    public static Response error(String text) {
        return error(Response.Status.INTERNAL_ERROR, text);
    }

    public static Response error(Response.IStatus status, String text) {
        return newFixedLengthResponse(status, MIME_PLAINTEXT, text);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String url = session.getUri().trim();
        Map<String, String> files = new HashMap<>();
        if (session.getMethod() == Method.POST) parse(session, files);
        if (url.contains("?")) url = url.substring(0, url.indexOf('?'));
        if (url.startsWith("/m3u8")) return m3u8(session);
        if (url.startsWith("/proxy")) return proxy(session.getParms());
        if (url.startsWith("/tvbus")) return success(LiveConfig.getResp());
        if (url.startsWith("/device")) return success(Device.get().toString());
        if (url.startsWith("/license")) return success(new String(Base64.decode(url.substring(9), Base64.DEFAULT)));
        for (Process process : process) if (process.isRequest(session, url)) return process.doResponse(session, url, files);
        return getAssets(url.substring(1));
    }

    private void parse(NanoHTTPD.IHTTPSession session, Map<String, String> files) {
        String ct = session.getHeaders().get("content-type");
        if (ct != null && ct.toLowerCase().contains("multipart/form-data") && !ct.toLowerCase().contains("charset=")) {
            Matcher matcher = Pattern.compile("[ |\t]*(boundary[ |\t]*=[ |\t]*['|\"]?[^\"^'^;^,]*['|\"]?)", Pattern.CASE_INSENSITIVE).matcher(ct);
            String boundary = matcher.find() ? matcher.group(1) : null;
            if (boundary != null) session.getHeaders().put("content-type", "multipart/form-data; charset=utf-8; " + boundary);
        }
        try {
            session.parseBody(files);
        } catch (Exception ignored) {
        }
    }

    private Response m3u8(IHTTPSession session) {
        try {
            String url = session.getParms().get("url");
            String result = M3U8.get(url, session.getHeaders());
            for (String ad : Sniffer.getRegex(Uri.parse(url))) result = result.replaceAll(ad, "");
            return newChunkedResponse(Response.Status.OK, MIME_PLAINTEXT, new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    private Response proxy(Map<String, String> params) {
        try {
            Object[] rs = ApiConfig.get().proxyLocal(params);
            return newChunkedResponse(Response.Status.lookup((Integer) rs[0]), (String) rs[1], (InputStream) rs[2]);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    private Response getAssets(String path) {
        try {
            if (path.isEmpty()) path = "index.html";
            InputStream is = Init.context().getAssets().open(path);
            return newFixedLengthResponse(Response.Status.OK, getMimeTypeForFile(path), is, is.available());
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_HTML, null);
        }
    }
}
