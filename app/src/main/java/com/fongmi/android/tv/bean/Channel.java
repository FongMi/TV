package com.fongmi.android.tv.bean;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.utils.Json;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class Channel {

    @SerializedName("urls")
    private List<String> urls;
    @SerializedName("number")
    private String number;
    @SerializedName("logo")
    private String logo;
    @SerializedName("epg")
    private String epg;
    @SerializedName("name")
    private String name;
    @SerializedName("ua")
    private String ua;
    @SerializedName("click")
    private String click;
    @SerializedName("format")
    private String format;
    @SerializedName("origin")
    private String origin;
    @SerializedName("referer")
    private String referer;
    @SerializedName("tvgId")
    private String tvgId;
    @SerializedName("tvgName")
    private String tvgName;
    @SerializedName("catchup")
    private Catchup catchup;
    @SerializedName("header")
    private JsonElement header;
    @SerializedName("parse")
    private Integer parse;
    @SerializedName("drm")
    private Drm drm;

    private boolean selected;
    private Group group;
    private String url;
    private String msg;
    private Epg data;
    private int line;

    public static Channel objectFrom(JsonElement element) {
        return App.gson().fromJson(element, Channel.class);
    }

    public static Channel create(int number) {
        return new Channel().setNumber(number);
    }

    public static Channel create(String name) {
        return new Channel(name);
    }

    public static Channel create(Channel channel) {
        return new Channel().copy(channel);
    }

    public static Channel error(String msg) {
        Channel result = new Channel();
        result.setMsg(msg);
        return result;
    }

    public Channel() {
    }

    public Channel(String name) {
        this.name = name;
    }

    public List<String> getUrls() {
        return urls = urls == null ? new ArrayList<>() : urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public String getNumber() {
        return TextUtils.isEmpty(number) ? "" : number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getLogo() {
        return TextUtils.isEmpty(logo) ? "" : logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getEpg() {
        return TextUtils.isEmpty(epg) ? "" : epg;
    }

    public void setEpg(String epg) {
        this.epg = epg;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUa() {
        return TextUtils.isEmpty(ua) ? "" : ua;
    }

    public void setUa(String ua) {
        this.ua = ua;
    }

    public String getClick() {
        return TextUtils.isEmpty(click) ? "" : click;
    }

    public void setClick(String click) {
        this.click = click;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getOrigin() {
        return TextUtils.isEmpty(origin) ? "" : origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getReferer() {
        return TextUtils.isEmpty(referer) ? "" : referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getTvgId() {
        return TextUtils.isEmpty(tvgId) ? getTvgName() : tvgId;
    }

    public void setTvgId(String tvgId) {
        this.tvgId = tvgId;
    }

    public String getTvgName() {
        return TextUtils.isEmpty(tvgName) ? getName() : tvgName;
    }

    public void setTvgName(String tvgName) {
        this.tvgName = tvgName;
    }

    public Catchup getCatchup() {
        return catchup == null ? new Catchup() : catchup;
    }

    public void setCatchup(Catchup catchup) {
        this.catchup = catchup;
    }

    public JsonElement getHeader() {
        return header;
    }

    public void setHeader(JsonElement header) {
        this.header = header;
    }

    public Integer getParse() {
        return parse == null ? 0 : parse;
    }

    public void setParse(Integer parse) {
        this.parse = parse;
    }

    public Drm getDrm() {
        return drm;
    }

    public void setDrm(Drm drm) {
        this.drm = drm;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMsg() {
        return TextUtils.isEmpty(msg) ? "" : msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean hasMsg() {
        return !getMsg().isEmpty();
    }

    public Epg getData() {
        return data == null ? new Epg() : data;
    }

    public void setData(Epg data) {
        this.data = data;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = Math.max(line, 0);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setSelected(Channel item) {
        this.selected = item.equals(this);
    }

    public int getLineVisible() {
        return isOnly() ? View.GONE : View.VISIBLE;
    }

    public void loadLogo(ImageView view) {
        ImgUtil.loadLive(getLogo(), view);
    }

    public void addUrls(String... urls) {
        getUrls().addAll(new ArrayList<>(Arrays.asList(urls)));
    }

    public void nextLine() {
        setLine(getLine() < getUrls().size() - 1 ? getLine() + 1 : 0);
    }

    public void prevLine() {
        setLine(getLine() > 0 ? getLine() - 1 : getUrls().size() - 1);
    }

    public String getCurrent() {
        return getUrls().isEmpty() ? "" : getUrls().get(getLine()).split("\\$")[0];
    }

    public boolean isOnly() {
        return getUrls().size() == 1;
    }

    public boolean isLast() {
        return getUrls().isEmpty() || getLine() == getUrls().size() - 1;
    }

    public boolean hasCatchup() {
        if (getCatchup().isEmpty() && getCurrent().contains("/PLTV/")) setCatchup(Catchup.PLTV());
        if (!getCatchup().getRegex().isEmpty()) return getCatchup().match(getCurrent());
        return !getCatchup().isEmpty();
    }

    public String getLineText() {
        if (getUrls().size() <= 1) return "";
        String[] sp = getUrls().get(getLine()).split("\\$");
        if (sp.length > 1 && !sp[1].isEmpty()) return sp[1];
        return ResUtil.getString(R.string.live_line, getLine() + 1);
    }

    public Channel setNumber(int number) {
        setNumber(String.format(Locale.getDefault(), "%03d", number));
        return this;
    }

    public Channel group(Group group) {
        setGroup(group);
        return this;
    }

    public void live(Live live) {
        if (!live.getUa().isEmpty() && getUa().isEmpty()) setUa(live.getUa());
        if (live.getHeader() != null && getHeader() == null) setHeader(live.getHeader());
        if (!live.getClick().isEmpty() && getClick().isEmpty()) setClick(live.getClick());
        if (!live.getOrigin().isEmpty() && getOrigin().isEmpty()) setOrigin(live.getOrigin());
        if (!live.getCatchup().isEmpty() && getCatchup().isEmpty()) setCatchup(live.getCatchup());
        if (!live.getReferer().isEmpty() && getReferer().isEmpty()) setReferer(live.getReferer());
        if (live.getEpg().contains("{") && !getEpg().startsWith("http")) setEpg(live.getEpgApi().replace("{id}", getTvgId()).replace("{name}", getTvgName()).replace("{epg}", getEpg()));
        if (live.getLogo().contains("{") && !getLogo().startsWith("http")) setLogo(live.getLogo().replace("{id}", getTvgId()).replace("{name}", getTvgName()).replace("{logo}", getLogo()));
    }

    public void setLine(String line) {
        setLine(getUrls().indexOf(line));
    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = Json.toMap(getHeader());
        if (!getUa().isEmpty()) headers.put(HttpHeaders.USER_AGENT, getUa());
        if (!getOrigin().isEmpty()) headers.put(HttpHeaders.ORIGIN, getOrigin());
        if (!getReferer().isEmpty()) headers.put(HttpHeaders.REFERER, getReferer());
        return headers;
    }

    public Channel copy(Channel item) {
        setCatchup(item.getCatchup());
        setReferer(item.getReferer());
        setTvgName(item.getTvgName());
        setHeader(item.getHeader());
        setNumber(item.getNumber());
        setOrigin(item.getOrigin());
        setFormat(item.getFormat());
        setParse(item.getParse());
        setClick(item.getClick());
        setTvgId(item.getTvgId());
        setLogo(item.getLogo());
        setName(item.getName());
        setUrls(item.getUrls());
        setData(item.getData());
        setDrm(item.getDrm());
        setEpg(item.getEpg());
        setUa(item.getUa());
        return this;
    }

    public Result result() {
        Result result = new Result();
        result.setClick(getClick());
        result.setUrl(Url.create().add(getUrl()));
        result.setHeader(Json.toObject(getHeaders()));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Channel)) return false;
        Channel it = (Channel) obj;
        if (!getName().isEmpty()) return getName().equals(it.getName());
        if (!getNumber().isEmpty()) return getNumber().equals(it.getNumber());
        return getName().equals(it.getName()) && getNumber().equals(it.getNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getNumber());
    }
}
