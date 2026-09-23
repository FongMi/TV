package com.fongmi.android.tv.bean;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.fongmi.android.tv.App;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Rule {

    @SerializedName("name")
    private String name;
    @SerializedName("hosts")
    private List<String> hosts;
    @SerializedName("regex")
    private List<String> regex;
    @SerializedName("script")
    private List<String> script;
    @SerializedName("exclude")
    private List<String> exclude;
    private transient volatile List<Pattern> regexPatterns;
    private transient volatile List<Pattern> excludePatterns;

    public Rule(String name) {
        this.name = name;
    }

    public static Rule create(String name) {
        return new Rule(name);
    }

    public static Rule empty() {
        return new Rule("");
    }

    public static List<Rule> arrayFrom(JsonElement element) {
        Type listType = TypeToken.getParameterized(List.class, Rule.class).getType();
        List<Rule> items = App.gson().fromJson(element, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public List<String> getHosts() {
        return hosts == null ? Collections.emptyList() : hosts;
    }

    public List<String> getRegex() {
        return regex == null ? Collections.emptyList() : regex;
    }

    public List<String> getScript() {
        return script == null ? Collections.emptyList() : script;
    }

    public List<String> getExclude() {
        return exclude == null ? Collections.emptyList() : exclude;
    }

    public List<Pattern> getRegexPatterns() {
        List<Pattern> result = regexPatterns;
        if (result == null) regexPatterns = result = compile(getRegex());
        return result;
    }

    public List<Pattern> getExcludePatterns() {
        List<Pattern> result = excludePatterns;
        if (result == null) excludePatterns = result = compile(getExclude());
        return result;
    }

    private static List<Pattern> compile(List<String> expressions) {
        List<Pattern> result = new ArrayList<>(expressions.size());
        for (String expression : expressions) {
            try {
                result.add(Pattern.compile(expression));
            } catch (PatternSyntaxException e) {
                Log.w("Rule", "Invalid regex", e);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Rule it)) return false;
        return getName().equals(it.getName());
    }
}
