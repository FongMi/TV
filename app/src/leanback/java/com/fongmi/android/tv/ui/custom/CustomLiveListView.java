package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.VerticalGridView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.KeyUtil;

public class CustomLiveListView extends VerticalGridView {

    private Callback listener;

    public CustomLiveListView(@NonNull Context context) {
        super(context);
    }

    public CustomLiveListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomLiveListView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setListener(Callback listener) {
        this.listener = listener;
    }

    private boolean onKeyDown() {
        if (getSelectedPosition() != getAdapter().getItemCount() - 1) return false;
        if (getId() == R.id.channel) setSelectedPosition(0);
        else listener.nextGroup(false);
        return true;
    }

    private boolean onKeyUp() {
        if (getSelectedPosition() != 0) return false;
        if (getId() == R.id.channel) setSelectedPosition(getAdapter().getItemCount());
        else listener.prevGroup(false);
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (getVisibility() == View.GONE || event.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event);
        if (getVisibility() == View.VISIBLE) listener.setUITimer();
        if (KeyUtil.isDownKey(event)) return onKeyDown();
        if (KeyUtil.isUpKey(event)) return onKeyUp();
        return super.dispatchKeyEvent(event);
    }

    public interface Callback {

        void setUITimer();

        boolean nextGroup(boolean skip);

        boolean prevGroup(boolean skip);
    }
}
