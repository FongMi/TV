package com.github.catvod.crawler;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import com.github.catvod.Proxy;
import com.github.catvod.Runtime;
import com.github.catvod.net.Net;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Js;
import com.github.catvod.utils.Local;

import org.json.JSONObject;

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

    public static Dns safeDns() {
        return OkHttp.dns();
    }

    public Activity getActivity() {
        return Runtime.activity();
    }

    public String getProxyUrl() {
        return getProxyUrl(Collections.emptyMap());
    }

    public String getDanmakuUrl(String id) {
        return getDanmakuUrl(siteKey, id);
    }

    public static String getDanmakuUrl(String siteKey, String id) {
        if (siteKey == null || siteKey.isEmpty() || id == null || id.isEmpty()) throw new IllegalArgumentException("Danmaku requires a site key and ID");
        return new Uri.Builder().scheme("spider").authority(siteKey).appendPath(id).build().toString();
    }

    public String getProxyUrl(Map<String, String> params) {
        Uri.Builder url = Uri.parse(Proxy.getUrl(true)).buildUpon().appendQueryParameter("do", "jar").appendQueryParameter("siteKey", siteKey);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().equals("do") || entry.getKey().equals("siteKey")) throw new IllegalArgumentException("Reserved proxy parameter");
            url.appendQueryParameter(entry.getKey(), entry.getValue());
        }
        return url.build().toString();
    }

    public void init(Context context) throws Exception {
    }

    public void init(Context context, String extend) throws Exception {
        init(context);
    }

    public String homeContent(boolean filter) throws Exception {
        return "";
    }

    public String homeVideoContent() throws Exception {
        return "";
    }

    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        return "";
    }

    public String detailContent(List<String> ids) throws Exception {
        return "";
    }

    public String searchContent(String key, boolean quick) throws Exception {
        return "";
    }

    public String searchContent(String key, boolean quick, String pg) throws Exception {
        return "";
    }

    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return "";
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
        return null;
    }

    public String action(String action) throws Exception {
        return null;
    }

    public AutoCloseable danmaku(String id, Consumer<JSONObject> emit) throws Exception {
        throw new UnsupportedOperationException("Spider does not support live danmaku");
    }

    public void destroy() {
    }
}
