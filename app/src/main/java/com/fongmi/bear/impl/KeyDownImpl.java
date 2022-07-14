package com.fongmi.bear.impl;

public interface KeyDownImpl {

    void onSeek(boolean forward);

    void onKeyVertical(boolean up);

    void onKeyLeft();

    void onKeyRight();

    void onKeyCenter();

    void onKeyMenu();

    void onKeyBack();

    void onLongPress();
}
