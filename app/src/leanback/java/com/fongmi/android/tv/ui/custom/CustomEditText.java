package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;

import com.fongmi.android.tv.R;

public class CustomEditText extends AppCompatEditText {

    public CustomEditText(@NonNull Context context) {
        super(context);
    }

    public CustomEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean isFocused() {
        return true;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(focused ? R.color.white : R.color.transparent)));
    }
}
