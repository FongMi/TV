package com.github.catvod.crawler;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import com.github.catvod.Proxy;
import com.github.catvod.Runtime;
import com.github.catvod.net.Net;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Dialog;
import com.github.catvod.utils.Js;
import com.github.catvod.utils.Local;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import okhttp3.Dns;

public abstract class Spider {

    public String siteKey;
    public Net net;
    public Local local;
    public final Js js = new Js();
    private final Dialog dialog = new Dialog();

    public JSONObject dialog(JSONObject options) throws Exception {
        return dialog(options, null);
    }

    public JSONObject dialog(JSONObject options, Dialog.Callback callback) throws Exception {
        return dialog.show(options, callback);
    }

    public final void closeDialog() {
        dialog.close();
    }

    public static Dns safeDns() {
        return OkHttp.dns();
    }

    public Activity getActivity() {
        return Runtime.activity();
    }

    public String proxyUrl() {
        return proxyUrl(Collections.emptyMap());
    }

    public String danmakuUrl(String id) {
        return danmakuUrl(siteKey, id);
    }

    public static String danmakuUrl(String siteKey, String id) {
        if (siteKey == null || siteKey.isEmpty() || id == null || id.isEmpty()) throw new IllegalArgumentException("Danmaku requires a site key and ID");
        return new Uri.Builder().scheme("spider").authority(siteKey).appendPath(id).build().toString();
    }

    public String proxyUrl(Map<String, String> params) {
        return proxyUrl(params, true);
    }

    public String proxyUrl(Map<String, String> params, boolean local) {
        if (params == null) params = Collections.emptyMap();
        Uri.Builder url = Uri.parse(Proxy.getUrl(local)).buildUpon();
        url.appendQueryParameter("do", "jar");
        url.appendQueryParameter("siteKey", siteKey);
        for (Map.Entry<?, ?> entry : params.entrySet()) appendProxyParam(url, entry.getKey(), entry.getValue());
        return url.build().toString();
    }

    private void appendProxyParam(Uri.Builder url, Object key, Object value) {
        if (!(key instanceof String name) || !(value instanceof String text)) throw new IllegalArgumentException("Proxy parameters require string keys and values");
        if (name.equals("do") || name.equals("siteKey")) throw new IllegalArgumentException("Reserved proxy parameter");
        url.appendQueryParameter(name, text);
    }

    public void init(Context context, String extend) throws Exception {
    }

    public String homeContent(boolean filter) throws Exception {
        return "{\"class\":[]}";
    }

    public String homeVideoContent() throws Exception {
        return "{\"list\":[]}";
    }

    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        return "{\"list\":[]}";
    }

    public String detailContent(List<String> ids) throws Exception {
        return "{\"list\":[]}";
    }

    public String searchContent(String key, boolean quick) throws Exception {
        return searchContent(key, quick, "1");
    }

    public String searchContent(String key, boolean quick, String pg) throws Exception {
        return "{\"list\":[]}";
    }

    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return "{}";
    }

    public String liveContent(String url) throws Exception {
        return "";
    }

    public boolean manualVideoCheck() throws Exception {
        return false;
    }

    public boolean isVideoFormat(String url) throws Exception {
        return false;
    }

    public Object[] proxy(Map<String, String> params) throws Exception {
        return new Object[]{404, "text/plain", new ByteArrayInputStream(new byte[0])};
    }

    public String action(String action) throws Exception {
        return "{}";
    }

    public AutoCloseable danmaku(String id, Consumer<JSONObject> emit) throws Exception {
        throw new UnsupportedOperationException("Spider does not support live danmaku");
    }

    public void destroy() {
    }
}
