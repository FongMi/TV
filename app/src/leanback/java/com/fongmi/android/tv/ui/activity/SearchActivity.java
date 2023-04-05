package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Hot;
import com.fongmi.android.tv.bean.Suggest;
import com.fongmi.android.tv.databinding.ActivitySearchBinding;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.ui.adapter.RecordAdapter;
import com.fongmi.android.tv.ui.adapter.WordAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomKeyboard;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.ui.custom.dialog.SiteDialog;
import com.fongmi.android.tv.utils.Utils;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Response;

public class SearchActivity extends BaseActivity implements WordAdapter.OnClickListener, RecordAdapter.OnClickListener, CustomKeyboard.Callback {

    private ActivitySearchBinding mBinding;
    private RecordAdapter mRecordAdapter;
    private WordAdapter mWordAdapter;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SearchActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySearchBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        CustomKeyboard.init(this, mBinding);
        setRecyclerView();
        getHot();
    }

    @Override
    protected void initEvent() {
        mBinding.keyword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) onSearch();
            return true;
        });
        mBinding.keyword.addTextChangedListener(new CustomTextListener() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) getHot();
                else getSuggest(s.toString());
            }
        });
        mBinding.mic.setListener(this, new CustomTextListener() {
            @Override
            public void onEndOfSpeech() {
                mBinding.mic.stop();
            }

            @Override
            public void onResults(String result) {
                mBinding.keyword.setText(result);
                mBinding.keyword.setSelection(mBinding.keyword.length());
            }
        });
    }

    private void setRecyclerView() {
        mBinding.wordRecycler.setHasFixedSize(true);
        mBinding.wordRecycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        mBinding.wordRecycler.setAdapter(mWordAdapter = new WordAdapter(this));
        mBinding.recordRecycler.setHasFixedSize(true);
        mBinding.recordRecycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        mBinding.recordRecycler.setAdapter(mRecordAdapter = new RecordAdapter(this));
    }

    private void getHot() {
        mBinding.hint.setText(R.string.search_hot);
        OkHttp.newCall("https://api.web.360kan.com/v1/rank?cat=1").enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<String> items = Hot.get(response.body().string());
                App.post(() -> mWordAdapter.addAll(items));
            }
        });
    }

    private void getSuggest(String text) {
        mBinding.hint.setText(R.string.search_suggest);
        OkHttp.newCall("https://suggest.video.iqiyi.com/?if=mobile&key=" + text).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<String> items = Suggest.get(response.body().string());
                App.post(() -> mWordAdapter.addAll(items));
            }
        });
    }

    @Override
    public void onItemClick(String text) {
        mBinding.keyword.setText(text);
        onSearch();
    }

    @Override
    public void onDataChanged(int size) {
        mBinding.recordLayout.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onSearch() {
        mBinding.mic.setFocusable(false);
        String keyword = mBinding.keyword.getText().toString().trim();
        mBinding.keyword.setSelection(mBinding.keyword.length());
        Utils.hideKeyboard(mBinding.keyword);
        if (TextUtils.isEmpty(keyword)) return;
        CollectActivity.start(this, keyword);
        App.post(() -> mRecordAdapter.add(keyword), 250);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (Utils.isMenuKey(event)) showDialog();
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void showDialog() {
        SiteDialog.create(this).search().show();
    }

    @Override
    public void onRemote() {
        PushActivity.start(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBinding.keyword.requestFocus();
        mBinding.mic.setFocusable(true);
    }
}
