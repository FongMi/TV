package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.utils.KeyUtil;

public class CustomLeftRightLayout extends LinearLayout {

    private LeftListener leftListener;
    private RightListener rightListener;

    public CustomLeftRightLayout(@NonNull Context context) {
        super(context);
    }

    public CustomLeftRightLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setLeftListener(LeftListener leftListener) {
        this.leftListener = leftListener;
    }

    public void setRightListener(RightListener rightListener) {
        this.rightListener = rightListener;
    }

    private boolean hasEvent(KeyEvent event) {
        return event.getAction() == KeyEvent.ACTION_DOWN && ((leftListener != null && KeyUtil.isLeftKey(event)) || (rightListener != null && KeyUtil.isRightKey(event)));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (hasEvent(event)) return onKeyDown(event);
        else return super.dispatchKeyEvent(event);
    }

    private boolean onKeyDown(KeyEvent event) {
        if (leftListener != null && KeyUtil.isLeftKey(event)) leftListener.onLeft();
        if (rightListener != null && KeyUtil.isRightKey(event)) rightListener.onRight();
        return true;
    }

    public interface LeftListener {

        void onLeft();
    }

    public interface RightListener {

        void onRight();
    }
}
