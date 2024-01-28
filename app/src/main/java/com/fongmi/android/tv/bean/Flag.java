package com.fongmi.android.tv.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.extractor.Magnet;
import com.fongmi.android.tv.utils.Sniffer;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Trans;
import com.google.gson.annotations.SerializedName;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class Flag implements Parcelable {

    @Attribute(name = "flag", required = false)
    @SerializedName("flag")
    private String flag;
    private String show;

    @Text
    private String urls;

    @SerializedName("episodes")
    private List<Episode> episodes;

    private boolean activated;
    private int position;

    public static Flag create(String flag) {
        return new Flag(flag);
    }

    public Flag() {
        this.episodes = new ArrayList<>();
        this.position = -1;
    }

    public Flag(String flag) {
        this.episodes = new ArrayList<>();
        this.show = Trans.s2t(flag);
        this.flag = flag;
        this.position = -1;
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

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void createEpisode(String data) {
        String[] urls = data.contains("#") ? data.split("#") : new String[]{data};
        for (int i = 0; i < urls.length; i++) {
            String[] split = urls[i].split("\\$");
            String number = String.format(Locale.getDefault(), "%02d", i + 1);
            Episode episode = split.length > 1 ? Episode.create(split[0].isEmpty() ? number : split[0].trim(), split[1]) : Episode.create(number, urls[i]);
            if (!getEpisodes().contains(episode)) getEpisodes().add(episode);
        }
    }

    public void toggle(boolean activated, Episode episode) {
        if (activated) setActivated(episode);
        else for (Episode item : getEpisodes()) item.deactivated();
    }

    private void setActivated(Episode episode) {
        setPosition(getEpisodes().indexOf(episode));
        for (int i = 0; i < getEpisodes().size(); i++) getEpisodes().get(i).setActivated(i == getPosition());
    }

    public Episode find(String remarks, boolean strict) {
        int number = Util.getDigit(remarks);
        if (getEpisodes().size() == 0) return null;
        if (getEpisodes().size() == 1) return getEpisodes().get(0);
        for (Episode item : getEpisodes()) if (item.rule1(remarks)) return item;
        for (Episode item : getEpisodes()) if (item.rule2(number)) return item;
        if (number == -1) for (Episode item : getEpisodes()) if (item.rule3(remarks)) return item;
        if (number == -1) for (Episode item : getEpisodes()) if (item.rule4(remarks)) return item;
        if (getPosition() != -1) return getEpisodes().get(getPosition());
        return strict ? null : getEpisodes().get(0);
    }

    public static List<Flag> create(String flag, String name, String url) {
        Flag item = Flag.create(flag);
        item.getEpisodes().add(Episode.create(name, url));
        return List.of(item);
    }

    public List<Magnet> getMagnet() {
        Iterator<Episode> iterator = getEpisodes().iterator();
        List<Magnet> items = new ArrayList<>();
        while (iterator.hasNext()) addMagnet(iterator, items);
        return items;
    }

    private void addMagnet(Iterator<Episode> iterator, List<Magnet> items) {
        String url = iterator.next().getUrl();
        if (!Sniffer.isThunder(url)) return;
        items.add(Magnet.get(url));
        iterator.remove();
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
        return App.gson().toJson(this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.flag);
        dest.writeString(this.show);
        dest.writeString(this.urls);
        dest.writeTypedList(this.episodes);
        dest.writeByte(this.activated ? (byte) 1 : (byte) 0);
        dest.writeInt(this.position);
    }

    protected Flag(Parcel in) {
        this.flag = in.readString();
        this.show = in.readString();
        this.urls = in.readString();
        this.episodes = in.createTypedArrayList(Episode.CREATOR);
        this.activated = in.readByte() != 0;
        this.position = in.readInt();
    }

    public static final Creator<Flag> CREATOR = new Creator<>() {
        @Override
        public Flag createFromParcel(Parcel source) {
            return new Flag(source);
        }

        @Override
        public Flag[] newArray(int size) {
            return new Flag[size];
        }
    };
}
