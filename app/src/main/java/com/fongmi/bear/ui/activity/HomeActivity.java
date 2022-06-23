package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.App;
import com.fongmi.bear.R;
import com.fongmi.bear.bean.Func;
import com.fongmi.bear.databinding.ActivityHomeBinding;
import com.fongmi.bear.ui.adapter.FuncAdapter;
import com.fongmi.bear.utils.Notify;
import com.fongmi.bear.utils.Prefers;

public class HomeActivity extends BaseActivity {

    private ActivityHomeBinding binding;
    private FuncAdapter funcAdapter;

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
        if (Prefers.getUrl().isEmpty()) SettingActivity.start(this);
        else if (App.get().getConfig() == null) Notify.show(R.string.error_config);
        setRecyclerView();
    }

    @Override
    protected void initEvent() {
        funcAdapter.setOnItemClickListener(this::onFuncClick);
    }

    private void setRecyclerView() {
        binding.func.setHasFixedSize(true);
        binding.func.setLayoutManager(new GridLayoutManager(this, 5));
        binding.func.setAdapter(funcAdapter = new FuncAdapter());
    }

    private void onFuncClick(Func item) {
        switch (item.getResId()) {
            case R.string.home_setting:
                SettingActivity.start(this);
                break;
        }
    }
}