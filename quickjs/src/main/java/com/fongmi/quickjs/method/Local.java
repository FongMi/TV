package com.fongmi.quickjs.method;

import androidx.annotation.Keep;

import com.github.catvod.utils.Prefers;
import com.whl.quickjs.wrapper.JSMethod;

public class Local {

    @Keep
    @JSMethod
    public String get(String rule, String key) {
        return Prefers.getString("js_engine_" + rule + "_" + key);
    }

    @Keep
    @JSMethod
    public void set(String rule, String key, String value) {
        Prefers.put("js_engine_" + rule + "_" + key, value);
    }

    @Keep
    @JSMethod
    public void delete(String rule, String key) {
        Prefers.remove("js_engine_" + rule + "_" + key);
    }
}
