package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

public class PlaybackContentRecyclerView extends CustomRecyclerView {

    private int focusedPosition = NO_POSITION;
    private long focusedId = NO_ID;

    public PlaybackContentRecyclerView(@NonNull Context context) {
        super(context);
    }

    public PlaybackContentRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PlaybackContentRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public View focusSearch(View focused, int direction) {
        if (direction == FOCUS_RIGHT) {
            View target = getRootView().findViewById(getNextFocusRightId());
            if (target != null && target.isShown() && target.isEnabled() && target.isFocusable()) return target;
        }
        return super.focusSearch(focused, direction);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        int position = getChildAdapterPosition(child);
        if (position != NO_POSITION) {
            focusedPosition = position;
            focusedId = getAdapter() != null && getAdapter().hasStableIds() ? getAdapter().getItemId(position) : NO_ID;
        }
        if (position != NO_POSITION && !isInTouchMode() && !isComputingLayout() && getLayoutManager() != null) {
            stopScroll();
            getLayoutManager().requestChildRectangleOnScreen(this, child, new Rect(0, 0, child.getWidth(), child.getHeight()), true);
        }
        super.requestChildFocus(child, focused);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (getLayoutManager() instanceof LinearLayoutManager manager && !isComputingLayout()) {
            ViewHolder holder = focusedId == NO_ID ? null : findViewHolderForItemId(focusedId);
            View target = focusedId == NO_ID ? manager.findViewByPosition(focusedPosition) : holder == null ? null : holder.itemView;
            if (target == null || target.getBottom() <= getPaddingTop() || target.getTop() >= getHeight() - getPaddingBottom()) {
                int position = manager.findFirstCompletelyVisibleItemPosition();
                target = manager.findViewByPosition(position == NO_POSITION ? manager.findFirstVisibleItemPosition() : position);
            }
            if (target != null && target.requestFocus(direction, previouslyFocusedRect)) return true;
        }
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }
}
