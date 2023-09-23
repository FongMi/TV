package com.github.catvod.crawler;

import android.content.Context;
import android.net.Uri;

import com.github.catvod.net.OkHttp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Dns;

public abstract class Spider {

    private boolean proxy;

    public Spider proxy(boolean proxy) {
        this.proxy = proxy;
        return this;
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

    public boolean manualVideoCheck() throws Exception {
        return false;
    }

    public boolean isVideoFormat(String url) throws Exception {
        return false;
    }

    public boolean isProxy() {
        return proxy;
    }

    public Object[] proxyLocal(Map<String, String> params) throws Exception {
        return null;
    }

    public void destroy() {
    }

    public static Dns safeDns() {
        return OkHttp.dns();
    }

    public static Uri proxy() {
        return OkHttp.proxy();
    }
}
