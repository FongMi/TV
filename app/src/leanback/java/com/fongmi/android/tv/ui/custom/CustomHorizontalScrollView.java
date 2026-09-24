package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.HorizontalScrollView;

public class CustomHorizontalScrollView extends HorizontalScrollView {

    public CustomHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean arrowScroll(int direction) {
        View focused = findFocus();
        if (focused != null) {
            int id = switch (direction) {
                case FOCUS_LEFT -> focused.getNextFocusLeftId();
                case FOCUS_RIGHT -> focused.getNextFocusRightId();
                default -> NO_ID;
            };
            View next = getRootView().findViewById(id);
            // Explicit focus links may wrap to a control outside the visible scroll range.
            if (next != null && next.isShown() && next.isEnabled() && next.requestFocus(direction)) return true;
        }
        return super.arrowScroll(direction);
    }
}
