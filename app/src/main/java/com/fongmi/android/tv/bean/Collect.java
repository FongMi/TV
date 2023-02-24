package com.fongmi.android.tv.bean;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class Collect {

    private final Site site;
    private final List<Vod> list;
    private boolean activated;

    public static Collect all() {
        Collect all = new Collect(Site.get("all", ResUtil.getString(R.string.all)), new ArrayList<>());
        all.setActivated(true);
        return all;
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

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }
}
