package com.fongmi.android.tv.ui.activity;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.ActivityLiveBinding;

public class LiveActivity extends BaseActivity {

    private ActivityLiveBinding mBinding;

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityLiveBinding.inflate(getLayoutInflater());
    }
}
