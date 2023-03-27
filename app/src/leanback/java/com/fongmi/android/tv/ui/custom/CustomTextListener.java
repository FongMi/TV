package com.fongmi.android.tv.ui.custom;

import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;

import java.util.List;

public abstract class CustomTextListener implements TextWatcher, RecognitionListener {

    @Override
    public void onReadyForSpeech(Bundle params) {
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onRmsChanged(float rmsdB) {
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
    }

    @Override
    public void onEndOfSpeech() {
    }

    @Override
    public void onError(int error) {
    }

    @Override
    public void onResults(Bundle results) {
        if (results == null) return;
        StringBuilder sb = new StringBuilder();
        List<String> texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        for (String text : texts) sb.append(text).append("\n");
        String result = sb.toString().trim();
        if (result.length() > 0) onResults(result);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    public void onResults(String result) {
    }
}
