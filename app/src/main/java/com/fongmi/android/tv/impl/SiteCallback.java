package com.fongmi.android.tv.impl;

import com.fongmi.android.tv.bean.Site;

public interface SiteCallback {

    void setSite(Site item);

    void onChanged();
}
