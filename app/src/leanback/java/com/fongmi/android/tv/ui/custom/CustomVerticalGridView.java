package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;

public class CustomVerticalGridView extends VerticalGridView {

    private View tabView;
    private boolean pressUp;
    private boolean pressDown;

    public CustomVerticalGridView(@NonNull Context context) {
        super(context);
    }

    public CustomVerticalGridView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomVerticalGridView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void initAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
        super.initAttributes(context, attrs);
        setOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable ViewHolder child, int position, int subposition) {
                if (tabView == null) return;
                if (pressUp && position == 0) {
                    tabView.setVisibility(View.VISIBLE);
                } else if (pressDown && position == 1) {
                    tabView.setVisibility(View.GONE);
                }
            }
        });
    }

    public void setTabView(View tabView) {
        this.tabView = tabView;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event);
        pressUp = false;
        pressDown = false;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_UP:
                pressUp = true;
                return super.dispatchKeyEvent(event);
            case KeyEvent.KEYCODE_DPAD_DOWN:
                pressDown = true;
                return super.dispatchKeyEvent(event);
            case KeyEvent.KEYCODE_BACK:
                return moveToTop();
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    public boolean moveToTop() {
        if (tabView == null) return false;
        tabView.setVisibility(View.VISIBLE);
        tabView.requestFocus();
        scrollToPosition(0);
        return true;
    }
}
