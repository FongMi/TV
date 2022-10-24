package com.fongmi.android.tv.impl;

import com.fongmi.android.tv.bean.Config;

public interface ConfigCallback {

    void setVodConfig(Config config);

    void setLiveConfig(Config config);
}
