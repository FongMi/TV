package com.undcover.freedom.pyramid;

import android.content.Context;
import android.util.ArrayMap;

import com.chaquo.python.PyObject;
import com.github.catvod.net.OkHttp;
import com.github.catvod.spider.Proxy;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.Headers;

public class Spider extends com.github.catvod.crawler.Spider {

    private final PyObject app;
    private final PyObject obj;
    private final Gson gson;

    public Spider(PyObject app, PyObject obj) {
        this.gson = new Gson();
        this.app = app;
        this.obj = obj;
    }

    @Override
    public void init(Context context) {
        app.callAttr("init", obj);
    }

    @Override
    public void init(Context context, String extend) {
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
    public String playerContent(String flag, String id, List<String> vipFlags) {
        return app.callAttr("playerContent", obj, flag, id, gson.toJson(vipFlags)).toString();
    }

    @Override
    public Object[] proxyLocal(Map<?, ?> params) throws Exception {
        List<PyObject> list = app.callAttr("localProxy", params).asList();
        int code = list.get(0).toInt();
        String type = list.get(1).toString();
        String action = list.get(2).toString();
        String content = list.get(3).toString();
        JSONObject object = new JSONObject(action);
        String url = object.optString("url");
        Headers header = getHeader(object.getJSONObject("header"));
        ArrayMap<String, String> param = getParam(object.getJSONObject("param"));
        if (object.optString("type").equals("stream")) {
            return new Object[]{code, type, OkHttp.newCall(url, param, header).execute().body().byteStream()};
        } else {
            content = replaceUrl(content.isEmpty() ? OkHttp.newCall(url, header).execute().body().string() : content);
            return new Object[]{code, type, new ByteArrayInputStream(content.getBytes())};
        }
    }

    private Headers getHeader(JSONObject object) {
        Headers.Builder builder = new Headers.Builder();
        if (object == null) return builder.build();
        for (Iterator<String> iterator = object.keys(); iterator.hasNext(); ) {
            String key = iterator.next();
            builder.add(key, object.optString(key));
        }
        return builder.build();
    }

    private ArrayMap<String, String> getParam(JSONObject object) {
        ArrayMap<String, String> params = new ArrayMap<>();
        if (object == null) return params;
        for (Iterator<String> iterator = object.keys(); iterator.hasNext(); ) {
            String key = iterator.next();
            params.put(key, object.optString(key));
        }
        return params;
    }

    private String replaceUrl(String content) {
        return content.replace("http://127.0.0.1:UndCover/proxy", Proxy.getUrl());
    }
}
