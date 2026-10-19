package com.fongmi.android.tv.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.impl.Diffable;
import com.fongmi.android.tv.utils.Sniffer;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Trans;
import com.github.catvod.utils.UriUtil;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Root(strict = false)
public class Vod implements Parcelable, Diffable<Vod> {

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
    @SerializedName("action")
    private String action;
    @SerializedName("cate")
    private Cate cate;
    @SerializedName("style")
    private Style style;
    @SerializedName("land")
    private Integer land;
    @SerializedName("circle")
    private Integer circle;
    @SerializedName("ratio")
    private Float ratio;
    @Path("dl")
    @ElementList(entry = "dd", required = false, inline = true)
    private List<Flag> vodFlags;
    private Site site;

    public Vod() {
    }

    protected Vod(Parcel in) {
        this.vodId = in.readString();
        this.vodName = in.readString();
        this.typeName = in.readString();
        this.vodPic = in.readString();
        this.vodRemarks = in.readString();
        this.vodYear = in.readString();
        this.vodArea = in.readString();
        this.vodDirector = in.readString();
        this.vodActor = in.readString();
        this.vodContent = in.readString();
        this.vodPlayFrom = in.readString();
        this.vodPlayUrl = in.readString();
        this.vodTag = in.readString();
        this.action = in.readString();
        this.land = (Integer) in.readValue(Integer.class.getClassLoader());
        this.circle = (Integer) in.readValue(Integer.class.getClassLoader());
        this.ratio = (Float) in.readValue(Float.class.getClassLoader());
        this.cate = in.readParcelable(Cate.class.getClassLoader());
        this.style = in.readParcelable(Style.class.getClassLoader());
        this.vodFlags = in.createTypedArrayList(Flag.CREATOR);
        this.site = in.readParcelable(Site.class.getClassLoader());
    }

    public static Vod objectFrom(String str) {
        Vod vod = App.gson().fromJson(str, Vod.class);
        return vod == null ? new Vod() : vod.trans().setFlags();
    }

    public static List<Vod> arrayFrom(String str) {
        Type listType = TypeToken.getParameterized(List.class, Vod.class).getType();
        List<Vod> items = App.gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public String getId() {
        return TextUtils.isEmpty(vodId) ? "" : vodId.trim();
    }

    public void setId(String vodId) {
        this.vodId = vodId;
    }

    public String getName() {
        return TextUtils.isEmpty(vodName) ? "" : Html.fromHtml(vodName, Html.FROM_HTML_MODE_LEGACY).toString().trim();
    }

    public String getName(String fallback) {
        String name = getName();
        if (TextUtils.isEmpty(fallback)) return name;
        String scheme = UriUtil.scheme(name);
        return name.isEmpty() || (scheme.equals("http") || scheme.equals("https")) && !UriUtil.host(name).isEmpty() ? fallback : name;
    }

    public void setName(String vodName) {
        this.vodName = vodName;
    }

    public String getTypeName() {
        return TextUtils.isEmpty(typeName) ? "" : typeName.trim();
    }

    public String getPic() {
        return TextUtils.isEmpty(vodPic) ? "" : vodPic.trim();
    }

    public void setPic(String vodPic) {
        this.vodPic = vodPic;
    }

    public String getRemarks() {
        return TextUtils.isEmpty(vodRemarks) ? "" : vodRemarks.trim();
    }

    public String getYear() {
        return TextUtils.isEmpty(vodYear) ? "" : vodYear.trim();
    }

    public String getArea() {
        return TextUtils.isEmpty(vodArea) ? "" : vodArea.trim();
    }

    public String getDirector() {
        return TextUtils.isEmpty(vodDirector) ? "" : vodDirector.trim();
    }

    public void setDirector(String vodDirector) {
        this.vodDirector = vodDirector;
    }

    public String getActor() {
        return TextUtils.isEmpty(vodActor) ? "" : vodActor.trim();
    }

    public String getContent() {
        return TextUtils.isEmpty(vodContent) ? "" : Util.clean(vodContent);
    }

    public void setContent(String vodContent) {
        this.vodContent = vodContent;
    }

    public String getPlayFrom() {
        return TextUtils.isEmpty(vodPlayFrom) ? "" : vodPlayFrom;
    }

    public void setPlayFrom(String vodPlayFrom) {
        this.vodPlayFrom = vodPlayFrom;
    }

    public String getPlayUrl() {
        return TextUtils.isEmpty(vodPlayUrl) ? "" : vodPlayUrl;
    }

    public void setPlayUrl(String vodPlayUrl) {
        this.vodPlayUrl = vodPlayUrl;
    }

    public String getTag() {
        return TextUtils.isEmpty(vodTag) ? "" : vodTag;
    }

    public String getAction() {
        return TextUtils.isEmpty(action) ? "" : action;
    }

    public Cate getCate() {
        return cate;
    }

    public Style getStyle() {
        return style != null ? style : Style.get(getLand(), getCircle(), getRatio());
    }

    public int getLand() {
        return land == null ? 0 : land;
    }

    public int getCircle() {
        return circle == null ? 0 : circle;
    }

    public float getRatio() {
        return ratio == null ? 0 : ratio;
    }

    public List<Flag> getFlags() {
        return vodFlags = vodFlags == null ? new ArrayList<>() : vodFlags;
    }

    public void setFlags(List<Flag> vodFlags) {
        this.vodFlags = vodFlags;
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
        return getSite() != null || getYear().length() < 4 ? View.GONE : View.VISIBLE;
    }

    public int getNameVisible() {
        return getName().isEmpty() ? View.GONE : View.VISIBLE;
    }

    public int getRemarkVisible() {
        return getRemarks().isEmpty() ? View.GONE : View.VISIBLE;
    }

    public boolean isFolder() {
        return "folder".equals(getTag()) || getCate() != null;
    }

    public boolean isAction() {
        return !getAction().isEmpty();
    }

    public void checkPic(String pic) {
        if (getPic().isEmpty()) setPic(pic);
    }

    public void checkName(String name) {
        String title = getName(name);
        if (!title.equals(getName())) setName(title);
    }

    public Style getStyle(Style style) {
        return getStyle() != null ? getStyle() : style != null ? style : Style.rect();
    }

    public Vod setFlags() {
        String[] playUrls = getPlayUrl().split("\\$\\$\\$");
        String[] playFlags = getPlayFrom().split("\\$\\$\\$");
        if (!getFlags().isEmpty()) for (Flag item : getFlags()) item.setEpisodes(item.getUrls());
        else IntStream.range(0, playFlags.length).filter(i -> !playFlags[i].trim().isEmpty() && i < playUrls.length && !TextUtils.isEmpty(playUrls[i])).mapToObj(i -> Flag.create(playFlags[i].trim(), playUrls[i])).forEach(getFlags()::add);
        return this;
    }

    public Vod trans() {
        if (Trans.pass()) return this;
        this.vodName = Trans.s2t(vodName);
        this.vodArea = Trans.s2t(vodArea);
        this.typeName = Trans.s2t(typeName);
        if (vodActor != null) this.vodActor = Sniffer.CLICKER.matcher(vodActor).find() ? vodActor : Trans.s2t(vodActor);
        if (vodRemarks != null) this.vodRemarks = Sniffer.CLICKER.matcher(vodRemarks).find() ? vodRemarks : Trans.s2t(vodRemarks);
        if (vodContent != null) this.vodContent = Sniffer.CLICKER.matcher(vodContent).find() ? vodContent : Trans.s2t(vodContent);
        if (vodDirector != null) this.vodDirector = Sniffer.CLICKER.matcher(vodDirector).find() ? vodDirector : Trans.s2t(vodDirector);
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        return App.gson().toJson(this);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Vod it)) return false;
        return !getId().isEmpty() && !it.getId().isEmpty() ? Objects.equals(getId(), it.getId()) : Objects.equals(getName(), it.getName());
    }

    @Override
    public int hashCode() {
        return !getId().isEmpty() ? Objects.hash(getId()) : Objects.hash(getName());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.vodId);
        dest.writeString(this.vodName);
        dest.writeString(this.typeName);
        dest.writeString(this.vodPic);
        dest.writeString(this.vodRemarks);
        dest.writeString(this.vodYear);
        dest.writeString(this.vodArea);
        dest.writeString(this.vodDirector);
        dest.writeString(this.vodActor);
        dest.writeString(this.vodContent);
        dest.writeString(this.vodPlayFrom);
        dest.writeString(this.vodPlayUrl);
        dest.writeString(this.vodTag);
        dest.writeString(this.action);
        dest.writeValue(this.land);
        dest.writeValue(this.circle);
        dest.writeValue(this.ratio);
        dest.writeParcelable(this.cate, flags);
        dest.writeParcelable(this.style, flags);
        dest.writeTypedList(this.vodFlags);
        dest.writeParcelable(this.site, flags);
    }

    @Override
    public boolean isSameItem(Vod other) {
        return equals(other);
    }

    @Override
    public boolean isSameContent(Vod other) {
        return getName().equals(other.getName()) && getPic().equals(other.getPic()) && getRemarks().equals(other.getRemarks()) && Objects.equals(getSite(), other.getSite());
    }

    public static final Creator<Vod> CREATOR = new Creator<>() {
        @Override
        public Vod createFromParcel(Parcel source) {
            return new Vod(source);
        }

        @Override
        public Vod[] newArray(int size) {
            return new Vod[size];
        }
    };
}
