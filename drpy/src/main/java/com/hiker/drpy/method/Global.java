package com.hiker.drpy.method;

import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;
import com.hiker.drpy.Parser;
import com.hiker.drpy.net.OkHttp;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSMethod;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import okhttp3.Response;

public class Global {

    private final QuickJSContext ctx;

    public static Global create(QuickJSContext jsContext) {
        return new Global(jsContext);
    }

    private Global(QuickJSContext ctx) {
        this.ctx = ctx;
    }

    public void setProperty() {
        for (Method method : getClass().getMethods()) {
            if (!method.isAnnotationPresent(JSMethod.class)) continue;
            ctx.getGlobalObject().setProperty(method.getName(), args -> {
                try {
                    return method.invoke(this, args);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    return null;
                }
            });
        }
    }

    @JSMethod
    public JSObject req(String url, JSObject object) {
        try {
            JSObject jsObject = ctx.createNewJSObject();
            JSObject jsHeader = ctx.createNewJSObject();
            JSONObject obj = new JSONObject(ctx.stringify(object));
            Response response = OkHttp.get().newCall(url, obj).execute();
            for (String name : response.headers().names()) jsHeader.setProperty(name, response.header(name));
            jsObject.setProperty("headers", jsHeader);
            setReqContent(jsObject, obj, response);
            return jsObject;
        } catch (Throwable e) {
            return null;
        }
    }

    @JSMethod
    public String pd(String html, String rule, String urlKey) {
        try {
            return Parser.parseDomForUrl(html, rule, urlKey);
        } catch (Throwable e) {
            e.printStackTrace();
            return "";
        }
    }

    @JSMethod
    public JSObject pdfa(String html, String rule) {
        try {
            return ctx.parseJSON(new Gson().toJson(Parser.parseDomForList(html, rule)));
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    @JSMethod
    public String pdfh(String html, String rule) {
        try {
            return Parser.parseDomForUrl(html, rule, "");
        } catch (Throwable e) {
            e.printStackTrace();
            return "";
        }
    }

    @JSMethod
    public JSObject pdfl(String html, String p1, String list_text, String list_url, String urlKey) {
        return null;
    }

    @JSMethod
    public String joinUrl(String parent, String child) {
        try {
            if (TextUtils.isEmpty(parent)) return child;
            return new URL(new URL(parent), child).toString();
        } catch (Throwable e) {
            e.printStackTrace();
            return "";
        }
    }

    private void setReqContent(JSObject jsObject, JSONObject obj, Response response) throws IOException {
        switch (obj.optInt("buffer")) {
            case 1:
                byte[] bytes = response.body().bytes();
                JSArray array = ctx.createNewJSArray();
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
