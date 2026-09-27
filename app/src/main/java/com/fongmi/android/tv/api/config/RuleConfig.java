package com.fongmi.android.tv.api.config;

import com.fongmi.android.tv.bean.Rule;

import java.util.ArrayList;
import java.util.List;

public class RuleConfig {

    private List<String> ads = List.of();
    private List<Rule> rules = List.of();
    private volatile boolean dirty;

    public static RuleConfig get() {
        return Loader.INSTANCE;
    }

    public List<String> getAds() {
        if (dirty) merge();
        return ads;
    }

    public List<Rule> getRules() {
        if (dirty) merge();
        return rules;
    }

    synchronized void invalidate() {
        dirty = true;
    }

    private synchronized void merge() {
        if (!dirty) return;
        List<String> ads = new ArrayList<>(VodConfig.get().getAds());
        ads.addAll(LiveConfig.get().getAds());
        this.ads = ads;
        List<Rule> rules = new ArrayList<>(VodConfig.get().getRules());
        rules.addAll(LiveConfig.get().getRules());
        this.rules = rules;
        dirty = false;
    }

    private static class Loader {
        static volatile RuleConfig INSTANCE = new RuleConfig();
    }
}
