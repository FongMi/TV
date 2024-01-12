package com.undcover.freedom.pyramid;

import android.content.Context;

import androidx.collection.ArrayMap;

import com.chaquo.python.PyObject;
import com.github.catvod.Proxy;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
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
        return app.callAttr("searchContentPage", obj, key, quick, pg).toString();
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        return replaceProxy(app.callAttr("playerContent", obj, flag, id, gson.toJson(vipFlags)).toString());
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
        JsonObject action = JsonParser.parseString(list.get(2).toString()).getAsJsonObject();
        Map<String, String> headers = Json.toMap(action.get("header"));
        String url = action.get("url").getAsString();
        String content = list.get(3).toString();
        String type = list.get(1).toString();
        int code = list.get(0).toInt();
        if (action.get("type").getAsString().equals("redirect")) {
            NanoHTTPD.Response response = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.lookup(code), NanoHTTPD.MIME_HTML, "");
            for (Map.Entry<String, String> entry : headers.entrySet()) response.addHeader(entry.getKey(), entry.getValue());
            response.addHeader(HttpHeaders.LOCATION, url);
            return new Object[]{response};
        } else if (action.get("type").getAsString().equals("stream")) {
            ArrayMap<String, String> param = Json.toArrayMap(action.get("param"));
            return new Object[]{code, type, OkHttp.newCall(url, Headers.of(headers), param).execute().body().byteStream()};
        } else {
            if (content.isEmpty()) content = OkHttp.newCall(url, Headers.of(headers)).execute().body().string();
            return new Object[]{code, type, new ByteArrayInputStream(replaceProxy(content).getBytes())};
        }
    }

    private String replaceProxy(String content) {
        return content.replace("http://127.0.0.1:UndCover/proxy", Proxy.getUrl(true));
    }
}
