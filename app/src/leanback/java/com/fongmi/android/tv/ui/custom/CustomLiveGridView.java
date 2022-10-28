package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.VerticalGridView;

import com.fongmi.android.tv.utils.Utils;

public class CustomLiveGridView extends VerticalGridView {

    private Callback listener;

    public CustomLiveGridView(@NonNull Context context) {
        super(context);
    }

    public CustomLiveGridView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomLiveGridView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setListener(Callback listener) {
        this.listener = listener;
    }

    private void moveTop() {
        if (getSelectedPosition() == getAdapter().getItemCount() - 1) setSelectedPosition(0);
        listener.setUITimer();
    }

    private void moveBottom() {
        if (getSelectedPosition() == 0) setSelectedPosition(getAdapter().getItemCount());
        listener.setUITimer();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (getVisibility() == View.GONE || event.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event);
        if (Utils.isUpKey(event)) moveBottom();
        else if (Utils.isDownKey(event)) moveTop();
        return super.dispatchKeyEvent(event);
    }

    public interface Callback {

        void setUITimer();
    }
}
