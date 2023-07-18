package com.fongmi.android.tv.impl;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Response;

public class Callback implements okhttp3.Callback {

    public void success() {
    }

    public void success(String result) {
    }

    public void error(String msg) {
    }

    @Override
    public void onFailure(@NonNull Call call, @NonNull IOException e) {
    }

    @Override
    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
    }
}
