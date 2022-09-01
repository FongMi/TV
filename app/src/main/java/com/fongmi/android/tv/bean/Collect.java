package com.fongmi.android.tv.bean;

import java.util.ArrayList;
import java.util.List;

public class Collect {

    private final Site site;
    private final List<Vod> list;

    public static Collect all() {
        return new Collect(Site.get("all", "全部"), new ArrayList<>());
    }

    public static Collect create(List<Vod> list) {
        return new Collect(list.get(0).getSite(), list);
    }

    public Collect(Site site, List<Vod> list) {
        this.site = site;
        this.list = list;
    }

    public Site getSite() {
        return site;
    }

    public List<Vod> getList() {
        return list;
    }
}
