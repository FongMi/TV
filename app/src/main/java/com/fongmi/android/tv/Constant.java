package com.fongmi.android.tv;

public class Constant {
    //快進時間單位
    public static final int INTERVAL_SEEK = 10 * 1000;
    //控件隱藏時間
    public static final int INTERVAL_HIDE = 5 * 1000;
    //網路偵測間隔
    public static final int INTERVAL_TRAFFIC = 500;
    //點播爬蟲時間
    public static final int TIMEOUT_VOD = 30 * 1000;
    //直播解析時間
    public static final int TIMEOUT_LIVE = 10 * 1000;
    //播放超時時間
    public static final int TIMEOUT_PLAY = 15 * 1000;
    //解析預設時間
    public static final int TIMEOUT_PARSE_DEF = 15 * 1000;
    //嗅探超時時間
    public static final int TIMEOUT_PARSE_WEB = 15 * 1000;
    //網路超時時間
    public static final int TIMEOUT_HTTP = 30 * 1000;
    //代理超時時間
    public static final int TIMEOUT_GITHUB = 5 * 1000;
    //搜尋線程數量
    public static final int THREAD_POOL = 5;
}
