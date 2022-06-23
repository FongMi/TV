package com.fongmi.bear.ui.custom;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.bear.utils.ResUtil;

public class SpaceItemDecoration extends RecyclerView.ItemDecoration {

    private boolean includeEdge;
    private int spanCount;
    private int spacing;
    private int headerNum;

    public SpaceItemDecoration(int spanCount, int spacing, boolean includeEdge, int headerNum) {
        this.spanCount = spanCount;
        this.spacing = ResUtil.dp2px(spacing);
        this.includeEdge = includeEdge;
        this.headerNum = headerNum;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view) - headerNum;
        if (position >= 0) {
            int column = position % spanCount;
            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;
                if (position < spanCount) outRect.top = spacing;
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) outRect.top = spacing;
            }
        } else {
            outRect.left = 0;
            outRect.right = 0;
            outRect.top = 0;
            outRect.bottom = 0;
        }
    }
}