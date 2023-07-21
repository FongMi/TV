package com.fongmi.quickjs.method;

import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.Keep;

import com.fongmi.quickjs.bean.Req;
import com.fongmi.quickjs.utils.JSUtil;
import com.fongmi.quickjs.utils.Parser;
import com.fongmi.quickjs.utils.Proxy;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.google.gson.Gson;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSMethod;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.FormBody;
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
    public String getProxy(Boolean local) {
        return Proxy.getUrl() + "?do=js";
    }

    @Keep
    @JSMethod
    public String js2Proxy(Boolean dynamic, Integer siteType, String siteKey, String url, JSObject headers) {
        return getProxy(true) + "&from=catvod" + "&header=" + URLEncoder.encode(ctx.stringify(headers)) + "&url=" + URLEncoder.encode(url);
    }

    @Keep
    @JSMethod
    public Object setTimeout(JSFunction func, Integer delay) {
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
            Req req = Req.objectFrom(ctx.stringify(object));
            Headers headers = Headers.of(req.getHeader());
            Response response = call(url, req, headers).execute();
            setHeader(response, jsHeader);
            jsObject.setProperty("headers", jsHeader);
            setContent(jsObject, headers, req.getBuffer(), response.body().bytes());
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
                if (!executor.isShutdown()) executor.submit(() -> {func.call();});
            }
        }, delay);
    }

    private Call call(String url, Req req, Headers headers) {
        OkHttpClient client = req.getRedirect() == 1 ? OkHttp.client() : OkHttp.noRedirect();
        client = client.newBuilder().connectTimeout(req.getTimeout(), TimeUnit.MILLISECONDS).readTimeout(req.getTimeout(), TimeUnit.MILLISECONDS).writeTimeout(req.getTimeout(), TimeUnit.MILLISECONDS).build();
        return client.newCall(getRequest(url, req, headers));
    }

    private Request getRequest(String url, Req req, Headers headers) {
        if (req.getMethod().equalsIgnoreCase("post")) {
            return new Request.Builder().url(url).headers(headers).post(getPostBody(req, headers.get("Content-Type"))).build();
        } else if (req.getMethod().equalsIgnoreCase("header")) {
            return new Request.Builder().url(url).headers(headers).head().build();
        } else {
            return new Request.Builder().url(url).headers(headers).get().build();
        }
    }

    private RequestBody getPostBody(Req req, String contentType) {
        if (req.getData() != null && req.getPostType().equals("form")) return getFormBody(req);
        if (req.getData() != null) return RequestBody.create(gson.toJson(req.getData()), MediaType.get("application/json"));
        if (req.getBody() != null && contentType != null) return RequestBody.create(gson.toJson(req.getBody()), MediaType.get(contentType));
        return RequestBody.create("", null);
    }

    private RequestBody getFormBody(Req req) {
        FormBody.Builder formBody = new FormBody.Builder();
        Map<String, String> params = Json.toMap(req.getData());
        for (String key : params.keySet()) formBody.add(key, params.get(key));
        return formBody.build();
    }

    private void setHeader(Response response, JSObject object) {
        for (Map.Entry<String, List<String>> entry : response.headers().toMultimap().entrySet()) {
            if (entry.getValue().size() == 1) object.setProperty(entry.getKey(), entry.getValue().get(0));
            if (entry.getValue().size() >= 2) object.setProperty(entry.getKey(), JSUtil.toArray(ctx, entry.getValue()));
        }
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
