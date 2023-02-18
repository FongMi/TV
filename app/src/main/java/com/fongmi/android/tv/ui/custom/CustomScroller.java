package com.fongmi.android.tv.ui.custom;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class CustomScroller extends RecyclerView.OnScrollListener {

    private final Callback callback;
    private boolean loading;
    private boolean more;
    private int page;

    public CustomScroller(Callback callback) {
        this.callback = callback;
        this.page = 1;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        if (isLoading() || recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE || callback == null) return;
        if (!recyclerView.canScrollVertically(1) && dy > 0) callback.onLoadMore(String.valueOf(++page));
    }

    public void reset() {
        more = false;
        page = 1;
    }

    public boolean addPage() {
        if (more) return false;
        page++;
        return more = true;
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
