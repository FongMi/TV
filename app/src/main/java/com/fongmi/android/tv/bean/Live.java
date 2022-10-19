package com.fongmi.android.tv.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Live {

    @SerializedName("name")
    private String name;
    @SerializedName("groups")
    private List<Group> groups;

    public Live(String name, List<Group> groups) {
        this.name = name;
        this.groups = groups;
    }

    public String getName() {
        return name;
    }

    public List<Group> getGroups() {
        return groups;
    }
}
