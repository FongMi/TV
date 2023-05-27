package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.Utils;

public class CustomTypeView extends AppCompatTextView {

    private Listener listener;
    private boolean coolDown;

    public CustomTypeView(@NonNull Context context) {
        super(context);
    }

    public CustomTypeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private boolean hasEvent(KeyEvent event) {
        return !coolDown && event.getAction() == KeyEvent.ACTION_DOWN && Utils.isUpKey(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (hasEvent(event)) return onKeyDown();
        else return super.dispatchKeyEvent(event);
    }

    private boolean onKeyDown() {
        App.post(() -> coolDown = false, 5000);
        listener.onRefresh();
        coolDown = true;
        return true;
    }

    public interface Listener {

        void onRefresh();
    }
}
