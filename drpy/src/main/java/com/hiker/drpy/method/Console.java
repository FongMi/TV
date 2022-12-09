package com.hiker.drpy.method;

import android.util.Log;

import com.github.tvbox.quickjs.JSMethod;

public class Console {

    private final String TAG = Console.class.getSimpleName();

    @JSMethod
    public void log(String msg) {
        Log.d(TAG, msg);
    }
}
