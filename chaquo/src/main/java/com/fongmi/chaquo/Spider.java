package com.fongmi.chaquo;

import android.content.Context;

import com.chaquo.python.PyObject;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.UriUtil;
import com.github.catvod.utils.Util;
import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Spider extends com.github.catvod.crawler.Spider {

    private final PyObject app;
    private final PyObject obj;
    private final String api;
    private final Gson gson;

    public Spider(PyObject app, PyObject obj, String api) {
        this.gson = new Gson();
        this.app = app;
        this.obj = obj;
        this.api = api;
    }

    @Override
    public void init(Context context, String extend) {
        PyObject dependence = app.callAttr("getDependence", obj);
        if (dependence != null) for (PyObject item : dependence.asList()) download(item + ".py");
        obj.put("siteKey", siteKey);
        app.callAttr("init", obj, extend);
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
        return app.callAttr("categoryContent", obj, tid, pg, filter, gson.toJson(extend)).toString();
    }

    @Override
    public String detailContent(List<String> ids) {
        return app.callAttr("detailContent", obj, gson.toJson(ids)).toString();
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
        return app.callAttr("playerContent", obj, flag, id, gson.toJson(vipFlags)).toString();
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
        List<PyObject> list = app.callAttr("localProxy", obj, gson.toJson(params)).asList();
        boolean base64 = list.size() > 4 && list.get(4).toInt() == 1;
        boolean header = list.size() > 3 && list.get(3) != null;
        Object[] result = new Object[4];
        result[0] = list.get(0).toInt();
        result[1] = list.get(1).toString();
        result[2] = getStream(list.get(2), base64);
        result[3] = header ? getHeader(list.get(3)) : null;
        return result;
    }

    @Override
    public String action(String action) {
        return app.callAttr("action", obj, action).toString();
    }

    @Override
    public void onPlayback(String event, String vodId, int episodeIndex, String episodeUrl, long position, long duration) {
        try {
            app.callAttr("onPlayback", obj, event, vodId, episodeIndex, episodeUrl, position, duration);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void destroy() {
        try {
            app.callAttr("destroy", obj);
        } catch (Exception ignored) {
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

    private void download(String name) {
        String path = Path.py(name).getAbsolutePath();
        String url = UriUtil.resolve(api, name);
        app.callAttr("download", path, url);
    }
}
