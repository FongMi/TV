package com.fongmi.bear.ui.custom;

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

    private View mTabView;
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
                if (pressUp && position == 0) {
                    mTabView.setVisibility(View.VISIBLE);
                } else if (pressDown && position == 1) {
                    mTabView.setVisibility(View.GONE);
                }
            }
        });
    }

    public void setTabView(View tabView) {
        this.mTabView = tabView;
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
                moveToTop();
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    public void moveToTop() {
        mTabView.setVisibility(View.VISIBLE);
        mTabView.requestFocus();
        scrollToPosition(0);
    }
}
