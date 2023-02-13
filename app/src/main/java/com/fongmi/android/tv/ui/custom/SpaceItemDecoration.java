package com.fongmi.android.tv.ui.custom;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.utils.ResUtil;

public class SpaceItemDecoration extends RecyclerView.ItemDecoration {

    private final int spacing;
    private final int spanCount;

    public SpaceItemDecoration(int spacing) {
        this(-1, spacing);
    }

    public SpaceItemDecoration(int spanCount, int spacing) {
        this.spanCount = spanCount;
        this.spacing = ResUtil.dp2px(spacing);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        if (position >= 0 && spanCount == -1) {
            outRect.left = position == 0 ? 0 : spacing / 2;
            outRect.right = position == parent.getAdapter().getItemCount() - 1 ? 0 : spacing / 2;
        } else if (position >= 0 && spanCount > 0) {
            int column = position % spanCount;
            outRect.left = column * spacing / spanCount;
            outRect.right = spacing - (column + 1) * spacing / spanCount;
            if (position >= spanCount) outRect.top = spacing;
        } else {
            outRect.left = 0;
            outRect.right = 0;
            outRect.top = 0;
            outRect.bottom = 0;
        }
    }
}