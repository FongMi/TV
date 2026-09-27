package com.fongmi.android.tv.bean;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.gson.HeaderAdapter;
import com.fongmi.android.tv.utils.Formatters;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.utils.Trans;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
    @JsonAdapter(HeaderAdapter.class)
    private Map<String, String> header;
    @SerializedName("parse")
    private Integer parse;
    @SerializedName("drm")
    private Drm drm;

    private boolean selected;
    private Group group;
    private String show;
    private int index;
    private List<Epg> dataList;
    private transient boolean epgFromLive;

    public Channel() {
    }

    public Channel(String name) {
        this.name = name;
    }

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

    public Channel setNumber(int number) {
        setNumber(String.format(Locale.getDefault(), "%03d", number));
        return this;
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
        this.epgFromLive = false;
    }

    public boolean hasEpgOverride() {
        return !epgFromLive && getEpg().startsWith("http");
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShow() {
        return TextUtils.isEmpty(show) ? getName() : show;
    }

    public void setShow(String show) {
        this.show = show;
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

    public Map<String, String> getHeader() {
        return header == null ? new HashMap<>() : header;
    }

    public void setHeader(Map<String, String> header) {
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

    public Epg getData() {
        return getData(ZoneId.systemDefault());
    }

    public void setData(Epg data) {
        if (dataList == null) dataList = new ArrayList<>();
        dataList.removeIf(e -> e.equal(data.getDate()));
        dataList.add(data);
    }

    public Epg getData(ZoneId zoneId) {
        return getData(zoneId, LocalDate.now(zoneId));
    }

    Epg getData(ZoneId zoneId, LocalDate day) {
        String date = day.format(Formatters.DATE);
        List<Epg> days = getDataList();
        Epg today = days.stream().filter(e -> e.equal(date)).findFirst().orElseGet(() -> Epg.create(getTvgId(), date));
        long midnight = day.atStartOfDay(zoneId).toInstant().toEpochMilli();
        List<EpgData> list = new ArrayList<>();
        for (Epg epg : days) {
            if (epg.equal(date)) continue;
            for (EpgData item : epg.getList()) {
                if (item.getStartTime() < midnight && item.getEndTime() > midnight) list.add(item);
            }
        }
        if (list.isEmpty()) return today;
        list.addAll(today.getList());
        list.sort(Comparator.comparingLong(EpgData::getStartTime));
        Epg result = Epg.create(getTvgId(), date);
        result.setList(list);
        return result;
    }

    public List<Epg> getDataList() {
        return dataList == null ? Collections.emptyList() : dataList;
    }

    public void setDataList(List<Epg> list) {
        this.dataList = new ArrayList<>(list);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = Math.max(index, 0);
    }

    public void setIndex(String line) {
        for (int i = 0; i < getUrls().size(); i++) {
            String url = getUrls().get(i);
            if (url.equals(line) || (url.contains("$") && line.equals(url.split("\\$")[0]))) {
                setIndex(i);
                break;
            }
        }
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
        ImgUtil.load(getName(), getLogo(), view, false);
    }

    public void switchLine(boolean next) {
        List<?> urls = getUrls();
        if (urls.isEmpty()) return;
        int size = urls.size();
        int step = next ? 1 : -1;
        setIndex((getIndex() + step + size) % size);
    }

    public String getCurrent() {
        if (getUrls().isEmpty()) return "";
        String url = getUrls().get(getIndex());
        return (getDrm() != null) ? url : url.split("\\$")[0];
    }

    public boolean isOnly() {
        return getUrls().size() == 1;
    }

    public boolean isLast() {
        return getUrls().isEmpty() || getIndex() == getUrls().size() - 1;
    }

    public boolean isRtsp() {
        return getCurrent().startsWith("rtsp");
    }

    public boolean hasCatchup() {
        if (getCatchup().isEmpty() && getCurrent().contains("/PLTV/")) setCatchup(Catchup.PLTV());
        if (!getCatchup().getRegex().isEmpty()) return getCatchup().match(getCurrent());
        return !getCatchup().isEmpty();
    }

    public String getLine() {
        if (getUrls().size() <= 1) return "";
        String[] sp = getUrls().get(getIndex()).split("\\$");
        if (sp.length > 1 && !sp[1].isEmpty()) return sp[1];
        return ResUtil.getString(R.string.live_line, getIndex() + 1);
    }

    public Channel group(Group group) {
        setGroup(group);
        return this;
    }

    public void live(Live live) {
        if (!live.getUa().isEmpty() && getUa().isEmpty()) setUa(live.getUa());
        if (!live.getClick().isEmpty() && getClick().isEmpty()) setClick(live.getClick());
        if (!live.getHeader().isEmpty() && getHeader().isEmpty()) setHeader(live.getHeader());
        if (!live.getOrigin().isEmpty() && getOrigin().isEmpty()) setOrigin(live.getOrigin());
        if (!live.getCatchup().isEmpty() && getCatchup().isEmpty()) setCatchup(live.getCatchup());
        if (!live.getReferer().isEmpty() && getReferer().isEmpty()) setReferer(live.getReferer());
        if (live.getEpg().contains("{") && !getEpg().startsWith("http")) {
            setEpg(live.getEpgApi().replace("{id}", getTvgId()).replace("{name}", getTvgName()).replace("{epg}", getEpg()));
            epgFromLive = true;
        }
        if (live.getLogo().contains("{") && !getLogo().startsWith("http")) setLogo(live.getLogo().replace("{id}", getTvgId()).replace("{name}", getTvgName()).replace("{logo}", getLogo()));
    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>(getHeader());
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
        setShow(item.getShow());
        setUrls(item.getUrls());
        setDataList(item.getDataList());
        setDrm(item.getDrm());
        epg = item.getEpg();
        epgFromLive = item.epgFromLive;
        setUa(item.getUa());
        return this;
    }

    public Result result() {
        Result result = new Result();
        result.setDrm(getDrm());
        result.setUrl(getCurrent());
        result.setClick(getClick());
        result.setParse(getParse());
        result.setFormat(getFormat());
        result.setHeader(getHeaders());
        return result;
    }

    public Channel trans() {
        if (Trans.pass()) return this;
        this.show = Trans.s2t(name);
        return this;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Channel it)) return false;
        String name1 = getName(), name2 = it.getName();
        String number1 = getNumber(), number2 = it.getNumber();
        if (!name1.isEmpty() && !name2.isEmpty()) return Objects.equals(name1, name2);
        if (!number1.isEmpty() && !number2.isEmpty()) return Objects.equals(number1, number2);
        return false;
    }

    @Override
    public int hashCode() {
        String name = getName(), number = getNumber();
        if (!name.isEmpty()) return Objects.hash(name);
        if (!number.isEmpty()) return Objects.hash(number);
        return 0;
    }
}
