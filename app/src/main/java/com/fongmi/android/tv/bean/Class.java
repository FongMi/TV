package com.fongmi.android.tv.bean;

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

    private Boolean filter;

    public String getTypeId() {
        return typeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setFilter(Boolean filter) {
        this.filter = filter;
    }

    public Boolean getFilter() {
        return filter;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Class)) return false;
        Class it = (Class) obj;
        return getTypeId().equals(it.getTypeId());
    }
}
