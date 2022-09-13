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

    public String getTypeId() {
        return typeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getTypeFlag() {
        return TextUtils.isEmpty(typeFlag) ? "" : typeFlag;
    }

    public void setFilter(Boolean filter) {
        this.filter = filter;
    }

    public Boolean getFilter() {
        return filter;
    }

    public Class toggleFilter() {
        setFilter(!getFilter());
        return this;
    }

    public int getIcon() {
        return getFilter() == null ? 0 : getFilter() ? R.drawable.ic_filter_off_small : R.drawable.ic_filter_on_small;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Class)) return false;
        Class it = (Class) obj;
        return getTypeId().equals(it.getTypeId());
    }
}
