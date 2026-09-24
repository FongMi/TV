package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.utils.KeyUtil;
import com.google.android.material.textfield.TextInputEditText;

public class CustomEditText extends TextInputEditText {

    public CustomEditText(@NonNull Context context) {
        super(context);
    }

    public CustomEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (consumeVerticalKey(keyCode, event)) return true;
        View next = findNextFocus(event);
        return next != null ? next.requestFocus() : super.onKeyDown(keyCode, event);
    }

    private boolean consumeVerticalKey(int keyCode, KeyEvent event) {
        int direction = getVerticalDirection(event);
        if (direction == 0 || !canMoveOrScrollVertically(direction)) return false;
        if (isDpadVertical(keyCode) && super.onKeyDown(keyCode, event)) return true;
        scrollByLine(direction);
        return true;
    }

    private View findNextFocus(KeyEvent event) {
        if (getParent() == null) return null;
        int direction = getFocusDirection(event);
        return direction == 0 ? null : getParent().focusSearch(this, direction);
    }

    private int getFocusDirection(KeyEvent event) {
        if (KeyUtil.isUpKey(event)) return FOCUS_UP;
        if (KeyUtil.isDownKey(event)) return FOCUS_DOWN;
        if (KeyUtil.isLeftKey(event) && isSelectionAtStart()) return FOCUS_LEFT;
        if (KeyUtil.isRightKey(event) && isSelectionAtEnd()) return FOCUS_RIGHT;
        return 0;
    }

    private int getVerticalDirection(KeyEvent event) {
        if (KeyUtil.isUpKey(event)) return -1;
        if (KeyUtil.isDownKey(event)) return 1;
        return 0;
    }

    private boolean canMoveOrScrollVertically(int direction) {
        Layout layout = getLayout();
        if (layout == null || layout.getLineCount() <= 1) return false;
        if (canScrollVertically(direction)) return true;
        int line = layout.getLineForOffset(Math.max(0, getSelectionStart()));
        return direction < 0 ? line > 0 : line < layout.getLineCount() - 1;
    }

    private boolean isDpadVertical(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
    }

    private boolean isSelectionAtStart() {
        return getSelectionStart() == 0;
    }

    private boolean isSelectionAtEnd() {
        return getText() == null || getSelectionStart() == getText().length();
    }

    private void scrollByLine(int direction) {
        scrollTo(getScrollX(), Math.clamp(getScrollY() + (long) direction * getLineHeight(), 0, getScrollRange()));
    }

    private int getScrollRange() {
        Layout layout = getLayout();
        if (layout == null) return 0;
        int height = getHeight() - getCompoundPaddingTop() - getCompoundPaddingBottom();
        return Math.max(0, layout.getHeight() - height);
    }
}
