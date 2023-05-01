package com.fongmi.android.tv.bean;

import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.utils.Trans;
import com.fongmi.android.tv.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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

    @SerializedName("vod_tag")
    private String vodTag;

    @Path("dl")
    @ElementList(entry = "dd", required = false, inline = true)
    private List<Flag> vodFlags;

    private Site site;

    public static List<Vod> arrayFrom(String str) {
        Type listType = new TypeToken<List<Vod>>() {}.getType();
        List<Vod> items = new Gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public String getVodId() {
        return TextUtils.isEmpty(vodId) ? "" : vodId.trim();
    }

    public String getVodName() {
        return TextUtils.isEmpty(vodName) ? "" : vodName.trim();
    }

    public String getTypeName() {
        return TextUtils.isEmpty(typeName) ? "" : typeName.trim();
    }

    public String getVodPic() {
        return TextUtils.isEmpty(vodPic) ? "" : vodPic.trim();
    }

    public String getVodRemarks() {
        return TextUtils.isEmpty(vodRemarks) ? "" : vodRemarks.trim();
    }

    public String getVodYear() {
        return TextUtils.isEmpty(vodYear) ? "" : vodYear.trim();
    }

    public String getVodArea() {
        return TextUtils.isEmpty(vodArea) ? "" : vodArea.trim();
    }

    public String getVodDirector() {
        return TextUtils.isEmpty(vodDirector) ? "" : vodDirector.trim();
    }

    public String getVodActor() {
        return TextUtils.isEmpty(vodActor) ? "" : vodActor.trim();
    }

    public String getVodContent() {
        return TextUtils.isEmpty(vodContent) ? "" : vodContent.trim().replace("\n", "<br>");
    }

    public String getVodPlayFrom() {
        return TextUtils.isEmpty(vodPlayFrom) ? "" : vodPlayFrom;
    }

    public String getVodPlayUrl() {
        return TextUtils.isEmpty(vodPlayUrl) ? "" : vodPlayUrl;
    }

    public String getVodTag() {
        return TextUtils.isEmpty(vodTag) ? "" : vodTag;
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

    public String getSiteName() {
        return getSite() == null ? "" : getSite().getName();
    }

    public String getSiteKey() {
        return getSite() == null ? "" : getSite().getKey();
    }

    public int getSiteVisible() {
        return getSite() == null ? View.GONE : View.VISIBLE;
    }

    public int getYearVisible() {
        return getSite() != null || getVodYear().length() < 4 ? View.GONE : View.VISIBLE;
    }

    public int getRemarkVisible() {
        return getVodRemarks().isEmpty() ? View.GONE : View.VISIBLE;
    }

    public boolean isFolder() {
        return getVodTag().equals("folder");
    }

    public void trans() {
        if (Trans.pass()) return;
        this.vodName = Trans.s2t(vodName);
        this.vodArea = Trans.s2t(vodArea);
        this.typeName = Trans.s2t(typeName);
        this.vodActor = Trans.s2t(vodActor);
        this.vodRemarks = Trans.s2t(vodRemarks);
        this.vodContent = Trans.s2t(vodContent);
        this.vodDirector = Trans.s2t(vodDirector);
    }

    public void setVodFlags() {
        String[] playFlags = getVodPlayFrom().split("\\$\\$\\$");
        String[] playUrls = getVodPlayUrl().split("\\$\\$\\$");
        for (int i = 0; i < playFlags.length; i++) {
            if (playFlags[i].isEmpty() || i >= playUrls.length) continue;
            Vod.Flag item = new Vod.Flag(playFlags[i].trim());
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
        private String show;

        @Text
        private String urls;

        @SerializedName("episodes")
        private List<Episode> episodes;

        private boolean activated;

        public Flag() {
            this.episodes = new ArrayList<>();
        }

        public Flag(String flag) {
            this.episodes = new ArrayList<>();
            this.show = Trans.s2t(flag);
            this.flag = flag;
        }

        public String getShow() {
            return TextUtils.isEmpty(show) ? getFlag() : show;
        }

        public String getFlag() {
            return TextUtils.isEmpty(flag) ? "" : flag;
        }

        public String getUrls() {
            return urls;
        }

        public List<Episode> getEpisodes() {
            return episodes;
        }

        public boolean isActivated() {
            return activated;
        }

        public void setActivated(Flag item) {
            this.activated = item.equals(this);
            if (activated) item.episodes = episodes;
        }

        public void createEpisode(String data) {
            String[] urls = data.contains("#") ? data.split("#") : new String[]{data};
            for (int i = 0; i < urls.length; i++) {
                String[] split = urls[i].split("\\$");
                String number = String.format(Locale.getDefault(), "%02d", i + 1);
                Episode episode = split.length > 1 ? new Vod.Flag.Episode(split[0].isEmpty() ? number : split[0].trim(), split[1]) : new Vod.Flag.Episode(number, urls[i]);
                if (!getEpisodes().contains(episode)) getEpisodes().add(episode);
            }
        }

        public void toggle(boolean activated, Episode episode) {
            if (activated) for (Episode item : getEpisodes()) item.setActivated(episode);
            else for (Episode item : getEpisodes()) item.deactivated();
        }

        public Episode find(String remarks) {
            int number = Utils.getDigit(remarks);
            if (getEpisodes().size() == 1) return getEpisodes().get(0);
            for (Vod.Flag.Episode item : getEpisodes()) if (item.rule1(remarks)) return item;
            for (Vod.Flag.Episode item : getEpisodes()) if (item.rule2(number)) return item;
            for (Vod.Flag.Episode item : getEpisodes()) if (item.rule3(remarks)) return item;
            for (Vod.Flag.Episode item : getEpisodes()) if (item.rule4(remarks)) return item;
            return null;
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

            private final int number;

            private boolean activated;

            public static Episode objectFrom(String str) {
                return new Gson().fromJson(str, Episode.class);
            }

            public static List<Episode> arrayFrom(String str) {
                Type listType = new TypeToken<List<Episode>>() {}.getType();
                List<Episode> items = new Gson().fromJson(str, listType);
                return items == null ? Collections.emptyList() : items;
            }

            public Episode(String name, String url) {
                this.number = Utils.getDigit(name);
                this.name = Trans.s2t(name);
                this.url = url;
            }

            public String getName() {
                return name;
            }

            public String getUrl() {
                return url;
            }

            public int getNumber() {
                return number;
            }

            public boolean isActivated() {
                return activated;
            }

            public void deactivated() {
                this.activated = false;
            }

            private void setActivated(Episode item) {
                this.activated = item.equals(this);
            }

            public boolean rule1(String name) {
                return getName().equalsIgnoreCase(name);
            }

            public boolean rule2(int number) {
                return getNumber() == number && number != -1;
            }

            public boolean rule3(String name) {
                return getName().toLowerCase().contains(name.toLowerCase());
            }

            public boolean rule4(String name) {
                return name.toLowerCase().contains(getName().toLowerCase());
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof Episode)) return false;
                Episode it = (Episode) obj;
                return getUrl().equals(it.getUrl()) || getName().equals(it.getName());
            }
        }
    }
}
