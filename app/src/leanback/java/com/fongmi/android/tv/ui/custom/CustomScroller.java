package com.fongmi.android.tv.ui.custom;

import android.view.View;

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
    public void onScrollStateChanged(@NonNull RecyclerView view, int newState) {
        if (isDisable() || isLoading() || newState != RecyclerView.SCROLL_STATE_IDLE) return;
        if (isBottom(view)) callback.onLoadMore(String.valueOf(++page));
    }

    private boolean isBottom(RecyclerView view) {
        if (view.getLayoutManager() == null || view.getLayoutManager().getItemCount() == 0) return false;
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

    public boolean first() {
        return page == 1;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
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

    public interface Callback {
        void onLoadMore(String page);
    }
}
