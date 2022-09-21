package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.HorizontalGridView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;

public class CustomHorizontalGridView extends HorizontalGridView {

    private Animation shake;

    public CustomHorizontalGridView(@NonNull Context context) {
        super(context);
    }

    public CustomHorizontalGridView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomHorizontalGridView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void initAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
        super.initAttributes(context, attrs);
        this.shake = ResUtil.getAnim(R.anim.shake);
    }

    @Override
    public View focusSearch(View focused, int direction) {
        if (focused != null) {
            FocusFinder finder = FocusFinder.getInstance();
            View found = finder.findNextFocus(this, focused, direction);
            if (direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT) {
                if ((found == null || found.getId() != R.id.text) && getScrollState() == SCROLL_STATE_IDLE) {
                    focused.clearAnimation();
                    focused.startAnimation(shake);
                    return null;
                }
            }
        }
        return super.focusSearch(focused, direction);
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return super.dispatchKeyEvent(event) || executeKeyEvent(event);
    }

    public boolean executeKeyEvent(@NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT) return arrowScroll(FOCUS_LEFT);
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT) return arrowScroll(FOCUS_RIGHT);
        return false;
    }

    public boolean arrowScroll(int direction) {
        View currentFocused = findFocus();
        if (currentFocused == this) {
            currentFocused = null;
        } else if (currentFocused != null) {
            boolean isChild = false;
            for (ViewParent parent = currentFocused.getParent(); parent instanceof ViewGroup; parent = parent.getParent()) {
                if (parent == this) {
                    isChild = true;
                    break;
                }
            }
            if (!isChild) {
                currentFocused = null;
            }
        }
        boolean handled = false;
        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);
        if (nextFocused == null || nextFocused == currentFocused) {
            if (direction == FOCUS_LEFT || direction == FOCUS_RIGHT) {
                shake(currentFocused);
                handled = true;
            }
        }
        return handled;
    }

    private void shake(View currentFocused) {
        if (currentFocused != null && getScrollState() == SCROLL_STATE_IDLE) {
            currentFocused.clearAnimation();
            currentFocused.startAnimation(shake);
        }
    }
}
