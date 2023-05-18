package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.gson.FilterAdapter;
import com.fongmi.android.tv.utils.Json;
import com.fongmi.android.tv.utils.Trans;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Persister;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Root(name = "rss", strict = false)
public class Result {

    @Path("class")
    @ElementList(entry = "ty", required = false, inline = true)
    @SerializedName("class")
    private List<Class> types;

    @Path("list")
    @ElementList(entry = "video", required = false, inline = true)
    @SerializedName("list")
    private List<Vod> list;

    @SerializedName("filters")
    private LinkedHashMap<String, List<Filter>> filters;

    @SerializedName("header")
    private JsonElement header;
    @SerializedName("playUrl")
    private String playUrl;
    @SerializedName("jxFrom")
    private String jxFrom;
    @SerializedName("parse")
    private Integer parse;
    @SerializedName("jx")
    private Integer jx;
    @SerializedName("flag")
    private String flag;
    @SerializedName("url")
    private String url;
    @SerializedName("key")
    private String key;
    @SerializedName("subs")
    private List<Sub> subs;

    public static Result fromJson(String str) {
        try {
            Result result = FilterAdapter.gson().fromJson(str, Result.class);
            return result == null ? empty() : result.trans();
        } catch (Exception e) {
            return empty();
        }
    }

    public static Result fromXml(String str) {
        try {
            return new Persister().read(Result.class, str).trans();
        } catch (Exception e) {
            return empty();
        }
    }

    public static Result fromObject(JSONObject object) {
        return object == null ? empty() : objectFrom(object.toString());
    }

    public static Result objectFrom(String str) {
        try {
            return new Gson().fromJson(str, Result.class);
        } catch (Exception e) {
            return empty();
        }
    }

    public static Result empty() {
        return new Result();
    }

    public static Result folder(Vod item) {
        Result result = new Result();
        Class type = new Class();
        type.setTypeFlag("1");
        type.setTypeId(item.getVodId());
        type.setTypeName(item.getVodName());
        result.setTypes(List.of(type));
        return result;
    }

    public static Result list(List<Vod> items) {
        Result result = new Result();
        result.setList(items);
        return result;
    }

    public List<Class> getTypes() {
        return types == null ? Collections.emptyList() : types;
    }

    public void setTypes(List<Class> types) {
        if (types.size() > 0) this.types = types;
    }

    public List<Vod> getList() {
        return list == null ? Collections.emptyList() : list;
    }

    public void setList(List<Vod> list) {
        this.list = list;
    }

    public LinkedHashMap<String, List<Filter>> getFilters() {
        return filters == null ? new LinkedHashMap<>() : filters;
    }

    public JsonElement getHeader() {
        return header;
    }

    public String getPlayUrl() {
        return TextUtils.isEmpty(playUrl) ? "" : playUrl;
    }

    public void setPlayUrl(String playUrl) {
        this.playUrl = playUrl;
    }

    public String getJxFrom() {
        return TextUtils.isEmpty(jxFrom) ? "" : jxFrom;
    }

    public Integer getParse() {
        return getParse(0);
    }

    public Integer getParse(Integer def) {
        return parse == null ? def : parse;
    }

    public void setParse(Integer parse) {
        this.parse = parse;
    }

    public Integer getJx() {
        return jx == null ? 0 : jx;
    }

    public void setJx(Integer jx) {
        this.jx = jx;
    }

    public String getFlag() {
        return TextUtils.isEmpty(flag) ? "" : flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getKey() {
        return TextUtils.isEmpty(key) ? "" : key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<Sub> getSubs() {
        return subs == null ? Collections.emptyList() : subs;
    }

    public String getRealUrl() {
        return getPlayUrl() + getUrl();
    }

    public Map<String, String> getHeaders() {
        return Json.toMap(getHeader());
    }

    public Result clear() {
        getList().clear();
        return this;
    }

    public Result trans() {
        if (Trans.pass()) return this;
        for (Class type : getTypes()) type.trans();
        for (Vod vod : getList()) vod.trans();
        for (Sub sub : getSubs()) sub.trans();
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
