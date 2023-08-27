package com.fongmi.android.tv.bean;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class Collect {

    private boolean activated;
    private List<Vod> list;
    private Site site;
    private int page;

    public static Collect all() {
        Collect item = new Collect(Site.get("all", ResUtil.getString(R.string.all)), new ArrayList<>());
        item.setActivated(true);
        return item;
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

    public int getPage() {
        return Math.max(1, page);
    }

    public void setPage(int page) {
        this.page = page;
    }
}
