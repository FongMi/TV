package com.fongmi.android.tv.ui.custom;

import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

import com.fongmi.android.tv.App;
import com.github.catvod.crawler.SpiderDebug;

import java.util.List;
import java.util.function.BooleanSupplier;

final class WebViewClicker {

    private static final String TAG = "WebViewClicker";
    private static final int MAX_ATTEMPTS = 20;
    private static final long RETRY_DELAY_MS = 250;
    private static final long UP_DELAY_MS = 50;

    static void click(WebView view, List<String> selectors, BooleanSupplier current) {
        click(view, selectors, 0, 0, current);
    }

    private static void click(WebView view, List<String> selectors, int index, int attempt, BooleanSupplier current) {
        if (!current.getAsBoolean() || index >= selectors.size()) return;
        String selector = selectors.get(index);
        if (selector == null || selector.isEmpty()) {
            click(view, selectors, index + 1, 0, current);
            return;
        }
        String js = "(function(){var e=document.querySelector(" + App.gson().toJson(selector) + ");"
                + "if(!e)return null;e.scrollIntoView({block:'center',inline:'center'});"
                + "var r=e.getBoundingClientRect(),s=getComputedStyle(e);"
                + "if(r.width<=0||r.height<=0||s.display==='none'||s.visibility==='hidden')return null;"
                + "return JSON.stringify([r.left+r.width/2,r.top+r.height/2,window.innerWidth,window.innerHeight]);})()";
        view.evaluateJavascript(js, value -> onTarget(view, selectors, index, attempt, current, decodePoint(value)));
    }

    private static void onTarget(WebView view, List<String> selectors, int index, int attempt, BooleanSupplier current, float[] point) {
        if (!current.getAsBoolean()) return;
        if (point == null) {
            if (attempt + 1 < MAX_ATTEMPTS) App.post(() -> click(view, selectors, index, attempt + 1, current), RETRY_DELAY_MS);
            return;
        }
        dispatchClick(view, point, current);
        App.post(() -> click(view, selectors, index + 1, 0, current), RETRY_DELAY_MS);
    }

    private static float[] decodePoint(String value) {
        try {
            String json = App.gson().fromJson(value, String.class);
            float[] point = App.gson().fromJson(json, float[].class);
            return point != null && point.length == 4 ? point : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void dispatchClick(WebView view, float[] point, BooleanSupplier current) {
        if (!Float.isFinite(point[2]) || !Float.isFinite(point[3]) || point[2] <= 0 || point[3] <= 0) return;
        float x = point[0] * view.getWidth() / point[2];
        float y = point[1] * view.getHeight() / point[3];
        if (!Float.isFinite(x) || !Float.isFinite(y) || x < 0 || y < 0 || x >= view.getWidth() || y >= view.getHeight()) return;
        long downTime = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        boolean handled = view.dispatchTouchEvent(down);
        down.recycle();
        SpiderDebug.log(TAG, "view=%s, handled=%s, x=%s, y=%s, viewport=%sx%s", view.getClass().getSimpleName(), handled, x, y, view.getWidth(), view.getHeight());
        App.post(() -> {
            int action = current.getAsBoolean() ? MotionEvent.ACTION_UP : MotionEvent.ACTION_CANCEL;
            MotionEvent up = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0);
            up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            view.dispatchTouchEvent(up);
            up.recycle();
        }, UP_DELAY_MS);
    }

    static void ensureViewport(WebView view) {
        if (view.getWidth() > 0 && view.getHeight() > 0) return;
        DisplayMetrics metrics = view.getResources().getDisplayMetrics();
        int width = View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY);
        int height = View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY);
        view.measure(width, height);
        view.layout(0, 0, metrics.widthPixels, metrics.heightPixels);
    }

    private WebViewClicker() {
    }
}
