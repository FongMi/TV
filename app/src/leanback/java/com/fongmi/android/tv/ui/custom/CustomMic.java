package com.fongmi.android.tv.ui.custom;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;
import com.github.bassaer.library.MDColor;
import com.permissionx.guolindev.PermissionX;

public class CustomMic extends AppCompatImageView {

    private SpeechRecognizer recognizer;
    private FragmentActivity activity;
    private Animation flicker;
    private boolean listen;

    public CustomMic(@NonNull Context context) {
        super(context);
    }

    public CustomMic(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        flicker = ResUtil.getAnim(R.anim.flicker);
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
    }

    private boolean isListen() {
        return listen;
    }

    private void setListen(boolean listen) {
        this.listen = listen;
    }

    public void setListener(FragmentActivity activity, CustomTextListener listener) {
        this.recognizer.setRecognitionListener(listener);
        this.activity = activity;
    }

    private void checkPermission() {
        PermissionX.init(activity).permissions(Manifest.permission.RECORD_AUDIO).request((allGranted, grantedList, deniedList) -> {
            if (allGranted) start();
        });
    }

    private void startListening() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizer.startListening(intent);
        } catch (Exception ignored) {
        }
    }

    public void start() {
        setColorFilter(MDColor.RED_500, PorterDuff.Mode.SRC_IN);
        startAnimation(flicker);
        startListening();
        setListen(true);
    }

    public boolean stop() {
        setColorFilter(MDColor.WHITE, PorterDuff.Mode.SRC_IN);
        recognizer.stopListening();
        clearAnimation();
        setListen(false);
        return true;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) checkPermission();
        else stop();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isListen() && Utils.isBackKey(event)) return stop();
        else return super.dispatchKeyEvent(event);
    }
}
