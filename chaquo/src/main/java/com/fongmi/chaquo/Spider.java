package com.fongmi.chaquo;

import android.content.Context;

import androidx.annotation.Keep;

import com.chaquo.python.PyObject;
import com.github.catvod.net.Net;
import com.github.catvod.utils.Local;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Spider extends com.github.catvod.crawler.Spider {

    private static final Gson GSON = new Gson();

    private final PyObject app;
    private final PyObject obj;
    private final Net.Cookies cookies;
    private final UserAgent userAgent;
    private final Streams streams = new Streams();

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
            app.callAttr("bindNet", obj, net, userAgent, local, streams, js);
            app.callAttr("init", obj, extend);
        } catch (RuntimeException | Error e) {
            releaseSpider(e);
            throw e;
        }
    }

    private void releaseSpider(Throwable failure) {
        closeResources(failure);
        try {
            app.callAttr("release", obj);
        } catch (RuntimeException | Error error) {
            if (error != failure) failure.addSuppressed(error);
        }
    }

    private Throwable closeResources(Throwable failure) {
        try {
            js.close();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (error != failure) failure.addSuppressed(error);
        }
        try {
            streams.close();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (error != failure) failure.addSuppressed(error);
        }
        try {
            if (net != null) net.close();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (error != failure) failure.addSuppressed(error);
        }
        return failure;
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
        List<PyObject> list = app.callAttr("proxy", obj, GSON.toJson(params)).asList();
        if (list.size() < 3) throw new IllegalArgumentException("Proxy response requires status, MIME and body");
        InputStream stream = proxyBody(list.get(2));
        try {
            boolean header = list.size() > 3 && list.get(3) != null;
            return new Object[]{proxyCode(list.get(0)), proxyMime(list.get(1)), stream, header ? getHeader(list.get(3)) : null};
        } catch (RuntimeException | Error error) {
            closeProxyStream(stream, error);
            throw error;
        }
    }

    @Override
    public String action(String action) {
        return app.callAttr("action", obj, action).toString();
    }

    @Override
    public AutoCloseable danmaku(String id, Consumer<JSONObject> emit) {
        Emitter emitter = new Emitter(emit);
        PyObject stop;
        try {
            stop = app.callAttr("danmaku", obj, id, emitter);
        } catch (RuntimeException | Error error) {
            emitter.close();
            throw error;
        }
        return () -> {
            if (emitter.close()) stop.call();
        };
    }

    @Override
    public void destroy() {
        Throwable failure = destroyPython(closeResources(null));
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private Throwable destroyPython(Throwable failure) {
        try {
            app.callAttr("destroy", obj);
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        return failure;
    }

    private Map<String, String> getHeader(PyObject obj) {
        Map<String, String> header = new HashMap<>();
        for (Map.Entry<PyObject, PyObject> entry : obj.asMap().entrySet()) header.put(headerString(entry.getKey()), headerValue(entry.getValue()));
        return header;
    }

    private String headerString(PyObject value) {
        if (value == null) throw new IllegalArgumentException("Invalid proxy response header");
        return value.toJava(String.class);
    }

    private String headerValue(PyObject value) {
        try {
            return headerString(value);
        } catch (ClassCastException error) {
            String[] values = value.toJava(String[].class);
            for (String part : values) if (part == null) throw new IllegalArgumentException("Invalid proxy response header");
            return String.join(", ", values);
        }
    }

    private void closeProxyStream(InputStream stream, Throwable error) {
        try {
            stream.close();
        } catch (IOException | RuntimeException | Error failure) {
            if (failure != error) error.addSuppressed(failure);
        }
    }

    private int proxyCode(PyObject value) {
        if (value == null) throw new IllegalArgumentException("Proxy response status requires an integer from 100 to 599");
        int code = value.toInt();
        if (code < 100 || code > 599) throw new IllegalArgumentException("Proxy response status requires an integer from 100 to 599");
        return code;
    }

    private String proxyMime(PyObject value) {
        if (value == null) throw new IllegalArgumentException("Proxy response MIME requires a string");
        return value.toJava(String.class);
    }

    private InputStream proxyBody(PyObject body) {
        if (body == null) throw new IllegalArgumentException("Proxy body requires text, bytes or InputStream");
        Object value = body.toJava(Object.class);
        if (value instanceof InputStream stream) return stream;
        if (value instanceof byte[] bytes) return new ByteArrayInputStream(bytes);
        if (value instanceof String text) return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
        throw new IllegalArgumentException("Proxy body requires text, bytes or InputStream");
    }

    @Keep
    public static final class Emitter {

        private final AtomicReference<Consumer<JSONObject>> listener;

        private Emitter(Consumer<JSONObject> listener) {
            this.listener = new AtomicReference<>(listener);
        }

        public void emit(String json) throws JSONException {
            Consumer<JSONObject> current = listener.get();
            if (current != null) current.accept(new JSONObject(json));
        }

        private boolean close() {
            return listener.getAndSet(null) != null;
        }
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
