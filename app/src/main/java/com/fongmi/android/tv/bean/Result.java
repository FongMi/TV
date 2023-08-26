package com.fongmi.android.tv.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.gson.FilterAdapter;
import com.fongmi.android.tv.gson.MsgAdapter;
import com.fongmi.android.tv.gson.UrlAdapter;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Trans;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Persister;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Root(name = "rss", strict = false)
public class Result implements Parcelable {

    @Path("class")
    @ElementList(entry = "ty", required = false, inline = true)
    @SerializedName("class")
    private List<Class> types;

    @Path("list")
    @ElementList(entry = "video", required = false, inline = true)
    @SerializedName("list")
    private List<Vod> list;

    @SerializedName("filters")
    @JsonAdapter(FilterAdapter.class)
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
    @SerializedName("format")
    private String format;
    @SerializedName("url")
    @JsonAdapter(UrlAdapter.class)
    private Url url;
    @SerializedName("key")
    private String key;
    @SerializedName("subs")
    private List<Sub> subs;
    @SerializedName("pagecount")
    private Integer pagecount;
    @SerializedName("code")
    private Integer code;
    @JsonAdapter(MsgAdapter.class)
    @SerializedName("msg")
    private String msg;

    public static Result objectFrom(String str) {
        try {
            return new Gson().fromJson(str, Result.class);
        } catch (Exception e) {
            return empty();
        }
    }

    public static Result fromJson(String str) {
        Result result = objectFrom(str);
        return result == null ? empty() : result.trans();
    }

    public static Result fromXml(String str) {
        try {
            return new Persister().read(Result.class, str).trans();
        } catch (Exception e) {
            return empty();
        }
    }

    public static Result fromType(int type, String str) {
        return type == 0 ? fromXml(str) : fromJson(str);
    }

    public static Result fromObject(JSONObject object) {
        return object == null ? empty() : objectFrom(object.toString());
    }

    public static Result empty() {
        return new Result();
    }

    public static Result error(String msg) {
        Result result = new Result();
        result.setMsg(msg);
        return result;
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

    public static Result vod(Vod item) {
        return list(Arrays.asList(item));
    }

    public Result() {
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

    public String getFormat() {
        return format;
    }

    public String getFlag() {
        return TextUtils.isEmpty(flag) ? "" : flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public Url getUrl() {
        return url == null ? Url.create() : url;
    }

    public void setUrl(Url url) {
        this.url = url;
    }

    public void setUrl(String url) {
        this.url = getUrl().replace(url);
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

    public Integer getPageCount() {
        return pagecount == null ? 0 : pagecount;
    }

    public Integer getCode() {
        return code == null ? 0 : code;
    }

    public String getMsg() {
        return TextUtils.isEmpty(msg) || getCode() != 0 ? "" : msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean hasMsg() {
        return getMsg().length() > 0;
    }

    public String getRealUrl() {
        return getPlayUrl() + getUrl().v();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(this.types);
        dest.writeTypedList(this.list);
        dest.writeString(App.gson().toJson(this.filters));
    }

    protected Result(Parcel in) {
        this.types = new ArrayList<>();
        in.readList(this.types, Class.class.getClassLoader());
        this.list = in.createTypedArrayList(Vod.CREATOR);
        Type listType = new TypeToken<LinkedHashMap<String, List<Filter>>>() {}.getType();
        this.filters = App.gson().fromJson(in.readString(), listType);
    }

    public static final Creator<Result> CREATOR = new Creator<>() {
        @Override
        public Result createFromParcel(Parcel source) {
            return new Result(source);
        }

        @Override
        public Result[] newArray(int size) {
            return new Result[size];
        }
    };
}
