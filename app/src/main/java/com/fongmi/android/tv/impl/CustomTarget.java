package com.fongmi.android.tv.impl;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.request.transition.Transition;

public class CustomTarget extends com.bumptech.glide.request.target.CustomTarget<Drawable> {

    @Override
    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
    }

    @Override
    public void onLoadCleared(@Nullable Drawable placeholder) {
    }
}
