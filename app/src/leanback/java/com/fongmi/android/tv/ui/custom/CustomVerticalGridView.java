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

import com.fongmi.android.tv.R;

import java.util.Arrays;
import java.util.List;

public class CustomVerticalGridView extends VerticalGridView {

    private boolean pressUp;
    private boolean pressDown;
    private List<View> views;

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
                if (views == null) return;
                if (pressUp && position == 0) {
                    for (View view : views) view.setVisibility(View.VISIBLE);
                } else if (pressDown && position == 1) {
                    for (View view : views) view.setVisibility(View.GONE);
                }
            }
        });
    }

    public void setHeader(View... views) {
        this.views = Arrays.asList(views);
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
        if (views == null) return false;
        for (View view : views) view.setVisibility(View.VISIBLE);
        for (View view : views) if (view.getId() == R.id.recycler) view.requestFocus();
        scrollToPosition(0);
        return true;
    }
}
