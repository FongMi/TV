package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;

import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.RowPresenter;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;

public class CustomRowPresenter extends ListRowPresenter {

    private final int spacing;
    private final int strategy;
    private RecyclerView.RecycledViewPool recycledViewPool;
    private ArrayList<Presenter> presenterMapper;

    public CustomRowPresenter(int spacing) {
        this(spacing, FocusHighlight.ZOOM_FACTOR_SMALL);
    }

    @SuppressLint("RestrictedApi")
    public CustomRowPresenter(int spacing, int focusZoomFactor) {
        this(spacing, focusZoomFactor, HorizontalGridView.FOCUS_SCROLL_ITEM);
    }

    public CustomRowPresenter(int spacing, int focusZoomFactor, int strategy) {
        super(focusZoomFactor);
        this.spacing = spacing;
        this.strategy = strategy;
        setShadowEnabled(false);
        setSelectEffectEnabled(false);
        setKeepChildForeground(false);
    }

    @Override
    @SuppressLint("RestrictedApi")
    protected void initializeRowViewHolder(RowPresenter.ViewHolder holder) {
        super.initializeRowViewHolder(holder);
        ViewHolder vh = (ViewHolder) holder;
        vh.getGridView().setFocusScrollStrategy(strategy);
        vh.getGridView().setHorizontalSpacing(ResUtil.dp2px(spacing));
        // Keep view types consistent when recycling item views between rows.
        if (recycledViewPool == null) recycledViewPool = vh.getGridView().getRecycledViewPool();
        else vh.getGridView().setRecycledViewPool(recycledViewPool);
        ItemBridgeAdapter adapter = vh.getBridgeAdapter();
        if (presenterMapper == null) presenterMapper = adapter.getPresenterMapper();
        else adapter.setPresenterMapper(presenterMapper);
    }
}
