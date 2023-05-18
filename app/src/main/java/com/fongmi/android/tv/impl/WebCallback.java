package com.fongmi.android.tv.impl;

import java.util.Map;

public interface WebCallback {

    void onSniffSuccess(Map<String, String> headers, String url, String from);

    void onSniffError();
}
