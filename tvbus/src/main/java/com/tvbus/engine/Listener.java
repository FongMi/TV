package com.tvbus.engine;

public interface Listener {

    void onInited(String result);

    void onStart(String result);

    void onPrepared(String result);

    void onInfo(String result);

    void onStop(String result);

    void onQuit(String result);
}
