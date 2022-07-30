package com.fongmi.android.tv.ui.custom;

import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class CustomScroller extends RecyclerView.OnScrollListener {

    private final Callback callback;
    private boolean loading;
    private int page;

    public CustomScroller(Callback callback) {
        this.callback = callback;
        this.page = 1;
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView view, int newState) {
        if (isLoading() || !isBottom(view) || newState != RecyclerView.SCROLL_STATE_IDLE) return;
        setLoading(true); callback.onLoadMore(String.valueOf(++page));
    }

    private boolean isBottom(RecyclerView view) {
        View lastChildView = view.getLayoutManager().getChildAt(view.getLayoutManager().getChildCount() - 1);
        int lastChildBottom = lastChildView.getBottom();
        int recyclerBottom = view.getBottom() - view.getPaddingBottom();
        int lastPosition = view.getLayoutManager().getPosition(lastChildView);
        return lastChildBottom == recyclerBottom && lastPosition == view.getLayoutManager().getItemCount() - 1;
    }

    public void reset() {
        page = 1;
    }

    public boolean isLoading() {
        return loading;
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
    }

    public void endLoading(boolean empty) {
        new Handler().postDelayed(() -> setLoading(false), 1000);
        if (empty) page--;
    }

    public interface Callback {
        void onLoadMore(String page);
    }
}
