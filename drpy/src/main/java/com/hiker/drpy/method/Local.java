package com.hiker.drpy.method;

import androidx.annotation.Keep;

import com.whl.quickjs.wrapper.JSMethod;

import java.util.HashMap;
import java.util.Map;

public class Local {

    private final Map<String, String> maps = new HashMap<>();

    @Keep
    @JSMethod
    public String get(String R_KEY, String k) {
        return maps.get("js_engine_" + R_KEY + "_" + k);
    }

    @Keep
    @JSMethod
    public void set(String R_KEY, String k, String v) {
        maps.put("js_engine_" + R_KEY + "_" + k, v);
    }

    @Keep
    @JSMethod
    public void delete(String R_KEY, String k) {
        maps.remove("js_engine_" + R_KEY + "_" + k);
    }
}
