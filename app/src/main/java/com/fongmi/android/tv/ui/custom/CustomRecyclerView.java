package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;

public class CustomRecyclerView extends RecyclerView {

    private int maxHeight;

    public CustomRecyclerView(@NonNull Context context) {
        super(context);
    }

    public CustomRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomRecyclerView);
        maxHeight = a.getLayoutDimension(R.styleable.CustomRecyclerView_maxHeight, maxHeight);
        a.recycle();
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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int newHeight = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
        if (heightMeasureSpec > newHeight) heightMeasureSpec = newHeight;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
