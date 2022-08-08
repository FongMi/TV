package com.fongmi.android.tv.bean;

import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.util.ArrayList;
import java.util.List;

@Root(strict = false)
public class Vod {

    @Element(name = "id", required = false)
    @SerializedName("vod_id")
    private String vodId;

    @Element(name = "name", required = false)
    @SerializedName("vod_name")
    private String vodName;

    @Element(name = "type", required = false)
    @SerializedName("type_name")
    private String typeName;

    @Element(name = "pic", required = false)
    @SerializedName("vod_pic")
    private String vodPic;

    @Element(name = "note", required = false)
    @SerializedName("vod_remarks")
    private String vodRemarks;

    @Element(name = "year", required = false)
    @SerializedName("vod_year")
    private String vodYear;

    @Element(name = "area", required = false)
    @SerializedName("vod_area")
    private String vodArea;

    @Element(name = "director", required = false)
    @SerializedName("vod_director")
    private String vodDirector;

    @Element(name = "actor", required = false)
    @SerializedName("vod_actor")
    private String vodActor;

    @Element(name = "des", required = false)
    @SerializedName("vod_content")
    private String vodContent;

    @SerializedName("vod_play_from")
    private String vodPlayFrom;

    @SerializedName("vod_play_url")
    private String vodPlayUrl;

    @Path("dl")
    @ElementList(entry = "dd", required = false, inline = true)
    private List<Flag> vodFlags;

    private Site site;

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
        return vodFlags = vodFlags == null ? new ArrayList<>() : vodFlags;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public int getRemarkVisible() {
        return getVodRemarks().isEmpty() ? View.GONE : View.VISIBLE;
    }

    public void setVodFlags() {
        String[] playFlags = getVodPlayFrom().split("\\$\\$\\$");
        String[] playUrls = getVodPlayUrl().split("\\$\\$\\$");
        for (int i = 0; i < playFlags.length; i++) {
            if (playFlags[i].isEmpty()) continue;
            Vod.Flag item = new Vod.Flag(playFlags[i]);
            item.createEpisode(playUrls[i]);
            getVodFlags().add(item);
        }
        for (Vod.Flag item : getVodFlags()) {
            if (item.getUrls() == null) continue;
            item.createEpisode(item.getUrls());
        }
    }

    public static class Flag {

        @Attribute(name = "flag", required = false)
        @SerializedName("flag")
        private String flag;

        @Text
        private String urls;

        @SerializedName("episodes")
        private List<Episode> episodes;

        private boolean activated;

        public static Flag objectFrom(String str) {
            return new Gson().fromJson(str, Flag.class);
        }

        public Flag() {
            this.episodes = new ArrayList<>();
        }

        public Flag(String flag) {
            this();
            this.flag = flag;
        }

        public String getFlag() {
            return flag;
        }

        public String getUrls() {
            return urls;
        }

        public List<Episode> getEpisodes() {
            return episodes;
        }

        public void createEpisode(String data) {
            String[] urls = data.contains("#") ? data.split("#") : new String[]{data};
            for (String url : urls) {
                String[] split = url.split("\\$");
                if (split.length >= 2) getEpisodes().add(new Vod.Flag.Episode(split[0], split[1]));
                else getEpisodes().add(new Vod.Flag.Episode(ResUtil.getString(R.string.play), url));
            }
        }

        public boolean isActivated() {
            return activated;
        }

        public void setActivated(boolean activated) {
            this.activated = activated;
        }

        public void toggle(boolean activated, Episode episode) {
            if (activated) for (Episode item : getEpisodes()) item.setActivated(episode);
            else for (Episode item : getEpisodes()) item.deactivated();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Flag)) return false;
            Flag it = (Flag) obj;
            return getFlag().equals(it.getFlag());
        }

        @NonNull
        @Override
        public String toString() {
            return new Gson().toJson(this);
        }

        public static class Episode {

            @SerializedName("name")
            private final String name;
            @SerializedName("url")
            private final String url;

            private boolean activated;

            public Episode(String url) {
                this("", url);
            }

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

            public boolean isActivated() {
                return activated;
            }

            private void deactivated() {
                this.activated = false;
            }

            private void setActivated(Episode item) {
                this.activated = item.equals(this);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof Episode)) return false;
                Episode it = (Episode) obj;
                return getUrl().equals(it.getUrl());
            }
        }
    }
}
