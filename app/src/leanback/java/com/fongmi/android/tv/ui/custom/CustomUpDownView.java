package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.fongmi.android.tv.utils.Utils;

public class CustomUpDownView extends AppCompatTextView {

    private AddListener addListener;
    private SubListener subListener;

    public CustomUpDownView(@NonNull Context context) {
        super(context);
    }

    public CustomUpDownView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAddListener(AddListener addListener) {
        this.addListener = addListener;
    }

    public void setSubListener(SubListener subListener) {
        this.subListener = subListener;
    }

    private boolean hasEvent(KeyEvent event) {
        return event.getAction() == KeyEvent.ACTION_DOWN && (Utils.isUpKey(event) || Utils.isDownKey(event));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (hasEvent(event)) return onKeyDown(event);
        else return super.dispatchKeyEvent(event);
    }

    private boolean onKeyDown(KeyEvent event) {
        if (Utils.isUpKey(event)) addListener.onAdd();
        if (Utils.isDownKey(event)) subListener.onSud();
        return true;
    }

    public interface AddListener {

        void onAdd();
    }

    public interface SubListener {

        void onSud();
    }
}
