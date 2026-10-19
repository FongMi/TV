package com.fongmi.android.tv.server;

import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.server.impl.Process;
import com.fongmi.android.tv.server.process.Action;
import com.fongmi.android.tv.server.process.Cache;
import com.fongmi.android.tv.server.process.Fetch;
import com.fongmi.android.tv.server.process.Image;
import com.fongmi.android.tv.server.process.Local;
import com.fongmi.android.tv.server.process.Log;
import com.fongmi.android.tv.server.process.Media;
import com.fongmi.android.tv.server.process.Parse;
import com.fongmi.android.tv.server.process.Proxy;
import com.fongmi.android.tv.server.process.Remote;
import com.fongmi.android.tv.server.process.SpiderTest;
import com.fongmi.android.tv.server.process.Upload;
import com.github.catvod.utils.Asset;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class Nano extends NanoHTTPD {

    private static final String INDEX = "index.html";

    private final Server owner;
    private List<Process> process;
    private final SpiderTest spiderTest = new SpiderTest();
    private final Fetch fetch = new Fetch();

    public Nano(int port) {
        this(port, null);
    }

    Nano(int port, Server owner) {
        super(port);
        this.owner = owner;
        addProcess();
    }

    private void addProcess() {
        process = new ArrayList<>();
        process.add(new Action());
        process.add(new Cache());
        process.add(new Image());
        process.add(new Local());
        process.add(new Log());
        process.add(spiderTest);
        process.add(new Media());
        process.add(new Parse());
        process.add(new Proxy());
        process.add(new Remote());
        process.add(new Upload());
        process.add(fetch);
    }

    @Override
    public void stop() {
        Throwable failure = closeCrawlerSessions();
        failure = closeFetches(failure);
        failure = stopServer(failure);
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private Throwable closeCrawlerSessions() {
        try {
            spiderTest.close();
            return null;
        } catch (RuntimeException | Error error) {
            return error;
        }
    }

    private Throwable closeFetches(Throwable failure) {
        try {
            fetch.close();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        return failure;
    }

    private Throwable stopServer(Throwable failure) {
        try {
            super.stop();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        return failure;
    }

    public static Response ok() {
        return ok("OK");
    }

    public static Response ok(String text) {
        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, text);
    }

    public static Response error(String text) {
        return error(Response.Status.INTERNAL_ERROR, text);
    }

    public static Response error(Response.Status status, String text) {
        return newFixedLengthResponse(status, MIME_PLAINTEXT, text);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String url = session.getUri().trim();
        Map<String, String> files = new HashMap<>();
        if (session.getMethod() == Method.POST) parse(session, files);
        if (url.startsWith("/tvbus")) return ok(LiveConfig.getResp());
        if (url.startsWith("/device")) return ok(Device.get().toString());
        for (Process process : process) if (process.isRequest(session, url)) return doResponse(process, session, url, files);
        return getAssets(url.substring(1));
    }

    private Response doResponse(Process process, IHTTPSession session, String url, Map<String, String> files) {
        if (!(process instanceof Log) || owner == null) return process.doResponse(session, url, files);
        synchronized (owner) {
            if (owner.isCurrent(this)) return process.doResponse(session, url, files);
            Response response = error(Response.Status.GONE, "Log session ended");
            response.addHeader("Cache-Control", "no-store");
            return response;
        }
    }

    private void parse(IHTTPSession session, Map<String, String> files) {
        try {
            String ct = session.getHeaders().get("content-type");
            if (ct != null) session.getHeaders().put("content-type", ct.replace("multipart/form-data", "multipart/form-data; charset=utf-8"));
            session.parseBody(files);
        } catch (Exception ignored) {
        }
    }

    private Response getAssets(String path) {
        try {
            if (path.isEmpty()) path = INDEX;
            InputStream is = Asset.open(path);
            return newFixedLengthResponse(Response.Status.OK, getMimeTypeForFile(path), is, -1);
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_HTML, null, 0);
        }
    }
}
