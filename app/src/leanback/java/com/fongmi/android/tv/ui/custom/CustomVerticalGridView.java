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

    private List<View> views;
    private boolean moveTop;

    public CustomVerticalGridView(@NonNull Context context) {
        this(context, null);
    }

    public CustomVerticalGridView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomVerticalGridView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setMoveTop(true);
    }

    @Override
    protected void initAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
        super.initAttributes(context, attrs);
        setOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable ViewHolder child, int position, int subposition) {
                if (position == 0) {
                    for (View view : views) view.setVisibility(View.VISIBLE);
                } else if (position > 0) {
                    for (View view : views) view.setVisibility(View.GONE);
                }
            }
        });
    }

    public void setHeader(View... views) {
        this.views = Arrays.asList(views);
    }

    public void setMoveTop(boolean moveTop) {
        this.moveTop = moveTop;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event);
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && moveTop) return moveToTop();
        return super.dispatchKeyEvent(event);
    }

    public boolean moveToTop() {
        if (views == null || getSelectedPosition() == 0 || getAdapter() == null || getAdapter().getItemCount() == 0) return false;
        for (View view : views) if (view.getId() == R.id.recycler) view.requestFocus();
        for (View view : views) view.setVisibility(View.VISIBLE);
        setSelectedPosition(0);
        return true;
    }
}
