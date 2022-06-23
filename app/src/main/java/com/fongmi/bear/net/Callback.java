package com.fongmi.bear.net;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Response;

public abstract class Callback implements okhttp3.Callback {

    public void onResponse(String result) {
    }

    public void onResponse(File file) {
    }

    @Override
    public void onFailure(@NonNull Call call, @NonNull IOException e) {
    }

    @Override
    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
    }
}
