package com.fongmi.chaquo;

import android.content.Context;

import androidx.annotation.Keep;

import com.chaquo.python.PyObject;
import com.github.catvod.net.Net;
import com.github.catvod.utils.Local;
import com.github.catvod.utils.Util;
import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Spider extends com.github.catvod.crawler.Spider {

    private static final Gson GSON = new Gson();

    private final PyObject app;
    private final PyObject obj;
    private final Net.Cookies cookies;
    private final UserAgent userAgent;

    public Spider(PyObject app, PyObject obj, String api) {
        this(app, obj, api, null);
    }

    public Spider(PyObject app, PyObject obj, String api, Net.Cookies cookies) {
        this(app, obj, api, cookies, null);
    }

    public Spider(PyObject app, PyObject obj, String api, Net.Cookies cookies, Supplier<String> userAgent) {
        this(app, obj, api, cookies, userAgent, null);
    }

    public Spider(PyObject app, PyObject obj, String api, Net.Cookies cookies, Supplier<String> userAgent, Net net) {
        this.cookies = cookies;
        this.userAgent = new UserAgent(userAgent);
        this.app = app;
        this.obj = obj;
        this.net = net;
    }

    @Override
    public void init(Context context, String extend) {
        if (net == null) net = new Net(cookies, siteKey);
        local = new Local(siteKey);
        try {
            obj.put("siteKey", siteKey);
            app.callAttr("bindNet", obj, net, userAgent, local);
            app.callAttr("init", obj, extend);
        } catch (RuntimeException | Error e) {
            releaseSpider(e);
            throw e;
        }
    }

    private void releaseSpider(Throwable failure) {
        try {
            net.close();
        } catch (RuntimeException | Error error) {
            if (error != failure) failure.addSuppressed(error);
        }
        try {
            app.callAttr("release", obj);
        } catch (RuntimeException | Error error) {
            if (error != failure) failure.addSuppressed(error);
        }
    }

    @Override
    public String homeContent(boolean filter) {
        return app.callAttr("homeContent", obj, filter).toString();
    }

    @Override
    public String homeVideoContent() {
        return app.callAttr("homeVideoContent", obj).toString();
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        return app.callAttr("categoryContent", obj, tid, pg, filter, GSON.toJson(extend)).toString();
    }

    @Override
    public String detailContent(List<String> ids) {
        return app.callAttr("detailContent", obj, GSON.toJson(ids)).toString();
    }

    @Override
    public String searchContent(String key, boolean quick) {
        return app.callAttr("searchContent", obj, key, quick).toString();
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) {
        return app.callAttr("searchContent", obj, key, quick, pg).toString();
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        return app.callAttr("playerContent", obj, flag, id, GSON.toJson(vipFlags)).toString();
    }

    @Override
    public String liveContent(String url) {
        return app.callAttr("liveContent", obj, url).toString();
    }

    @Override
    public boolean manualVideoCheck() {
        return app.callAttr("manualVideoCheck", obj).toBoolean();
    }

    @Override
    public boolean isVideoFormat(String url) {
        return app.callAttr("isVideoFormat", obj, url).toBoolean();
    }

    @Override
    public Object[] proxy(Map<String, String> params) throws Exception {
        List<PyObject> list = app.callAttr("localProxy", obj, GSON.toJson(params)).asList();
        boolean base64 = list.size() > 4 && list.get(4).toInt() == 1;
        boolean header = list.size() > 3 && list.get(3) != null;
        return new Object[]{list.get(0).toInt(), list.get(1).toString(), getStream(list.get(2), base64), header ? getHeader(list.get(3)) : null};
    }

    @Override
    public String action(String action) {
        return app.callAttr("action", obj, action).toString();
    }

    @Override
    public void destroy() {
        Throwable failure = null;
        try {
            if (net != null) net.close();
        } catch (RuntimeException | Error error) {
            failure = error;
        }
        destroyPython(failure);
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private void destroyPython(Throwable failure) {
        try {
            app.callAttr("destroy", obj);
        } catch (Exception error) {
            if (failure != null && failure != error) failure.addSuppressed(error);
        } catch (Error error) {
            if (failure == null) throw error;
            if (failure != error) failure.addSuppressed(error);
        }
    }

    private Map<String, String> getHeader(PyObject obj) {
        try {
            Map<String, String> header = new HashMap<>();
            for (Map.Entry<PyObject, PyObject> entry : obj.asMap().entrySet()) header.put(entry.getKey().toString(), entry.getValue().toString());
            return header;
        } catch (Exception e) {
            return null;
        }
    }

    private ByteArrayInputStream getStream(PyObject o, boolean base64) {
        if (o == null) return null;
        if (o.type().toString().contains("bytes")) return new ByteArrayInputStream(o.toJava(byte[].class));
        String content = o.toString();
        if (base64 && content.contains("base64,")) content = content.split("base64,")[1];
        return new ByteArrayInputStream(base64 ? Util.decode(content) : content.getBytes());
    }

    public static final class UserAgent {

        private final Supplier<String> supplier;

        private UserAgent(Supplier<String> supplier) {
            this.supplier = supplier;
        }

        @Keep
        public String getUserAgent() {
            return supplier == null ? "" : supplier.get();
        }
    }
}
