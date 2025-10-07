package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.bassaer.library.MDColor;

public class CustomMic extends AppCompatImageView {

    private SpeechRecognizer recognizer;
    private FragmentActivity activity;
    private boolean listen;

    public CustomMic(@NonNull Context context) {
        super(context);
    }

    public CustomMic(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    private boolean isListen() {
        return listen;
    }

    private void setListen(boolean listen) {
        this.listen = listen;
    }

    public void setListener(FragmentActivity activity, CustomTextListener listener) {
        if (recognizer == null) recognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        this.recognizer.setRecognitionListener(listener);
        this.activity = activity;
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
        PermissionUtil.requestAudio(activity, allGranted -> {
            startListening();
            requestFocus();
            updateUI(true);
        });
    }

    public boolean stop() {
        recognizer.stopListening();
        updateUI(false);
        return true;
    }

    private void updateUI(boolean listening) {
        if (listening) {
            setListen(true);
            startAnimation(ResUtil.getAnim(R.anim.flicker));
            setColorFilter(MDColor.RED_500, PorterDuff.Mode.SRC_IN);
        } else {
            setListen(false);
            clearAnimation();
            setColorFilter(MDColor.WHITE, PorterDuff.Mode.SRC_IN);
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) start();
        else stop();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isListen() && KeyUtil.isBackKey(event)) return stop();
        else return super.dispatchKeyEvent(event);
    }
}
