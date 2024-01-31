package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.fongmi.android.tv.utils.KeyUtil;

public class CustomUpDownView extends AppCompatTextView {

    private UpListener upListener;
    private DownListener downListener;

    public CustomUpDownView(@NonNull Context context) {
        super(context);
    }

    public CustomUpDownView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setUpListener(UpListener upListener) {
        this.upListener = upListener;
    }

    public void setDownListener(DownListener downListener) {
        this.downListener = downListener;
    }

    private boolean hasEvent(KeyEvent event) {
        return event.getAction() == KeyEvent.ACTION_DOWN && ((upListener != null && KeyUtil.isUpKey(event)) || (downListener != null && KeyUtil.isDownKey(event)));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (hasEvent(event)) return onKeyDown(event);
        else return super.dispatchKeyEvent(event);
    }

    private boolean onKeyDown(KeyEvent event) {
        if (upListener != null && KeyUtil.isUpKey(event)) upListener.onUp();
        if (downListener != null && KeyUtil.isDownKey(event)) downListener.onDown();
        return true;
    }

    public interface UpListener {

        void onUp();
    }

    public interface DownListener {

        void onDown();
    }
}
