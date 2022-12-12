package com.hiker.drpy;

import android.content.Context;

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

    public void init(Context context) {
        setModuleLoader(context);
        Worker.submit(this::initJS);
    }

    private void setModuleLoader(Context context) {
        JSModule.setModuleLoader(new JSModule.ModuleLoader() {
            @Override
            public String convertModuleName(String moduleBaseName, String moduleName) {
                return Module.convertModuleName(moduleBaseName, moduleName);
            }

            @Override
            public String getModuleScript(String moduleName) {
                return Module.get().load(context, moduleName);
            }
        });
    }

    private void initJS() {
        ctx = QuickJSContext.create();
        ctx.getGlobalObject().setProperty("console", Console.class);
        ctx.getGlobalObject().setProperty("local", Local.class);
        Global.create(ctx).setProperty();
    }

    public Spider spider(String key, String api, String ext) {
        return new Spider(ctx, key, api, ext);
    }
}
