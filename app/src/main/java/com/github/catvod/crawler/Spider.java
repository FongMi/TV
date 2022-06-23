package com.github.catvod.crawler;

import android.content.Context;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public abstract class Spider {

    public static JSONObject empty = new JSONObject();

    public void init(Context context) {
    }

    public void init(Context context, String extend) {
        init(context);
    }

    /**
     * 首頁數據內容
     */
    public String homeContent(boolean filter) {
        return "";
    }

    /**
     * 首頁最近更新數據 如果上面的homeContent中不包含首頁最近更新視頻的數據 可以使用這個接口返回
     */
    public String homeVideoContent() {
        return "";
    }

    /**
     * 分類數據
     */
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        return "";
    }

    /**
     * 詳情數據
     */
    public String detailContent(List<String> ids) {
        return "";
    }

    /**
     * 搜索數據內容
     */
    public String searchContent(String key, boolean quick) {
        return "";
    }

    /**
     * 播放信息
     */
    public String playerContent(String flag, String id, List<String> vipFlags) {
        return "";
    }

    /**
     * Webview 解析時使用 可自定義判斷當前加載的 url 是否是視頻
     */
    public boolean isVideoFormat(String url) {
        return false;
    }

    /**
     * 是否手動檢測 Webview 中加載的 url
     */
    public boolean manualVideoCheck() {
        return false;
    }
}
