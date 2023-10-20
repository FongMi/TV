package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.KeyUtil;

public class CustomTypeView extends AppCompatTextView {

    private Listener listener;
    private boolean coolDown;

    public CustomTypeView(@NonNull Context context) {
        super(context);
    }

    public CustomTypeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private boolean hasEvent(KeyEvent event) {
        return !coolDown && event.getAction() == KeyEvent.ACTION_DOWN && KeyUtil.isUpKey(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (hasEvent(event)) return onKeyDown();
        else return super.dispatchKeyEvent(event);
    }

    private boolean onKeyDown() {
        App.post(() -> coolDown = false, 3000);
        listener.onRefresh();
        coolDown = true;
        return true;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        App.post(() -> coolDown = false, 500);
        if (focused) coolDown = true;
    }

    public interface Listener {

        void onRefresh();
    }
}
