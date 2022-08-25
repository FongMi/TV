package com.fongmi.android.tv.impl;

import com.fongmi.android.tv.bean.Site;

public interface SettingCallback {

    void setConfig(String url);

    void setSite(Site item);
}
