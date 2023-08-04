package com.fongmi.quickjs.method;

import android.util.Base64;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.fongmi.quickjs.bean.Req;
import com.fongmi.quickjs.utils.Connect;
import com.fongmi.quickjs.utils.Parser;
import com.fongmi.quickjs.utils.Proxy;
import com.google.gson.Gson;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSMethod;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.Callback;
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
        return getProxy(true) + "&from=catvod" + "&header=" + URLEncoder.encode(headers.stringify()) + "&url=" + URLEncoder.encode(url);
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
    public JSObject _http(String url, JSObject options) {
        JSFunction complete = options.getJSFunction("complete");
        if (complete == null) return req(url, options);
        Req req = Req.objectFrom(ctx.stringify(options));
        Connect.to(url, req).enqueue(getCallback(complete, req));
        return null;
    }

    @Keep
    @JSMethod
    public JSObject req(String url, JSObject options) {
        try {
            Req req = Req.objectFrom(ctx.stringify(options));
            Response res = Connect.to(url, req).execute();
            return Connect.success(ctx, req, res);
        } catch (Exception e) {
            return Connect.error(ctx);
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

    @Keep
    @JSMethod
    public String aesX(String mode, boolean encrypt, String input, boolean inBase64, String key, String iv, boolean outBase64) {
        try {
            byte[] keyBuf = key.getBytes();
            if (keyBuf.length < 16) keyBuf = Arrays.copyOf(keyBuf, 16);
            byte[] ivBuf = iv == null ? new byte[0] : iv.getBytes();
            if (ivBuf.length < 16) ivBuf = Arrays.copyOf(ivBuf, 16);
            Cipher cipher = Cipher.getInstance(mode + "Padding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBuf, "AES");
            if (iv == null) cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, keySpec);
            else cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(ivBuf));
            byte[] inBuf = inBase64 ? Base64.decode(input, Base64.DEFAULT) : input.getBytes(StandardCharsets.UTF_8);
            return outBase64 ? Base64.encodeToString(cipher.doFinal(inBuf), Base64.DEFAULT) : new String(cipher.doFinal(inBuf), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private Callback getCallback(JSFunction complete, Req req) {
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response res) {
                if (!executor.isShutdown()) executor.submit(() -> {complete.call(Connect.success(ctx, req, res));});
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (!executor.isShutdown()) executor.submit(() -> {complete.call(Connect.error(ctx));});
            }
        };
    }

    private void schedule(JSFunction func, int delay) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!executor.isShutdown()) executor.submit(() -> {func.call();});
            }
        }, delay);
    }
}
