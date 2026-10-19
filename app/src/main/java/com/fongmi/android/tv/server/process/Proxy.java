package com.fongmi.android.tv.server.process;

import com.fongmi.android.tv.api.loader.BaseLoader;
import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.server.impl.Process;
import com.fongmi.android.tv.utils.Notify;
import com.github.catvod.utils.RuntimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class Proxy implements Process {

    private static final String INVALID_RESPONSE = "Invalid proxy response";

    @Override
    public boolean isRequest(IHTTPSession session, String url) {
        return url.startsWith("/proxy");
    }

    @Override
    public Response doResponse(IHTTPSession session, String url, Map<String, String> files) {
        try {
            Map<String, String> params = session.getParms();
            params.putAll(session.getHeaders());
            params.putAll(files);
            return createResponse(BaseLoader.get().proxy(params));
        } catch (Throwable e) {
            RuntimeModule.MissingException missing = RuntimeModule.MissingException.find(e);
            if (missing != null) return markError(Nano.error(Status.SERVICE_UNAVAILABLE, Notify.getError(missing)));
            e.printStackTrace();
            return markError(Nano.error(Objects.toString(e.getMessage(), e.toString())));
        }
    }

    static Response createResponse(Object[] rs) {
        if (rs == null || rs.length == 0) return markError(Nano.error(INVALID_RESPONSE));
        if (rs[0] instanceof Response response) return markError(response);
        if (rs.length < 3) return markError(Nano.error(INVALID_RESPONSE));
        InputStream stream = (InputStream) rs[2];
        try {
            Response response = NanoHTTPD.newChunkedResponse(toStatus((Integer) rs[0]), (String) rs[1], stream);
            addHeaders(response, rs.length > 3 ? rs[3] : null);
            return markError(response);
        } catch (RuntimeException | Error error) {
            closeFailedStream(stream, error);
            throw error;
        }
    }

    private static void addHeaders(Response response, Object headers) {
        if (headers == null) return;
        for (Map.Entry<String, String> entry : ((Map<String, String>) headers).entrySet()) {
            String name = entry.getKey();
            if (!name.equalsIgnoreCase("Content-Length") && !name.equalsIgnoreCase("Transfer-Encoding")) response.addHeader(name, entry.getValue());
        }
    }

    private static void closeFailedStream(InputStream stream, Throwable error) {
        if (stream == null) return;
        try {
            stream.close();
        } catch (IOException | RuntimeException | Error failure) {
            if (failure != error) error.addSuppressed(failure);
        }
    }

    private static Response markError(Response response) {
        if (response.getStatus().getRequestStatus() >= 400) response.addHeader("X-TV-Proxy-Error", "1");
        return response;
    }

    private static IStatus toStatus(int code) {
        Status status = Status.lookup(code);
        return status != null ? status : code >= 100 && code <= 599 ? new ProxyStatus(code) : Status.INTERNAL_ERROR;
    }

    private record ProxyStatus(int code) implements IStatus {

        @Override
        public String getDescription() {
            return code + " Proxy Status";
        }

        @Override
        public int getRequestStatus() {
            return code;
        }
    }
}
