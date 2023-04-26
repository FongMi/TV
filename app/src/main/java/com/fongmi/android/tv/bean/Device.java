package com.fongmi.android.tv.bean;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.List;

@Entity(indices = @Index(value = {"uuid", "name"}, unique = true))
public class Device {

    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    private Integer id;
    @SerializedName("uuid")
    private String uuid;
    @SerializedName("name")
    private String name;
    @SerializedName("ip")
    private String ip;
    @SerializedName("type")
    private int type;

    public static Device get() {
        Device device = new Device();
        device.setUuid(Utils.getDeviceId());
        device.setName(Utils.getDeviceName());
        device.setIp(Server.get().getAddress());
        device.setType(Product.getDeviceType());
        return device;
    }

    public static Device objectFrom(String str) {
        return new Gson().fromJson(str, Device.class);
    }

    public Device() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUuid() {
        return TextUtils.isEmpty(uuid) ? "" : uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return TextUtils.isEmpty(ip) ? "" : ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isLeanback() {
        return getType() == 0;
    }

    public boolean isMobile() {
        return getType() == 1;
    }

    public boolean isDLNA() {
        return getType() == 2;
    }

    public boolean isApp() {
        return isLeanback() || isMobile();
    }

    public String getHost() {
        return isDLNA() ? getUuid() : Uri.parse(getIp()).getHost();
    }

    public Device save() {
        AppDatabase.get().getDeviceDao().insertOrUpdate(this);
        return this;
    }

    public static List<Device> getAll() {
        return AppDatabase.get().getDeviceDao().findAll();
    }

    public static void delete() {
        AppDatabase.get().getDeviceDao().delete();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Device)) return false;
        Device it = (Device) obj;
        return getUuid().equals(it.getUuid()) && getName().equals(it.getName());
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
