package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

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

    public static Catchup PLTV() {
        Catchup item = new Catchup();
        item.setDays("7");
        item.setType("append");
        item.setRegex("/PLTV/");
        item.setSource("?playseek=${(b)yyyyMMddHHmmss}-${(e)yyyyMMddHHmmss}");
        return item;
    }

    public static Catchup create() {
        return new Catchup();
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

    public String format(EpgData data) {
        String result = getSource();
        Matcher matcher = Pattern.compile("(\\$\\{[^}]*\\})").matcher(result);
        while (matcher.find()) result = result.replace(matcher.group(1), data.format(matcher.group(1)));
        return result;
    }
}
