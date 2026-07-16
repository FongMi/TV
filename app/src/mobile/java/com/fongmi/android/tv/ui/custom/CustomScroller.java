package com.fongmi.android.tv.ui.custom;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Result;

public class CustomScroller extends RecyclerView.OnScrollListener {

    private final Callback callback;
    private boolean loading;
    private boolean enable;
    private int page;

    public CustomScroller(Callback callback) {
        this.callback = callback;
        this.enable = true;
        this.page = 1;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
        if (view.getScrollState() == RecyclerView.SCROLL_STATE_IDLE || view.canScrollVertically(1) || dy <= 0) return;
        loadMore();
    }

    public void checkMore(RecyclerView view) {
        if (view != null && view.canScrollVertically(1)) return;
        loadMore();
    }

    private void loadMore() {
        if (isDisable() || isLoading() || callback == null) return;
        if (callback.onLoadMore(String.valueOf(page + 1))) {
            page++;
            setLoading(true);
        }
    }

    public void reset() {
        loading = false;
        enable = true;
        page = 1;
    }

    public boolean first() {
        return page == 1;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public boolean isLoading() {
        return loading;
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
    }

    public boolean isDisable() {
        return !enable;
    }

    public void setEnable(int pageCount) {
        this.enable = page < pageCount || pageCount == 0;
    }

    public void endLoading(Result result) {
        if (result.getList().isEmpty()) page--;
        setEnable(result.getPageCount());
        setLoading(false);
    }

    public void endLoading(boolean hasMore) {
        enable = hasMore;
        setLoading(false);
    }

    public interface Callback {
        boolean onLoadMore(String page);
    }
}
