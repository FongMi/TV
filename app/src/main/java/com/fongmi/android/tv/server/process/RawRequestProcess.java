package com.fongmi.android.tv.server.process;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.server.Nano;

import java.io.IOException;
import java.io.InputStream;

import fi.iki.elonen.NanoHTTPD;

public class RawRequestProcess implements RequestProcess {

    private final String mimeType;
    private final String path;
    private final int resId;

    public RawRequestProcess(String path, int resId, String mimeType) {
        this.path = path;
        this.resId = resId;
        this.mimeType = mimeType;
    }

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String path) {
        return session.getMethod() == NanoHTTPD.Method.GET && path.equalsIgnoreCase(this.path);
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String path) {
        try {
            InputStream is = App.get().getResources().openRawResource(resId);
            return Nano.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, mimeType + ";charset=utf-8", is, is.available());
        } catch (IOException e) {
            return Nano.createErrorResponse(e.getMessage());
        }
    }
}