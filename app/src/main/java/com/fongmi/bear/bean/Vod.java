package com.fongmi.bear.bean;

import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class Vod {

    @SerializedName("vod_id")
    private String vodId;
    @SerializedName("vod_name")
    private String vodName;
    @SerializedName("vod_pic")
    private String vodPic;
    @SerializedName("vod_remarks")
    private String vodRemarks;

    public static Vod objectFrom(String str) {
        return new Gson().fromJson(str, Vod.class);
    }

    public String getVodId() {
        return TextUtils.isEmpty(vodId) ? "" : vodId;
    }

    public String getVodName() {
        return TextUtils.isEmpty(vodName) ? "" : vodName;
    }

    public String getVodPic() {
        return TextUtils.isEmpty(vodPic) ? "" : vodPic;
    }

    public String getVodRemarks() {
        return TextUtils.isEmpty(vodRemarks) ? "" : vodRemarks;
    }

    public int getRemarkVisible() {
        return getVodRemarks().isEmpty() ? View.GONE : View.VISIBLE;
    }
}
