package com.fongmi.quickjs;

import com.github.catvod.utils.RuntimeModule;

public final class QuickJSLoader {

    private QuickJSLoader() {
    }

    public static void init() {
        RuntimeModule.QUICKJS.load(System::load);
    }
}
