package com.fongmi.android.tv.ui.custom;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.App;

public class CustomScroller extends RecyclerView.OnScrollListener {

    private Callback callback;
    private boolean loading;
    private int page;

    public CustomScroller() {
    }

    public CustomScroller(Callback callback) {
        this.callback = callback;
        this.page = 1;
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE) Glide.with(App.get()).resumeRequests();
        else Glide.with(App.get()).pauseRequests();
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        if (isLoading() || recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) return;
        if (!recyclerView.canScrollVertically(1) && dy > 0 && callback != null) callback.onLoadMore(String.valueOf(++page));
    }

    public void reset() {
        page = 1;
    }

    public void addPage() {
        page++;
    }

    public int getPage() {
        return page;
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
