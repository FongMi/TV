package com.fongmi.android.tv.event;

import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.History;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class SyncEvent {

    private final Config config;
    private final List<History> history;

    public static void post(Config config, List<History> history) {
        EventBus.getDefault().post(new SyncEvent(config, history));
    }

    public SyncEvent(Config config, List<History> history) {
        this.config = config;
        this.history = history;
    }

    public Config getConfig() {
        return config;
    }

    public List<History> getHistory() {
        return history;
    }
}
