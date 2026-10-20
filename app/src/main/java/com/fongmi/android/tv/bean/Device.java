package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.impl.Diffable;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Prefers;
import com.github.catvod.utils.UriUtil;
import com.google.gson.annotations.SerializedName;

import org.jupnp.model.meta.RemoteDevice;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Entity(indices = @Index(value = {"uuid", "name"}, unique = true))
public class Device implements Diffable<Device>, Comparable<Device> {

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

    @Ignore
    @SerializedName("serial")
    private String serial;
    @Ignore
    @SerializedName("eth")
    private String eth;
    @Ignore
    @SerializedName("wlan")
    private String wlan;
    @Ignore
    @SerializedName("time")
    private long time;

    public static Device get() {
        Device device = new Device();
        device.setTime(App.time());
        device.setSerial(Util.getSerial());
        device.setEth(Util.getMac("eth0"));
        device.setWlan(Util.getMac("wlan0"));
        device.setUuid(Util.getAndroidId());
        device.setName(Util.getDeviceName());
        device.setIp(Server.get().getAddress());
        device.setType(Product.getDeviceType());
        return device;
    }

    public static Device get(RemoteDevice item) {
        Device device = new Device();
        device.setUuid(item.getIdentity().getUdn().getIdentifierString());
        device.setName(item.getDetails().getFriendlyName());
        device.setType(2);
        return device;
    }

    public static Device objectFrom(String str) {
        return App.gson().fromJson(str, Device.class);
    }

    public static List<Device> getAll() {
        return AppDatabase.get().getDeviceDao().findAll();
    }

    public static void delete() {
        AppDatabase.get().getDeviceDao().delete();
    }

    public static void replaceAppDevices(List<Device> devices) {
        AppDatabase.get().getDeviceDao().replaceAppDevices(devices);
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

    public boolean canRename() {
        return isApp() && !getUuid().isBlank() && !getUuid().matches("0+");
    }

    public String getAlias() {
        return canRename() ? Prefers.getString("device_alias_" + getUuid()) : "";
    }

    public String getDisplayName() {
        String alias = getAlias();
        return alias.isEmpty() ? getName() : alias;
    }

    public void setAlias(String alias) {
        if (!canRename()) throw new IllegalStateException("Invalid device identity");
        String key = "device_alias_" + getUuid();
        if (alias.trim().isEmpty()) Prefers.remove(key);
        else Prefers.put(key, alias.trim());
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

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public void setEth(String eth) {
        this.eth = eth;
    }

    public void setWlan(String wlan) {
        this.wlan = wlan;
    }

    public void setTime(long time) {
        this.time = time;
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
        return isDLNA() ? getUuid() : UriUtil.host(getIp());
    }

    public Device save() {
        AppDatabase.get().getDeviceDao().insertOrUpdate(this);
        return this;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Device it)) return false;
        return Objects.equals(getUuid(), it.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid());
    }

    @NonNull
    @Override
    public String toString() {
        return App.gson().toJson(this);
    }

    @Override
    public boolean isSameItem(Device other) {
        return equals(other);
    }

    @Override
    public boolean isSameContent(Device other) {
        return getName().equals(other.getName()) && getType() == other.getType() && getIp().equals(other.getIp());
    }

    @Override
    public int compareTo(Device other) {
        return Comparator.comparingInt(Device::getType).thenComparing(Device::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(Device::getUuid).compare(this, other);
    }
}
