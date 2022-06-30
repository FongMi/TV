package com.fongmi.bear.ui.presenter;

import android.annotation.SuppressLint;

import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.RowPresenter;

import com.fongmi.bear.utils.ResUtil;

public class VodRowPresenter extends ListRowPresenter {

    public VodRowPresenter() {
        setShadowEnabled(false);
        setSelectEffectEnabled(false);
        setKeepChildForeground(false);
    }

    @Override
    @SuppressLint("RestrictedApi")
    protected void initializeRowViewHolder(RowPresenter.ViewHolder holder) {
        super.initializeRowViewHolder(holder);
        ViewHolder vh = (ViewHolder) holder;
        vh.getGridView().setItemSpacing(ResUtil.dp2px(16));
        vh.getGridView().setFocusScrollStrategy(HorizontalGridView.FOCUS_SCROLL_ITEM);
    }
}