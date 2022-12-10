package com.hiker.drpy;

import com.hiker.drpy.method.Console;
import com.hiker.drpy.method.Global;
import com.hiker.drpy.method.Local;
import com.whl.quickjs.android.QuickJSLoader;
import com.whl.quickjs.wrapper.JSModule;
import com.whl.quickjs.wrapper.QuickJSContext;

public class Loader {

    private QuickJSContext ctx;

    static {
        QuickJSLoader.init();
    }

    public Loader() {
        setModuleLoader();
        Worker.submit(this::initCtx);
    }

    private void setModuleLoader() {
        JSModule.setModuleLoader(new JSModule.ModuleLoader() {
            @Override
            public String getModuleScript(String moduleName) {
                return Module.get().load(moduleName);
            }
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
