package com.fongmi.quickjs.method;

import androidx.annotation.Keep;

import com.orhanobut.logger.Logger;
import com.whl.quickjs.wrapper.JSMethod;

public class Console {

    private final String TAG = "quickjs";

    @Keep
    @JSMethod
    public void debug(Object array) {
        Logger.t(TAG).d(String.valueOf(array));
    }

    @Keep
    @JSMethod
    public void error(Object array) {
        Logger.t(TAG).e(String.valueOf(array));
    }

    @Keep
    @JSMethod
    public void warn(Object array) {
        Logger.t(TAG).w(String.valueOf(array));
    }

    @Keep
    @JSMethod
    public void info(Object array) {
        Logger.t(TAG).i(String.valueOf(array));
    }

    @Keep
    @JSMethod
    public void log(Object array) {
        Logger.t(TAG).v(String.valueOf(array));
    }
}
