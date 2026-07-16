package com.fongmi.android.tv.api;

import android.text.TextUtils;

import com.fongmi.android.tv.setting.SubtitleSetting;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Trans;

import java.util.Objects;

import okhttp3.Call;
import okhttp3.HttpUrl;

public class SubtitleApi {

    private static final String TAG = SubtitleApi.class.getSimpleName();
    private static final String SEARCH_URL = "https://api.assrt.net/v1/sub/search";
    private static final String DETAIL_URL = "https://api.assrt.net/v1/sub/detail";
    private static final int SEARCH_COUNT = 15;

    public static boolean hasToken() {
        return !TextUtils.isEmpty(SubtitleSetting.getEffectiveToken());
    }

    public static int getSearchCount() {
        return SEARCH_COUNT;
    }

    public static Call search(String keyword, int pos) {
        OkHttp.cancel(TAG);
        keyword = Trans.t2s(keyword);
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(SEARCH_URL)).newBuilder().addQueryParameter("token", SubtitleSetting.getEffectiveToken()).addQueryParameter("q", keyword).addQueryParameter("cnt", String.valueOf(SEARCH_COUNT)).addQueryParameter("pos", String.valueOf(pos)).addQueryParameter("filelist", "1").build();
        return OkHttp.newCall(url.toString(), TAG);
    }

    public static Call detail(int id) {
        OkHttp.cancel(TAG);
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(DETAIL_URL)).newBuilder().addQueryParameter("token", SubtitleSetting.getEffectiveToken()).addQueryParameter("id", String.valueOf(id)).build();
        return OkHttp.newCall(url.toString(), TAG);
    }

    public static void cancel() {
        OkHttp.cancel(TAG);
    }
}
