package com.fongmi.android.tv.utils;

import com.android.cast.dlna.dmc.DLNACastManager;
import com.fongmi.android.tv.bean.Device;

import java.util.LinkedHashSet;
import java.util.Set;

public class DLNADevice {

    private final Set<org.fourthline.cling.model.meta.Device<?, ?, ?>> devices;

    private static class Loader {
        static final DLNADevice INSTANCE = new DLNADevice();
    }

    public static DLNADevice get() {
        return Loader.INSTANCE;
    }

    private DLNADevice() {
        this.devices = new LinkedHashSet<>();
    }

    public Device add(org.fourthline.cling.model.meta.Device<?, ?, ?> item) {
        devices.add(item);
        return Device.get(item);
    }

    public Device remove(org.fourthline.cling.model.meta.Device<?, ?, ?> item) {
        devices.remove(item);
        return Device.get(item);
    }

    public void disconnect() {
        devices.forEach(DLNACastManager.INSTANCE::disconnectDevice);
        devices.clear();
    }

    public org.fourthline.cling.model.meta.Device<?, ?, ?> find(Device item) {
        return devices.stream().filter(d -> d.getIdentity().getUdn().getIdentifierString().equals(item.getUuid())).findFirst().orElse(null);
    }
}
