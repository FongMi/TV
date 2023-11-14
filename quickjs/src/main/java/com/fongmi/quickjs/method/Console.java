package com.fongmi.quickjs.method;

import com.orhanobut.logger.Logger;
import com.whl.quickjs.wrapper.QuickJSContext;

public class Console implements QuickJSContext.Console {

    private static final String TAG = "quickjs";

    @Override
    public void log(String info) {
        Logger.t(TAG).d(info);
    }

    @Override
    public void info(String info) {
        Logger.t(TAG).i(info);
    }

    @Override
    public void warn(String info) {
        Logger.t(TAG).w(info);
    }

    @Override
    public void error(String info) {
        Logger.t(TAG).e(info);
    }
}