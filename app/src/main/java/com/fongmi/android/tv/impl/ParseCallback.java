package com.fongmi.android.tv.impl;

import java.util.List;
import java.util.Map;

public interface ParseCallback {

    void onParseSuccess(List<String> ads, Map<String, String> headers, String url, String from);

    void onParseError();
}
