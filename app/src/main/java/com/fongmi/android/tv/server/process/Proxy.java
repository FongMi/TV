package com.fongmi.android.tv.server.process;

import com.fongmi.android.tv.api.loader.BaseLoader;
import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.server.impl.Process;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.InputStream;
import java.util.Map;

public class Proxy implements Process {

    @Override
    public boolean isRequest(IHTTPSession session, String path) {
        return "/proxy".equals(path);
    }

    @Override
    public Response doResponse(IHTTPSession session, String path, Map<String, String> files) {
        try {
            Map<String, String> params = session.getParms();
            params.putAll(session.getHeaders());
            Object[] rs = BaseLoader.get().proxyLocal(params);
            Response response = Response.newChunkedResponse(Status.lookup((Integer) rs[0]), (String) rs[1], (InputStream) rs[2]);
            if (rs.length > 3 && rs[3] != null) for (Map.Entry<String, String> entry : ((Map<String, String>) rs[3]).entrySet()) response.addHeader(entry.getKey(), entry.getValue());
            return response;
        } catch (Throwable e) {
            return Nano.error(e.getMessage());
        }
    }
}
