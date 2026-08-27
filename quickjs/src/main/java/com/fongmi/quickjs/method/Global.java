package com.fongmi.quickjs.method;

import android.net.Uri;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.fongmi.quickjs.bean.Req;
import com.fongmi.quickjs.utils.Connect;
import com.github.catvod.utils.Crypto;
import com.github.catvod.Proxy;
import com.github.catvod.utils.Trans;
import com.github.catvod.utils.UriUtil;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSMethod;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class Global {

    private final Map<Integer, Timeout> timers;
    private final ExecutorService executor;
    private final AtomicInteger timerId;
    private final QuickJSContext ctx;
    private final Timer timer;

    private volatile boolean destroyed;

    private Global(QuickJSContext ctx, ExecutorService executor) {
        this.executor = executor;
        this.timerId = new AtomicInteger();
        this.timers = new ConcurrentHashMap<>();
        this.timer = new Timer("quickjs-timer", true);
        this.ctx = ctx;
        setProperty();
    }

    public static Global create(QuickJSContext ctx, ExecutorService executor) {
        return new Global(ctx, executor);
    }

    public void destroy() {
        destroyed = true;
        for (Timeout timeout : timers.values()) timeout.cancelAndRelease();
        timers.clear();
        timer.cancel();
    }

    private void setProperty() {
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

    private boolean submit(Runnable runnable) {
        try {
            if (destroyed) return false;
            if (executor.isShutdown()) return false;
            executor.submit(runnable);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Keep
    @JSMethod
    public String s2t(String text) {
        return Trans.s2t(false, text);
    }

    @Keep
    @JSMethod
    public String t2s(String text) {
        return Trans.t2s(false, text);
    }

    @Keep
    @JSMethod
    public Integer getPort() {
        return Proxy.getPort();
    }

    @Keep
    @JSMethod
    public String getProxy(Boolean local) {
        return Proxy.getUrl(local) + "?do=js";
    }

    @Keep
    @JSMethod
    public String js2Proxy(Boolean dynamic, Integer siteType, String siteKey, String url, JSObject headers) {
        return getProxy(!dynamic) + String.format("&from=catvod&siteType=%s&siteKey=%s&header=%s&url=%s", siteType, siteKey, Uri.encode(headers.stringify()), Uri.encode(url));
    }

    @Keep
    @JSMethod
    public Integer setTimeout(JSFunction func, Integer delay) {
        Timeout timeout = createTimeout(func);
        if (timeout == null) return 0;
        return schedule(timeout, delay) ? timeout.id : 0;
    }

    @Keep
    @JSMethod
    public Object clearTimeout(Integer id) {
        cancel(id);
        return null;
    }

    @Keep
    @JSMethod
    public JSObject _http(String url, JSObject options) {
        JSFunction complete = options.getJSFunction("complete");
        if (complete == null) return req(url, options);
        requestAsync(url, options, complete);
        return null;
    }

    @Keep
    @JSMethod
    public JSObject req(String url, JSObject options) {
        try {
            Req req = Req.objectFrom(options.stringify());
            Response res = Connect.to(url, req).execute();
            return Connect.success(ctx, req, res);
        } catch (Exception e) {
            return Connect.error(ctx);
        }
    }

    @Keep
    @JSMethod
    public String joinUrl(String parent, String child) {
        return UriUtil.resolve(parent, child);
    }

    @Keep
    @JSMethod
    public String md5X(String text) {
        return Crypto.md5(text);
    }

    @Keep
    @JSMethod
    public String aesX(String mode, boolean encrypt, String input, boolean inBase64, String key, String iv, boolean outBase64) {
        return Crypto.aes(mode, encrypt, input, inBase64, key, iv, outBase64);
    }

    @Keep
    @JSMethod
    public String desX(String mode, boolean encrypt, String input, boolean inBase64, String key, String iv, boolean outBase64) {
        return Crypto.des(mode, encrypt, input, inBase64, key, iv, outBase64);
    }

    @Keep
    @JSMethod
    public String rsaX(String mode, boolean pub, boolean encrypt, String input, boolean inBase64, String key, boolean outBase64) {
        return Crypto.rsa(mode, pub, encrypt, input, inBase64, key, outBase64);
    }

    private void requestAsync(String url, JSObject options, JSFunction complete) {
        complete.hold();
        try {
            Req req = Req.objectFrom(options.stringify());
            Connect.to(url, req).enqueue(getCallback(complete, req));
        } catch (Throwable e) {
            completeError(complete);
        }
    }

    private Callback getCallback(JSFunction complete, Req req) {
        return new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response res) {
                completeSuccess(complete, req, res);
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                completeError(complete);
            }
        };
    }

    private void completeSuccess(JSFunction complete, Req req, Response res) {
        boolean posted = postCallback(complete, () -> complete.call(Connect.success(ctx, req, res)));
        if (!posted) res.close();
    }

    private void completeError(JSFunction complete) {
        postCallback(complete, () -> complete.call(Connect.error(ctx)));
    }

    private boolean postCallback(JSFunction callback, Runnable runnable) {
        boolean posted = submit(() -> callAndRelease(callback, runnable));
        if (!posted) callback.release();
        return posted;
    }

    private void callAndRelease(JSFunction callback, Runnable runnable) {
        try {
            if (!destroyed) runnable.run();
        } finally {
            callback.release();
        }
    }

    private Timeout createTimeout(JSFunction func) {
        if (func == null || destroyed) return null;
        Timeout timeout = new Timeout(timerId.incrementAndGet(), func);
        timers.put(timeout.id, timeout);
        func.hold();
        return timeout;
    }

    private boolean schedule(Timeout timeout, Integer delay) {
        try {
            timer.schedule(timeout, getDelay(delay));
            return true;
        } catch (Throwable e) {
            cancel(timeout.id);
            return false;
        }
    }

    private int getDelay(Integer delay) {
        return Math.max(0, delay == null ? 0 : delay);
    }

    private void cancel(Integer id) {
        if (id == null) return;
        Timeout timeout = timers.remove(id);
        if (timeout != null) timeout.cancelAndRelease();
    }

    private class Timeout extends TimerTask {

        private final JSFunction func;
        private final int id;
        private volatile boolean canceled;
        private boolean released;

        private Timeout(int id, JSFunction func) {
            this.func = func;
            this.id = id;
        }

        @Override
        public void run() {
            if (submit(this::fire)) return;
            Global.this.cancel(id);
        }

        private void fire() {
            if (canceled) return;
            try {
                func.call();
            } finally {
                Global.this.cancel(id);
            }
        }

        private synchronized void cancelAndRelease() {
            canceled = true;
            cancel();
            release();
        }

        private synchronized void release() {
            if (released) return;
            released = true;
            func.release();
        }
    }
}
