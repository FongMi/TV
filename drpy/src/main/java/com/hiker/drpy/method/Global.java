package com.hiker.drpy.method;

import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.Keep;

import com.github.catvod.net.OkHttp;
import com.google.gson.Gson;
import com.hiker.drpy.Parser;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSMethod;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Global {

    private final ExecutorService executor;
    private final QuickJSContext ctx;
    private final Parser parser;
    private final Timer timer;
    private final Gson gson;

    public static Global create(QuickJSContext ctx, ExecutorService executor) {
        return new Global(ctx, executor);
    }

    private Global(QuickJSContext ctx, ExecutorService executor) {
        this.parser = new Parser();
        this.executor = executor;
        this.timer = new Timer();
        this.gson = new Gson();
        this.ctx = ctx;
    }

    public void setProperty() {
        for (Method method : getClass().getMethods()) {
            if (!method.isAnnotationPresent(JSMethod.class)) continue;
            ctx.getGlobalObject().setProperty(method.getName(), args -> {
                try {
                    return method.invoke(this, args);
                } catch (Exception e) {
                    return null;
                }
            });
        }
    }

    @Keep
    @JSMethod
    public Object setTimeout(JSFunction func, int delay) {
        func.hold();
        schedule(func, delay);
        return null;
    }

    @Keep
    @JSMethod
    public JSObject req(String url, JSObject object) {
        try {
            JSObject jsObject = ctx.createNewJSObject();
            JSObject jsHeader = ctx.createNewJSObject();
            JSONObject obj = new JSONObject(ctx.stringify(object));
            Headers headers = getHeader(obj.optJSONObject("headers"));
            Response response = call(url, obj, headers).execute();
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

    private void schedule(JSFunction func, int delay) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                executor.submit(() -> {
                    func.call();
                });
            }
        }, delay);
    }

    private Call call(String url, JSONObject object, Headers headers) {
        int redirect = object.optInt("redirect", 1);
        int timeout = object.optInt("timeout", 10000);
        OkHttpClient client = redirect == 1 ? OkHttp.client() : OkHttp.noRedirect();
        client.newBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS);
        return client.newCall(getRequest(url, object, headers));
    }

    private Request getRequest(String url, JSONObject object, Headers headers) {
        String method = object.optString("method", "get");
        if (method.equalsIgnoreCase("post")) {
            return new Request.Builder().url(url).headers(headers).post(getPostBody(object, headers.get("Content-Type"))).build();
        } else if (method.equalsIgnoreCase("header")) {
            return new Request.Builder().url(url).headers(headers).head().build();
        } else {
            return new Request.Builder().url(url).headers(headers).get().build();
        }
    }

    private RequestBody getPostBody(JSONObject object, String contentType) {
        String body = object.optString("body").trim();
        String data = object.optString("data").trim();
        if (data.length() > 0) return RequestBody.create(data, MediaType.get("application/json"));
        if (body.length() > 0 && contentType != null) return RequestBody.create(body, MediaType.get(contentType));
        return RequestBody.create("", null);
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
