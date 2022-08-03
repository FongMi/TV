package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Persister;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

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

    @SerializedName("playUrl")
    private String playUrl;
    @SerializedName("header")
    private String header;
    @SerializedName("parse")
    private String parse;
    @SerializedName("flag")
    private String flag;
    @SerializedName("jx")
    private String jx;
    @SerializedName("url")
    private String url;

    public static Result fromJson(String str) {
        try {
            Type type = new TypeToken<LinkedHashMap<String, List<Filter>>>() {}.getType();
            Gson gson = new GsonBuilder().registerTypeAdapter(type, new FiltersAdapter()).create();
            Result result = gson.fromJson(str, Result.class);
            return result == null ? new Result() : result;
        } catch (Exception e) {
            e.printStackTrace();
            return new Result();
        }
    }

    public static Result fromXml(String str) {
        try {
            return new Persister().read(Result.class, str);
        } catch (Exception e) {
            e.printStackTrace();
            return new Result();
        }
    }

    public static Result objectFrom(String str) {
        return new Gson().fromJson(str, Result.class);
    }

    public List<Class> getTypes() {
        return types == null ? Collections.emptyList() : types;
    }

    public void setTypes(List<Class> types) {
        this.types = types;
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

    public String getPlayUrl() {
        return TextUtils.isEmpty(playUrl) ? "" : playUrl;
    }

    public void setPlayUrl(String playUrl) {
        this.playUrl = playUrl;
    }

    public String getHeader() {
        return TextUtils.isEmpty(header) ? "" : header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getParse() {
        return TextUtils.isEmpty(parse) ? "1" : parse;
    }

    public void setParse(String parse) {
        this.parse = parse;
    }

    public String getFlag() {
        return TextUtils.isEmpty(flag) ? "" : flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public String getJx() {
        return TextUtils.isEmpty(jx) ? "0" : jx;
    }

    public void setJx(String jx) {
        this.jx = jx;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    static class FiltersAdapter implements JsonDeserializer<LinkedHashMap<String, List<Filter>>> {

        @Override
        public LinkedHashMap<String, List<Filter>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            LinkedHashMap<String, List<Filter>> filterMap = new LinkedHashMap<>();
            JsonObject filters = json.getAsJsonObject();
            if (filters == null) return filterMap;
            for (String key : filters.keySet()) {
                List<Filter> items = new ArrayList<>();
                JsonElement element = filters.get(key);
                if (element.isJsonObject()) items.add(Filter.objectFrom(element));
                else for (JsonElement item : element.getAsJsonArray()) items.add(Filter.objectFrom(item));
                filterMap.put(key, items);
            }
            return filterMap;
        }
    }
}
