package com.fongmi.android.tv.server;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.server.process.InputRequestProcess;
import com.fongmi.android.tv.server.process.RawRequestProcess;
import com.fongmi.android.tv.server.process.RequestProcess;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;

public class Nano extends NanoHTTPD {

    private List<RequestProcess> processes;
    private Listener listener;

    public Nano(int port) {
        super(port);
        addRequestProcess();
    }

    private void addRequestProcess() {
        processes = new ArrayList<>();
        processes.add(new InputRequestProcess(this));
        processes.add(new RawRequestProcess("/", R.raw.index, NanoHTTPD.MIME_HTML));
        processes.add(new RawRequestProcess("/index.html", R.raw.index, NanoHTTPD.MIME_HTML));
        processes.add(new RawRequestProcess("/ui.css", R.raw.ui, "text/css"));
        processes.add(new RawRequestProcess("/style.css", R.raw.style, "text/css"));
        processes.add(new RawRequestProcess("/jquery.js", R.raw.jquery, "application/x-javascript"));
        processes.add(new RawRequestProcess("/script.js", R.raw.script, "application/x-javascript"));
        processes.add(new RawRequestProcess("/favicon.ico", R.mipmap.ic_launcher, "image/x-icon"));
    }

    public Listener getListener() {
        return listener;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String url = session.getUri().trim();
        if (url.contains("?")) url = url.substring(0, url.indexOf('?'));
        if (session.getMethod() == Method.POST) parseBody(session);
        for (RequestProcess process : processes) {
            if (process.isRequest(session, url)) {
                return process.doResponse(session, url);
            }
        }
        if (session.getMethod() == Method.GET && url.equals("/proxy")) {
            Map<String, String> params = session.getParms();
            if (params.containsKey("do")) {
                Object[] rs = ApiConfig.get().proxyLocal(params);
                try {
                    int code = (int) rs[0];
                    String mime = (String) rs[1];
                    InputStream stream = rs[2] != null ? (InputStream) rs[2] : null;
                    return NanoHTTPD.newChunkedResponse(Response.Status.lookup(code), mime, stream);
                } catch (Exception e) {
                    return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "500");
                }
            }
        }
        return processes.get(0).doResponse(session, "");
    }

    private void parseBody(IHTTPSession session) {
        Map<String, String> files = new HashMap<>();
        try {
            String hd = session.getHeaders().get("content-type");
            if (hd == null) return;
            if (hd.toLowerCase().contains("multipart/form-data") && !hd.toLowerCase().contains("charset=")) {
                Matcher matcher = Pattern.compile("[ |\t]*(boundary[ |\t]*=[ |\t]*['|\"]?[^\"^'^;^,]*['|\"]?)", Pattern.CASE_INSENSITIVE).matcher(hd);
                String boundary = matcher.find() ? matcher.group(1) : null;
                if (boundary != null) session.getHeaders().put("content-type", "multipart/form-data; charset=utf-8; " + boundary);
            }
            session.parseBody(files);
        } catch (Exception ignored) {
        }
    }

    public static Response createPlainTextResponse(Response.IStatus status, String text) {
        return newFixedLengthResponse(status, NanoHTTPD.MIME_PLAINTEXT, text);
    }

    public interface Listener {

        void onSearch(String text);

        void onPush(String url);

        void onApi(String url);
    }
}
