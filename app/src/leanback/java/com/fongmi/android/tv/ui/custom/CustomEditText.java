package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

import com.fongmi.android.tv.utils.KeyUtil;

public class CustomEditText extends AppCompatEditText {

    public CustomEditText(@NonNull Context context) {
        super(context);
    }

    public CustomEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    private View focusSearch(KeyEvent event) {
        if (KeyUtil.isUpKey(event)) return getParent().focusSearch(this, FOCUS_UP);
        if (KeyUtil.isDownKey(event)) return getParent().focusSearch(this, FOCUS_DOWN);
        if (KeyUtil.isLeftKey(event) && getSelectionStart() == 0) return getParent().focusSearch(this, FOCUS_LEFT);
        if (KeyUtil.isRightKey(event) && getSelectionStart() == getText().length()) return getParent().focusSearch(this, FOCUS_RIGHT);
        return null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        View v = focusSearch(event);
        if (v != null) return v.requestFocus();
        return super.onKeyDown(keyCode, event);
    }
}
