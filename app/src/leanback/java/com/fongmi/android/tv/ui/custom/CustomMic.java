package com.fongmi.android.tv.ui.custom;

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

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;
import com.github.bassaer.library.MDColor;

public class CustomMic extends AppCompatImageView {

    private SpeechRecognizer recognizer;
    private Animation flicker;

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

    public void setListener(CustomListener listener) {
        recognizer.setRecognitionListener(listener);
    }

    public void start() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        setColorFilter(MDColor.RED_500, PorterDuff.Mode.SRC_IN);
        recognizer.startListening(intent);
    }

    public boolean stop() {
        setColorFilter(MDColor.WHITE, PorterDuff.Mode.SRC_IN);
        recognizer.stopListening();
        clearAnimation();
        return true;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) startAnimation(flicker);
        else stop();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (Utils.isBackKey(event) && event.getAction() == KeyEvent.ACTION_UP) return stop();
        else return super.dispatchKeyEvent(event);
    }
}
