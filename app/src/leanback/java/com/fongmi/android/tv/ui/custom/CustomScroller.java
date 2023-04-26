package com.fongmi.android.tv.ui.custom;

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
        if (isLoading() || newState != RecyclerView.SCROLL_STATE_IDLE) return;
        if (isBottom(view)) callback.onLoadMore(String.valueOf(++page));
    }

    private boolean isBottom(RecyclerView view) {
        View lastChild = view.getLayoutManager().getChildAt(view.getLayoutManager().getChildCount() - 1);
        int lastPosition = view.getLayoutManager().getPosition(lastChild);
        return lastPosition == view.getLayoutManager().getItemCount() - 1;
    }

    public void reset() {
        page = 1;
    }

    public int addPage() {
        return ++page;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public void endLoading(boolean empty) {
        if (empty) page--;
        setLoading(false);
    }

    public interface Callback {
        void onLoadMore(String page);
    }
}
