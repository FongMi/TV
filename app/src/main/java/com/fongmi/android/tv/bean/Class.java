package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.fongmi.android.tv.R;
import com.google.gson.annotations.SerializedName;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

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

    private Boolean filter;
    private boolean activated;

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public String getTypeName() {
        return typeName;
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

    public int getIcon() {
        return getFilter() == null ? 0 : getFilter() ? R.drawable.ic_type_filter_off : R.drawable.ic_type_filter_on;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Class)) return false;
        Class it = (Class) obj;
        return getTypeId().equals(it.getTypeId());
    }
}
