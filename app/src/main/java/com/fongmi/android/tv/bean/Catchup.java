package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Catchup {

    @SerializedName("type")
    private String type;
    @SerializedName("days")
    private String days;
    @SerializedName("regex")
    private String regex;
    @SerializedName("source")
    private String source;
    @SerializedName("replace")
    private String replace;

    public static Catchup PLTV() {
        Catchup item = new Catchup();
        item.setDays("7");
        item.setType("append");
        item.setRegex("/PLTV/");
        item.setReplace("/PLTV/,/TVOD/");
        item.setSource("?playseek=${(b)yyyyMMddHHmmss}-${(e)yyyyMMddHHmmss}");
        return item;
    }

    public static Catchup create() {
        return new Catchup();
    }

    public static Catchup decide(Catchup major, Catchup minor) {
        if (!major.isEmpty()) return major;
        if (!minor.isEmpty()) return minor;
        return null;
    }

    public String getType() {
        return TextUtils.isEmpty(type) ? "" : type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDays() {
        return TextUtils.isEmpty(days) ? "" : days;
    }

    public void setDays(String days) {
        this.days = days;
    }

    public String getRegex() {
        return TextUtils.isEmpty(regex) ? "" : regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getReplace() {
        return TextUtils.isEmpty(replace) ? "" : replace;
    }

    public void setReplace(String replace) {
        this.replace = replace;
    }

    public String getSource() {
        return TextUtils.isEmpty(source) ? "" : source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean match(String url) {
        return url.contains(getRegex()) || Pattern.compile(getRegex()).matcher(url).find();
    }

    public boolean isEmpty() {
        return getSource().isEmpty();
    }

    private boolean isAppend() {
        return getType().equals("append");
    }

    private boolean isDefault() {
        return getType().equals("default");
    }

    private String append(String url, String result) {
        String[] splits = getReplace().split(",", 2);
        if (splits.length == 2) url = url.replaceAll(splits[0], splits[1]);
        if (!TextUtils.isEmpty(URI.create(url).getQuery())) result = result.replace("?", "&");
        return url + result;
    }

    public String format(String url, EpgData data) {
        String result = getSource();
        if (data.isInRange()) return url;
        Matcher matcher = Pattern.compile("(\\$?\\{[^}]*\\})").matcher(result);
        while (matcher.find()) result = result.replace(matcher.group(1), format(matcher.group(1), data.getStartTime(), data.getEndTime()));
        return isDefault() ? result : append(url, result);
    }

    private String format(String group, long start, long end) {
        Matcher matcher = Pattern.compile("\\{([^}]+)\\}").matcher(group);
        if (!matcher.find()) return "";
        String tag = matcher.group(1);
        if (tag.startsWith("(b")) return new SimpleDateFormat(tag.split("\\)")[1], Locale.getDefault()).format(start);
        if (tag.startsWith("(e")) return new SimpleDateFormat(tag.split("\\)")[1], Locale.getDefault()).format(end);
        if (tag.startsWith("utcend:")) return String.valueOf(end / 1000);
        if (tag.startsWith("utc:")) return String.valueOf(start / 1000);
        return "";
    }
}
