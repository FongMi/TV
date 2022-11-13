package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.gson.FilterAdapter;
import com.fongmi.android.tv.utils.Json;
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
    @SerializedName("sub")
    private String sub;

    public static Result fromJson(String str) {
        try {
            Result result = FilterAdapter.gson().fromJson(str, Result.class);
            return result == null ? empty() : result;
        } catch (Exception e) {
            return empty();
        }
    }

    public static Result fromXml(String str) {
        try {
            return new Persister().read(Result.class, str);
        } catch (Exception e) {
            return empty();
        }
    }

    public static Result fromObject(JSONObject object) {
        return objectFrom(object.toString());
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

    public String getSub() {
        return TextUtils.isEmpty(sub) ? "" : sub;
    }

    public Map<String, String> getHeaders() {
        return Json.toMap(getHeader());
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
