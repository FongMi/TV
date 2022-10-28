package com.fongmi.android.tv.ui.custom;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class CustomScrollerLive extends RecyclerView.OnScrollListener {

    private final Callback callback;

    public CustomScrollerLive(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        callback.onScrolled();
    }

    public interface Callback {
        void onScrolled();
    }
}
