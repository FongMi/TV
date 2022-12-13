package com.hiker.drpy;

import android.content.Context;

import com.whl.quickjs.android.QuickJSLoader;
import com.whl.quickjs.wrapper.JSModule;

public class Loader {

    static {
        QuickJSLoader.init();
    }

    public void init(Context context) {
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
}
