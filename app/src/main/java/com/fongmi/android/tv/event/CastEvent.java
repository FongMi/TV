package com.fongmi.android.tv.event;

import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.utils.FileUtil;

import org.greenrobot.eventbus.EventBus;

public class CastEvent {

    private final History history;
    private final Device device;
    private String config;

    public static void post(String device, String config, String history) {
        EventBus.getDefault().post(new CastEvent(device, config, history));
    }

    public CastEvent(String device, String config, String history) {
        this.history = History.objectFrom(history);
        this.device = Device.objectFrom(device);
        this.config = config;
        checkConfig();
    }

    public History getHistory() {
        return history;
    }

    public Device getDevice() {
        return device;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    private void checkConfig() {
        if (!config.startsWith("file")) return;
        if (FileUtil.getLocal(config).exists()) return;
        setConfig(device.getIp() + "/" + config);
    }
}
