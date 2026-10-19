package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.TooltipCompat;

public final class DialogActionView extends AppCompatImageView {

    public DialogActionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TooltipCompat.setTooltipText(this, getContentDescription());
    }

    @Override
    public void setContentDescription(@Nullable CharSequence description) {
        super.setContentDescription(description);
        TooltipCompat.setTooltipText(this, description);
    }
}
