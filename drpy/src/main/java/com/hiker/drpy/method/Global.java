package com.hiker.drpy.method;

import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.Keep;

import com.google.gson.Gson;
import com.hiker.drpy.Parser;
import com.hiker.drpy.net.OkHttp;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSMethod;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Iterator;

import okhttp3.Headers;
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

    @Keep
    @JSMethod
    public JSObject req(String url, JSObject object) {
        try {
            JSObject jsObject = ctx.createNewJSObject();
            JSObject jsHeader = ctx.createNewJSObject();
            JSONObject obj = new JSONObject(ctx.stringify(object));
            Headers headers = getHeader(obj.optJSONObject("headers"));
            Response response = OkHttp.get().newCall(url, obj, headers).execute();
            for (String name : response.headers().names()) jsHeader.setProperty(name, response.header(name));
            jsObject.setProperty("headers", jsHeader);
            setContent(jsObject, headers, obj.optInt("buffer"), response.body().bytes());
            return jsObject;
        } catch (Throwable e) {
            return null;
        }
    }

    @Keep
    @JSMethod
    public String pd(String html, String rule, String urlKey) {
        try {
            return Parser.parseDomForUrl(html, rule, urlKey);
        } catch (Throwable e) {
            e.printStackTrace();
            return "";
        }
    }

    @Keep
    @JSMethod
    public JSObject pdfa(String html, String rule) {
        try {
            return ctx.parseJSON(new Gson().toJson(Parser.parseDomForList(html, rule)));
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    @Keep
    @JSMethod
    public String pdfh(String html, String rule) {
        try {
            return Parser.parseDomForUrl(html, rule, "");
        } catch (Throwable e) {
            e.printStackTrace();
            return "";
        }
    }

    @Keep
    @JSMethod
    public JSObject pdfl(String html, String rule, String texts, String urls, String urlKey) {
        try {
            return ctx.parseJSON(new Gson().toJson(Parser.parseDomForList(html, rule, texts, urls, urlKey)));
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    @Keep
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

    private Headers getHeader(JSONObject object) {
        Headers.Builder builder = new Headers.Builder();
        if (object == null) return builder.build();
        for (Iterator<String> iterator = object.keys(); iterator.hasNext(); ) {
            String key = iterator.next();
            builder.add(key, object.optString(key));
        }
        return builder.build();
    }

    private void setContent(JSObject jsObject, Headers headers, int buffer, byte[] bytes) throws UnsupportedEncodingException {
        switch (buffer) {
            case 1:
                JSArray array = ctx.createNewJSArray();
                for (int i = 0; i < bytes.length; i++) array.set(bytes[i], i);
                jsObject.setProperty("content", array);
                break;
            case 2:
                jsObject.setProperty("content", Base64.encodeToString(bytes, Base64.DEFAULT));
                break;
            default:
                jsObject.setProperty("content", new String(bytes, getCharset(headers)));
                break;
        }
    }

    private String getCharset(Headers headers) {
        String contentType = headers.get("Content-Type");
        if (TextUtils.isEmpty(contentType)) return "UTF-8";
        for (String text : contentType.split(";")) if (text.contains("charset=")) return text.split("=")[1];
        return "UTF-8";
    }
}
