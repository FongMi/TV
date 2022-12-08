package com.hiker.drpy;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.hiker.drpy.net.OkHttp;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Response;

public class Loader {

    private final String TAG = Loader.class.getSimpleName();
    private QuickJSContext jsContext;
    private Context context;

    private void init(Context context) {
        System.loadLibrary("quickjs-android-wrapper");
        this.jsContext = QuickJSContext.create();
        this.context = context;
        initConsole();
        initLocal();
        initReq();
    }

    public Spider spider(Context context, String name, String ext) {
        if (jsContext == null) init(context);
        return new Spider();
    }

    private void initConsole() {
        jsContext.evaluate("var console = {};");
        JSObject console = (JSObject) jsContext.getGlobalObject().getProperty("console");
        console.setProperty("log", args -> {
            StringBuilder b = new StringBuilder();
            for (Object o : args) b.append(o == null ? "null" : o.toString());
            Log.e(TAG, b.toString());
            return null;
        });
    }

    private void initLocal() {
        jsContext.evaluate("var local = {};");
        JSObject local = (JSObject) jsContext.getGlobalObject().getProperty("local");
        local.setProperty("get", args -> {
            SharedPreferences pref = context.getSharedPreferences("js_engine_" + args[0].toString(), Context.MODE_PRIVATE);
            return pref.getString(args[1].toString(), "");
        });
        local.setProperty("set", args -> {
            SharedPreferences pref = context.getSharedPreferences("js_engine_" + args[0].toString(), Context.MODE_PRIVATE);
            pref.edit().putString(args[1].toString(), args[2].toString()).apply();
            return null;
        });
        local.setProperty("delete", args -> {
            SharedPreferences pref = context.getSharedPreferences("js_engine_" + args[0].toString(), Context.MODE_PRIVATE);
            pref.edit().remove(args[1].toString()).apply();
            return null;
        });
    }

    private void initReq() {
        jsContext.getGlobalObject().setProperty("req", args -> {
            JSObject jsObject = jsContext.createNewJSObject();
            JSObject jsHeader = jsContext.createNewJSObject();
            setReqObject(jsObject, jsHeader, args);
            return jsObject;
        });
    }

    private void setReqObject(JSObject jsObject, JSObject jsHeader, Object... args) {
        try {
            JSONObject obj = new JSONObject(jsContext.stringify((JSObject) args[1]));
            Response response = OkHttp.get().newCall(args[0].toString(), obj).execute();
            for (String name : response.headers().names()) jsHeader.setProperty(name, response.header(name));
            jsObject.setProperty("headers", jsHeader);
            setReqContent(jsObject, obj, response);
        } catch (Throwable e) {
            jsObject.setProperty("headers", jsHeader);
            jsObject.setProperty("content", "");
        }
    }

    private void setReqContent(JSObject jsObject, JSONObject obj, Response response) throws IOException {
        switch (obj.optInt("buffer")) {
            case 1:
                byte[] bytes = response.body().bytes();
                JSArray array = jsContext.createNewJSArray();
                for (int i = 0; i < bytes.length; i++) array.set(bytes[i], i);
                jsObject.setProperty("content", array);
                break;
            case 2:
                jsObject.setProperty("content", Base64.encodeToString(response.body().bytes(), Base64.DEFAULT));
                break;
            default:
                jsObject.setProperty("content", response.body().string());
                break;
        }
    }
}
