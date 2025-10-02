package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;

public class CustomRecyclerView extends RecyclerView {

    private int minWidth;
    private int minHeight;
    private int maxWidth;
    private int maxHeight;
    private int touchSlop;
    private float x1;
    private float y1;

    public CustomRecyclerView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public CustomRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        if (attrs != null) setAttrs(context, attrs);
        setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    private void setAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomRecyclerView);
        minWidth = a.getDimensionPixelSize(R.styleable.CustomRecyclerView_android_minWidth, 0);
        minHeight = a.getDimensionPixelSize(R.styleable.CustomRecyclerView_android_minHeight, 0);
        maxWidth = a.getDimensionPixelSize(R.styleable.CustomRecyclerView_maxWidth, 0);
        maxHeight = a.getDimensionPixelSize(R.styleable.CustomRecyclerView_maxHeight, 0);
        a.recycle();
    }

    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    private int getConstrainedSize(int measuredSize, int minSize, int maxSize) {
        int finalSize = measuredSize;
        if (maxSize > 0) finalSize = Math.min(finalSize, maxSize);
        if (minSize > 0) finalSize = Math.max(finalSize, minSize);
        return finalSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int finalWidth = getConstrainedSize(getMeasuredWidth(), minWidth, maxWidth);
        int finalHeight = getConstrainedSize(getMeasuredHeight(), minHeight, maxHeight);
        setMeasuredDimension(finalWidth, finalHeight);
    }

    private void focus(int position) {
        ViewHolder holder = findViewHolderForLayoutPosition(position);
        if (holder != null) holder.itemView.requestFocus();
    }

    @Override
    public void scrollToPosition(int position) {
        super.scrollToPosition(position);
        postDelayed(() -> focus(position), 50);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getPointerCount() != 1) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                x1 = y1 = 0;
                break;
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                y1 = event.getY();
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                float x2 = event.getX();
                float y2 = event.getY();
                float offsetX = Math.abs(x2 - x1);
                float offsetY = Math.abs(y2 - y1);
                if (offsetX > offsetY && offsetX > touchSlop) getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.dispatchTouchEvent(event);
    }
}
