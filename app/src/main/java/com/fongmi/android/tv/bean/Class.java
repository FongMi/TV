package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

import java.util.Collections;
import java.util.List;

@Root(strict = false)
public class Class {

    @Attribute(name = "id", required = false)
    @SerializedName("type_id")
    private String typeId;

    @Text
    @SerializedName("type_name")
    private String typeName;

    @SerializedName("type_flag")
    private String typeFlag;

    @SerializedName("filters")
    private List<Filter> filters;

    private Boolean filter;
    private boolean activated;

    public String getTypeId() {
        return TextUtils.isEmpty(typeId) ? "" : typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getTypeName() {
        return TextUtils.isEmpty(typeName) ? "" : typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeFlag() {
        return TextUtils.isEmpty(typeFlag) ? "" : typeFlag;
    }

    public void setTypeFlag(String typeFlag) {
        this.typeFlag = typeFlag;
    }

    public List<Filter> getFilters() {
        return filters == null ? Collections.emptyList() : filters;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    public void setFilter(Boolean filter) {
        this.filter = filter;
    }

    public Boolean getFilter() {
        return filter;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public boolean toggleFilter() {
        setFilter(!getFilter());
        return getFilter();
    }

    public boolean isHome() {
        return getTypeId().equals("home");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Class)) return false;
        Class it = (Class) obj;
        return getTypeId().equals(it.getTypeId());
    }
}
