package com.hiker.drpy.method;

import android.util.Log;

import androidx.annotation.Keep;

import com.whl.quickjs.wrapper.JSMethod;

public class Console {

    private final String TAG = Console.class.getSimpleName();

    @Keep
    @JSMethod
    public void log(Object msg) {
        Log.d(TAG, String.valueOf(msg));
    }
}
