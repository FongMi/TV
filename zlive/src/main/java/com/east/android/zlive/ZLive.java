package com.east.android.zlive;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface ZLive extends Library {

    ZLive INSTANCE = Native.load("core", ZLive.class);

    void OnLiveStart(long port);

    void OnLiveStop();
}