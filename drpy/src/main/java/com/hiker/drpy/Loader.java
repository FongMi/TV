package com.hiker.drpy;

import android.content.Context;

import androidx.media3.common.util.UriUtil;

import com.whl.quickjs.android.QuickJSLoader;
import com.whl.quickjs.wrapper.JSModule;

public class Loader {

    static {
        try {
            QuickJSLoader.init();
        } catch (Throwable ignored) {
        }
    }

    public static void init(Context context) {
        JSModule.setModuleLoader(new JSModule.ModuleLoader() {
            @Override
            public String convertModuleName(String moduleBaseName, String moduleName) {
                return UriUtil.resolve(moduleBaseName, moduleName);
            }

            @Override
            public String getModuleScript(String moduleName) {
                return Module.get().load(context, moduleName);
            }
        });
    }
}
