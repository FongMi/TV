package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.fongmi.android.tv.databinding.ViewEmptyBinding;
import com.fongmi.android.tv.databinding.ViewProgressBinding;

import java.util.ArrayList;
import java.util.List;

public class ProgressLayout extends RelativeLayout {

    private static final String TAG_PROGRESS = "ProgressLayout.TAG_PROGRESS";

    public enum State {
        CONTENT, PROGRESS, EMPTY
    }

    private List<View> mContentViews;
    private View mProgressView;
    private View mEmptyView;
    private State mState;

    public ProgressLayout(Context context) {
        super(context);
    }

    public ProgressLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProgressLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mState = State.CONTENT;
        mContentViews = new ArrayList<>();
        initView();
    }

    private void initView() {
        mEmptyView = ViewEmptyBinding.inflate(LayoutInflater.from(getContext())).getRoot();
        mEmptyView.setTag(TAG_PROGRESS);
        mEmptyView.setVisibility(GONE);
        mProgressView = ViewProgressBinding.inflate(LayoutInflater.from(getContext())).getRoot();
        mProgressView.setTag(TAG_PROGRESS);
        mProgressView.setVisibility(GONE);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(CENTER_IN_PARENT);
        addView(mProgressView, params);
        addView(mEmptyView, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (child.getTag() == null || !child.getTag().equals(TAG_PROGRESS)) {
            mContentViews.add(child);
        }
    }

    public void showProgress() {
        switchState(State.PROGRESS);
    }

    public void showEmpty() {
        switchState(State.EMPTY);
    }

    public void showContent() {
        switchState(State.CONTENT);
    }

    public void showContent(boolean flag, int size) {
        if (flag && size == 0) showEmpty();
        else showContent();
    }

    public boolean isProgress() {
        return mState == State.PROGRESS;
    }

    public void switchState(State state) {
        if (mState == state) return;
        mState = state;
        switch (state) {
            case CONTENT:
                mEmptyView.setVisibility(GONE);
                mProgressView.setVisibility(GONE);
                setContentVisibility(true);
                break;
            case PROGRESS:
                mEmptyView.setVisibility(GONE);
                mProgressView.setVisibility(VISIBLE);
                setContentVisibility(false);
                break;
            case EMPTY:
                mEmptyView.setVisibility(VISIBLE);
                mProgressView.setVisibility(GONE);
                setContentVisibility(false);
                break;
        }
    }

    private void setContentVisibility(boolean visible) {
        for (View view : mContentViews) {
            if (visible) showView(view);
            else hideView(view);
        }
    }

    private void showView(View view) {
        view.setAlpha(0f);
        view.setVisibility(VISIBLE);
        view.animate().alpha(1f).setDuration(100);
    }

    private void hideView(View view) {
        view.setVisibility(GONE);
    }
}
