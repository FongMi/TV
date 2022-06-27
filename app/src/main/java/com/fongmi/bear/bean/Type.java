package com.fongmi.bear.bean;

import com.google.gson.annotations.SerializedName;

public class Type {

    @SerializedName("type_id")
    private String typeId;
    @SerializedName("type_name")
    private String typeName;

    public String getTypeId() {
        return typeId;
    }

    public String getTypeName() {
        return typeName;
    }
}
