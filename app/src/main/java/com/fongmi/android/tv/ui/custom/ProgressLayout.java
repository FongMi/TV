package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fongmi.android.tv.databinding.ViewProgressBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProgressLayout extends RelativeLayout {

    private static final String TAG_PROGRESS = "ProgressLayout.TAG_PROGRESS";
    private static final String TAG_ERROR = "ProgressLayout.TAG_ERROR";

    public enum State {
        CONTENT, PROGRESS, ERROR
    }

    private View mProgressView;
    private TextView mErrorTextView;
    private List<View> mContentViews = new ArrayList<View>();

    private ProgressLayout.State mState = ProgressLayout.State.CONTENT;

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
        mProgressView = ViewProgressBinding.inflate(LayoutInflater.from(getContext())).getRoot();
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(CENTER_IN_PARENT);
        mProgressView.setTag(TAG_PROGRESS);
        addView(mProgressView, layoutParams);
        mErrorTextView = new TextView(getContext());
        mErrorTextView.setTag(TAG_ERROR);
        layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(CENTER_IN_PARENT);
        addView(mErrorTextView, layoutParams);
        mProgressView.setVisibility(GONE);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        if (child.getTag() == null || (!child.getTag().equals(TAG_PROGRESS) && !child.getTag().equals(TAG_ERROR))) {
            mContentViews.add(child);
        }
    }

    public void showProgress() {
        switchState(ProgressLayout.State.PROGRESS, null, Collections.<Integer>emptyList());
    }

    public void showProgress(List<Integer> skipIds) {
        switchState(ProgressLayout.State.PROGRESS, null, skipIds);
    }

    public void showErrorText() {
        switchState(ProgressLayout.State.ERROR, null, Collections.<Integer>emptyList());
    }

    public void showErrorText(List<Integer> skipIds) {
        switchState(ProgressLayout.State.ERROR, null, skipIds);
    }

    public void showErrorText(String error) {
        switchState(ProgressLayout.State.ERROR, error, Collections.<Integer>emptyList());
    }

    public void showErrorText(String error, List<Integer> skipIds) {
        switchState(ProgressLayout.State.ERROR, error, skipIds);
    }

    public void showContent() {
        switchState(ProgressLayout.State.CONTENT, null, Collections.<Integer>emptyList());
    }

    public void showContent(List<Integer> skipIds) {
        switchState(ProgressLayout.State.CONTENT, null, skipIds);
    }

    public void switchState(ProgressLayout.State state) {
        switchState(state, null, Collections.<Integer>emptyList());
    }

    public void switchState(ProgressLayout.State state, String errorText) {
        switchState(state, errorText, Collections.<Integer>emptyList());
    }

    public void switchState(ProgressLayout.State state, List<Integer> skipIds) {
        switchState(state, null, skipIds);
    }

    public void switchState(ProgressLayout.State state, String errorText, List<Integer> skipIds) {
        mState = state;
        switch (state) {
            case CONTENT:
                mErrorTextView.setVisibility(View.GONE);
                mProgressView.setVisibility(View.GONE);
                setContentVisibility(true, skipIds);
                break;
            case PROGRESS:
                mErrorTextView.setVisibility(View.GONE);
                mProgressView.setVisibility(View.VISIBLE);
                setContentVisibility(false, skipIds);
                break;
            case ERROR:
                mErrorTextView.setText(errorText);
                mErrorTextView.setVisibility(View.VISIBLE);
                mProgressView.setVisibility(View.GONE);
                setContentVisibility(false, skipIds);
                break;
        }
    }

    public ProgressLayout.State getState() {
        return mState;
    }

    public boolean isProgress() {
        return mState == ProgressLayout.State.PROGRESS;
    }

    public boolean isContent() {
        return mState == ProgressLayout.State.CONTENT;
    }

    public boolean isError() {
        return mState == ProgressLayout.State.ERROR;
    }

    private void setContentVisibility(boolean visible, List<Integer> skipIds) {
        for (View v : mContentViews) {
            if (!skipIds.contains(v.getId())) {
                v.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }
    }
}
