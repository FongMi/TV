package com.fongmi.bear.bean;

import android.text.TextUtils;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Vod {

    @SerializedName("vod_id")
    private String vodId;
    @SerializedName("vod_name")
    private String vodName;
    @SerializedName("type_name")
    private String typeName;
    @SerializedName("vod_pic")
    private String vodPic;
    @SerializedName("vod_remarks")
    private String vodRemarks;
    @SerializedName("vod_year")
    private String vodYear;
    @SerializedName("vod_area")
    private String vodArea;
    @SerializedName("vod_director")
    private String vodDirector;
    @SerializedName("vod_actor")
    private String vodActor;
    @SerializedName("vod_content")
    private String vodContent;
    @SerializedName("vod_play_from")
    private String vodPlayFrom;
    @SerializedName("vod_play_url")
    private String vodPlayUrl;

    private List<Flag> vodFlags;

    public static Vod objectFrom(String str) {
        return new Gson().fromJson(str, Vod.class);
    }

    public String getVodId() {
        return TextUtils.isEmpty(vodId) ? "" : vodId;
    }

    public String getVodName() {
        return TextUtils.isEmpty(vodName) ? "" : vodName;
    }

    public String getTypeName() {
        return TextUtils.isEmpty(typeName) ? "" : typeName;
    }

    public String getVodPic() {
        return TextUtils.isEmpty(vodPic) ? "" : vodPic;
    }

    public String getVodRemarks() {
        return TextUtils.isEmpty(vodRemarks) ? "" : vodRemarks;
    }

    public String getVodYear() {
        return TextUtils.isEmpty(vodYear) ? "" : vodYear;
    }

    public String getVodArea() {
        return TextUtils.isEmpty(vodArea) ? "" : vodArea;
    }

    public String getVodDirector() {
        return TextUtils.isEmpty(vodDirector) ? "" : vodDirector;
    }

    public String getVodActor() {
        return TextUtils.isEmpty(vodActor) ? "" : vodActor;
    }

    public String getVodContent() {
        return TextUtils.isEmpty(vodContent) ? "" : vodContent.replaceAll("\\s+", "");
    }

    public String getVodPlayFrom() {
        return TextUtils.isEmpty(vodPlayFrom) ? "" : vodPlayFrom;
    }

    public String getVodPlayUrl() {
        return TextUtils.isEmpty(vodPlayUrl) ? "" : vodPlayUrl;
    }

    public List<Flag> getVodFlags() {
        return vodFlags;
    }

    public void setVodFlags(List<Flag> vodFlags) {
        this.vodFlags = vodFlags;
    }

    public int getRemarkVisible() {
        return getVodRemarks().isEmpty() ? View.GONE : View.VISIBLE;
    }

    public static class Flag {

        private final String flag;
        private final List<Episode> episodes;

        public Flag(String flag) {
            this.flag = flag;
            this.episodes = new ArrayList<>();
        }

        public String getFlag() {
            return flag;
        }

        public List<Episode> getEpisodes() {
            return episodes;
        }

        public static class Episode {

            private final String name;
            private final String url;

            public Episode(String name, String url) {
                this.name = name;
                this.url = url;
            }

            public String getName() {
                return name;
            }

            public String getUrl() {
                return url;
            }
        }
    }
}
