package com.fongmi.android.tv.event;

import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.bean.History;

import org.greenrobot.eventbus.EventBus;

public class CastEvent {

    private final Config config;
    private final Device device;
    private final History history;

    public static void post(Config config, Device device, History history) {
        EventBus.getDefault().post(new CastEvent(config, device, history));
    }

    public CastEvent(Config config, Device device, History history) {
        this.config = config;
        this.device = device;
        this.history = history;
    }

    public History getHistory() {
        return history;
    }

    public Device getDevice() {
        return device;
    }

    public Config getConfig() {
        return config;
    }
}
