package com.fongmi.android.tv.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.impl.Diffable;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Trans;
import com.google.gson.annotations.SerializedName;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Flag implements Parcelable, Diffable<Flag> {

    @Attribute(name = "flag", required = false)
    @SerializedName("flag")
    private String flag;
    private String show;

    @Text
    private String urls;

    @SerializedName("episodes")
    private List<Episode> episodes;

    private boolean selected;
    private int position;

    public Flag() {
        this.position = -1;
        this.episodes = new ArrayList<>();
    }

    public Flag(String flag) {
        this.flag = flag;
        this.position = -1;
        this.episodes = new ArrayList<>();
    }

    protected Flag(Parcel in) {
        this.flag = in.readString();
        this.show = in.readString();
        this.urls = in.readString();
        this.episodes = in.createTypedArrayList(Episode.CREATOR);
        this.selected = in.readByte() != 0;
        this.position = in.readInt();
    }

    public static Flag create(String flag) {
        return new Flag(flag).trans();
    }

    public static Flag create(String flag, String url) {
        Flag item = create(flag);
        item.setEpisodes(url);
        return item;
    }

    public String getShow() {
        return TextUtils.isEmpty(show) ? getFlag() : show;
    }

    public String getFlag() {
        return TextUtils.isEmpty(flag) ? "" : flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public String getUrls() {
        return TextUtils.isEmpty(urls) ? "" : urls;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(String url) {
        String[] urls = url.contains("#") ? url.split("#") : new String[]{url};
        for (int i = 0; i < urls.length; i++) {
            String[] split = urls[i].split("\\$", 2);
            String number = String.format(Locale.getDefault(), "%02d", i + 1);
            Episode episode = split.length > 1 ? Episode.create(split[0].isEmpty() ? number : split[0].trim(), split[1]) : Episode.create(number, urls[i]);
            if (!getEpisodes().contains(episode)) getEpisodes().add(episode);
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(Flag item) {
        this.selected = item.equals(this);
        if (selected) item.episodes = episodes;
    }

    private void setSelected(Episode episode) {
        setPosition(getEpisodes().indexOf(episode));
        for (int i = 0; i < getEpisodes().size(); i++) getEpisodes().get(i).setSelected(i == getPosition());
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void toggle(boolean selected, Episode episode) {
        if (selected) setSelected(episode);
        else getEpisodes().forEach(Episode::deselect);
    }

    public Episode find(String remarks, boolean strict) {
        if (getEpisodes().isEmpty()) return null;
        if (getEpisodes().size() == 1) return getEpisodes().get(0);
        int number = Util.getNumber(remarks);
        return getEpisodes().stream()
                .map(episode -> new Episode.Rule(episode, episode.getScore(remarks, number)))
                .filter(Episode.Rule::find).max(Comparator.comparingInt(Episode.Rule::score)).map(Episode.Rule::episode)
                .orElseGet(() -> isPositionValid() ? getEpisodes().get(getPosition()) : strict ? null : getEpisodes().get(0));
    }

    private boolean isPositionValid() {
        return getPosition() >= 0 && getPosition() < getEpisodes().size();
    }

    public void mergeEpisodes(List<Episode> items, boolean rev) {
        Episode current = isPositionValid() ? getEpisodes().get(getPosition()) : null;
        for (Episode item : items) {
            if (getEpisodes().contains(item)) continue;
            if (rev) getEpisodes().add(0, item);
            else getEpisodes().add(item);
        }
        if (rev && current != null) setPosition(getEpisodes().indexOf(current));
    }

    public Flag trans() {
        if (Trans.pass()) return this;
        this.show = Trans.s2t(flag);
        return this;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Flag it)) return false;
        return Objects.equals(getFlag(), it.getFlag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFlag());
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
        dest.writeByte(this.selected ? (byte) 1 : (byte) 0);
        dest.writeInt(this.position);
    }

    @Override
    public boolean isSameItem(Flag other) {
        return equals(other);
    }

    @Override
    public boolean isSameContent(Flag other) {
        return equals(other);
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
