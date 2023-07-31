package com.fongmi.quickjs;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UriUtil;

import com.fongmi.quickjs.utils.Module;
import com.whl.quickjs.android.QuickJSLoader;
import com.whl.quickjs.wrapper.JSModule;

public class Provider extends ContentProvider {

    static {
        try {
            QuickJSLoader.init();
        } catch (Throwable ignored) {
        }
    }

    private void setModuleLoader() {
        JSModule.setModuleLoader(new JSModule.ModuleLoader() {
            @Override
            public String convertModuleName(String moduleBaseName, String moduleName) {
                return UriUtil.resolve(moduleBaseName, moduleName);
            }

            @Override
            public String getModuleScript(String moduleName) {
                return Module.get().load(moduleName);
            }
        });
    }

    @Override
    public boolean onCreate() {
        setModuleLoader();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
