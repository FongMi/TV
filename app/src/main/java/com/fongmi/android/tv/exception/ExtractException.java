package com.fongmi.android.tv.exception;

import androidx.annotation.Nullable;

import java.util.concurrent.ExecutionException;

public class ExtractException extends ExecutionException {

    public ExtractException(@Nullable String msg) {
        super(msg);
    }
}
