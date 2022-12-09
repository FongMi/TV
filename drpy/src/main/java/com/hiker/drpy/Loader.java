package com.hiker.drpy;

import com.github.tvbox.quickjs.JSModule;
import com.github.tvbox.quickjs.QuickJSContext;
import com.hiker.drpy.method.Console;
import com.hiker.drpy.method.Global;
import com.hiker.drpy.method.Local;

public class Loader {

    private QuickJSContext ctx;

    static {
        System.loadLibrary("quickjs");
    }

    public Loader() {
        Worker.submit(() -> {
            JSModule.setModuleLoader(name -> Module.get().load(name));
            initCtx();
        });
    }

    private void initCtx() {
        ctx = QuickJSContext.create();
        ctx.getGlobalObject().setProperty("console", Console.class);
        ctx.getGlobalObject().setProperty("local", Local.class);
        Global.create(ctx).setProperty();
    }

    public Spider spider(String api, String ext) {
        return new Spider(ctx, api, ext);
    }
}
