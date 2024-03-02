package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.ResUtil;

public class CustomEditText extends AppCompatEditText {

    private Animation flicker;

    public CustomEditText(@NonNull Context context) {
        super(context);
    }

    public CustomEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        flicker = ResUtil.getAnim(R.anim.flicker);
    }

    private View focusSearch(KeyEvent event) {
        if (KeyUtil.isUpKey(event)) return getParent().focusSearch(this, FOCUS_UP);
        if (KeyUtil.isDownKey(event)) return getParent().focusSearch(this, FOCUS_DOWN);
        if (KeyUtil.isLeftKey(event) && getSelectionStart() == 0) return getParent().focusSearch(this, FOCUS_LEFT);
        if (KeyUtil.isRightKey(event) && getSelectionStart() == getText().length()) return getParent().focusSearch(this, FOCUS_RIGHT);
        return null;
    }

    @Override
    public boolean isFocused() {
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        View view = focusSearch(event);
        if (view != null) view.requestFocus();
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) startAnimation(flicker);
        else clearAnimation();
    }
}
