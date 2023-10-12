package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.ui.activity.FolderActivity;

public class CustomClickSpan extends ClickableSpan {

    private final Activity activity;
    private final String json;
    private final String key;

    public static CustomClickSpan create(Activity activity, String key, String json) {
        return new CustomClickSpan(activity, key, json);
    }

    public CustomClickSpan(Activity activity, String key, String json) {
        this.activity = activity;
        this.json = json;
        this.key = key;
    }

    @Override
    public void onClick(@NonNull View view) {
        FolderActivity.start(activity, key, Result.type(json));
    }
}
