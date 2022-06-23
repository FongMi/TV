package com.fongmi.bear.bean;

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
        return vodId;
    }

    public String getVodName() {
        return vodName;
    }

    public String getVodPic() {
        return vodPic;
    }

    public String getVodRemarks() {
        return vodRemarks;
    }
}
