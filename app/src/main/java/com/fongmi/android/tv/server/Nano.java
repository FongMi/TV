package com.fongmi.android.tv.server;

import com.fongmi.android.tv.api.ApiConfig;

import java.io.InputStream;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class Nano extends NanoHTTPD {

    private Listener mListener;

    public Nano() {
        super(9978);
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (session.getUri().isEmpty()) return super.serve(session);
        String url = session.getUri().trim();
        if (url.indexOf('?') >= 0) url = url.substring(0, url.indexOf('?'));
        if (session.getMethod() == Method.GET) {
            if (url.equals("/proxy")) {
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
        }
        return super.serve(session);
    }

    public Listener getListener() {
        return mListener;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public interface Listener {

        void onTextReceived(String text);

        void onApiReceived(String url);

        void onPushReceived(String url);
    }
}
