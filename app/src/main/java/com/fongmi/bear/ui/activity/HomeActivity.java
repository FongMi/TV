package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.ApiConfig;
import com.fongmi.bear.R;
import com.fongmi.bear.bean.Func;
import com.fongmi.bear.databinding.ActivityHomeBinding;
import com.fongmi.bear.model.SiteViewModel;
import com.fongmi.bear.ui.adapter.FuncAdapter;
import com.fongmi.bear.ui.adapter.VodAdapter;
import com.fongmi.bear.ui.custom.SpaceItemDecoration;

public class HomeActivity extends BaseActivity {

    private ActivityHomeBinding binding;
    private SiteViewModel siteViewModel;
    private FuncAdapter funcAdapter;
    private VodAdapter vodAdapter;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, HomeActivity.class));
        activity.finish();
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        setRecyclerView();
        setViewModel();
        homeContent();
    }

    @Override
    protected void initEvent() {
        funcAdapter.setOnItemClickListener(this::onFuncClick);
    }

    private void setRecyclerView() {
        binding.func.setHasFixedSize(true);
        binding.func.setNestedScrollingEnabled(false);
        binding.func.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.func.setAdapter(funcAdapter = new FuncAdapter());
        binding.update.setHasFixedSize(true);
        binding.update.setNestedScrollingEnabled(false);
        binding.update.setLayoutManager(new GridLayoutManager(this, 5));
        binding.update.addItemDecoration(new SpaceItemDecoration(5, 12, false, 0));
        binding.update.setAdapter(vodAdapter = new VodAdapter());
    }

    private void setViewModel() {
        siteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        siteViewModel.result.observe(this, result -> vodAdapter.addAll(result.getList()));
    }

    private void homeContent() {
        siteViewModel.homeContent(ApiConfig.get().getSite().getKey());
    }

    private void onFuncClick(Func item) {
        switch (item.getResId()) {
            case R.string.home_setting:
                SettingActivity.start(this);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        homeContent();
    }
}