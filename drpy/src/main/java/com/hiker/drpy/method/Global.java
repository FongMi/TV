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
import java.util.Iterator;

import okhttp3.Headers;
import okhttp3.Response;

public class Global {

    private final QuickJSContext ctx;
    private final Parser parser;
    private final Gson gson;

    public static Global create(QuickJSContext jsContext) {
        return new Global(jsContext);
    }

    private Global(QuickJSContext ctx) {
        this.parser = new Parser();
        this.gson = new Gson();
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
            JSObject jsObject = ctx.createNewJSObject();
            JSObject jsHeader = ctx.createNewJSObject();
            jsObject.setProperty("headers", jsHeader);
            jsObject.setProperty("content", "");
            return jsObject;
        }
    }

    @Keep
    @JSMethod
    public String pd(String html, String rule, String urlKey) {
        try {
            return parser.pdfh(html, rule, urlKey);
        } catch (Exception e) {
            return "";
        }
    }

    @Keep
    @JSMethod
    public Object pdfa(String html, String rule) {
        try {
            return ctx.parse(gson.toJson(parser.pdfa(html, rule)));
        } catch (Exception e) {
            return ctx.createNewJSObject();
        }
    }

    @Keep
    @JSMethod
    public String pdfh(String html, String rule) {
        try {
            return parser.pdfh(html, rule, "");
        } catch (Exception e) {
            return "";
        }
    }

    @Keep
    @JSMethod
    public Object pdfl(String html, String rule, String texts, String urls, String urlKey) {
        try {
            return ctx.parse(gson.toJson(parser.pdfl(html, rule, texts, urls, urlKey)));
        } catch (Exception e) {
            return ctx.createNewJSObject();
        }
    }

    @Keep
    @JSMethod
    public String joinUrl(String parent, String child) {
        try {
            return parser.joinUrl(parent, child);
        } catch (Exception e) {
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

    private String getCharset(Headers headers) {
        String contentType = headers.get("Content-Type");
        if (TextUtils.isEmpty(contentType)) return "UTF-8";
        for (String text : contentType.split(";")) if (text.contains("charset=")) return text.split("=")[1];
        return "UTF-8";
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
}
