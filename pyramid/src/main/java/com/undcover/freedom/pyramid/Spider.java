package com.undcover.freedom.pyramid;

import android.content.Context;
import android.text.TextUtils;

import androidx.collection.ArrayMap;

import com.chaquo.python.PyObject;
import com.github.catvod.net.OkHttp;
import com.google.gson.Gson;

import org.json.JSONException;
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
    public String searchContent(String key, boolean quick, String pg) {
        return app.callAttr("searchContent", obj, key, quick, pg).toString();
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        return app.callAttr("playerContent", obj, flag, id, gson.toJson(vipFlags)).toString();
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
    public Object[] proxyLocal(Map<String, String> params) throws Exception {
        List<PyObject> list = app.callAttr("localProxy", obj, gson.toJson(params)).asList();
        int code = list.get(0).toInt();
        String type = list.get(1).toString();
        String action = list.get(2).toString();
        String content = list.get(3).toString();
        JSONObject object = new JSONObject(action);
        String url = object.optString("url");
        Headers header = getHeader(object.optString("header"));
        ArrayMap<String, String> param = getParam(object.optString("param"));
        if (object.optString("type").equals("stream")) {
            return new Object[]{code, type, OkHttp.newCall(url, param, header).execute().body().byteStream()};
        } else {
            if (content.isEmpty()) content = OkHttp.newCall(url, header).execute().body().string();
            return new Object[]{code, type, new ByteArrayInputStream(content.getBytes())};
        }
    }

    private Headers getHeader(String header) throws JSONException {
        Headers.Builder builder = new Headers.Builder();
        if (TextUtils.isEmpty(header)) return builder.build();
        JSONObject object = new JSONObject(header);
        for (Iterator<String> iterator = object.keys(); iterator.hasNext(); ) {
            String key = iterator.next();
            builder.add(key, object.optString(key));
        }
        return builder.build();
    }

    private ArrayMap<String, String> getParam(String param) throws JSONException {
        ArrayMap<String, String> params = new ArrayMap<>();
        if (TextUtils.isEmpty(param)) return params;
        JSONObject object = new JSONObject(param);
        for (Iterator<String> iterator = object.keys(); iterator.hasNext(); ) {
            String key = iterator.next();
            params.put(key, object.optString(key));
        }
        return params;
    }
}
