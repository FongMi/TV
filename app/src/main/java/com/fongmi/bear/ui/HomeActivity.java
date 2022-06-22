package com.fongmi.bear.ui;

import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.databinding.ActivityHomeBinding;

public class HomeActivity extends BaseActivity {

    private ActivityHomeBinding binding;

    @Override
    protected ViewBinding getBinding() {
        return binding = ActivityHomeBinding.inflate(getLayoutInflater());
    }
}