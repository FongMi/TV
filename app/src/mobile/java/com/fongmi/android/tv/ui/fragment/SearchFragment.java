package com.fongmi.android.tv.ui.fragment;

import static androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Hot;
import com.fongmi.android.tv.bean.Suggest;
import com.fongmi.android.tv.databinding.FragmentSearchBinding;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.adapter.RecordAdapter;
import com.fongmi.android.tv.ui.adapter.WordAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.net.OkHttp;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.common.net.HttpHeaders;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Optional;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.Response;

public class SearchFragment extends BaseFragment implements MenuProvider, WordAdapter.OnClickListener, RecordAdapter.OnClickListener {

    private FragmentSearchBinding mBinding;
    private RecordAdapter mRecordAdapter;
    private WordAdapter mWordAdapter;

    public static SearchFragment newInstance(String keyword) {
        Bundle args = new Bundle();
        args.putString("keyword", keyword);
        SearchFragment fragment = new SearchFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private String getKeyword() {
        return getArguments().getString("keyword");
    }

    private boolean empty() {
        return mBinding.keyword.getText().toString().trim().isEmpty();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSearchBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initMenu() {
        if (isHidden()) return;
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar(mBinding.toolbar);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activity.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        activity.setTitle("");
    }

    @Override
    protected void initView() {
        setRecyclerView();
        checkKeyword();
        getHot();
        search();
    }

    private void setRecyclerView() {
        mBinding.wordRecycler.setHasFixedSize(false);
        mBinding.wordRecycler.setAdapter(mWordAdapter = new WordAdapter(this));
        mBinding.wordRecycler.setLayoutManager(new FlexboxLayoutManager(getContext(), FlexDirection.ROW));
        mBinding.recordRecycler.setHasFixedSize(false);
        mBinding.recordRecycler.setAdapter(mRecordAdapter = new RecordAdapter(this));
        mBinding.recordRecycler.setLayoutManager(new FlexboxLayoutManager(getContext(), FlexDirection.ROW));
    }

    @Override
    protected void initEvent() {
        mBinding.keyword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) search();
            return true;
        });
        mBinding.keyword.addTextChangedListener(new CustomTextListener() {
            @Override
            public void afterTextChanged(Editable s) {
                requireActivity().invalidateOptionsMenu();
                if (s.toString().isEmpty()) getHot();
                else getSuggest(s.toString());
            }
        });
    }

    private void checkKeyword() {
        boolean visible = requireActivity().getSupportFragmentManager().findFragmentByTag(CollectFragment.class.getSimpleName()) != null;
        if (TextUtils.isEmpty(getKeyword()) && !visible) Util.showKeyboard(mBinding.keyword);
        setKeyword(getKeyword());
    }

    private void setKeyword(String text) {
        mBinding.keyword.setText(text);
        mBinding.keyword.setSelection(text.length());
    }

    private void search() {
        if (empty()) return;
        String keyword = mBinding.keyword.getText().toString().trim();
        App.post(() -> mRecordAdapter.add(keyword), 250);
        Util.hideKeyboard(mBinding.keyword);
        collect(keyword);
    }

    private void collect(String keyword) {
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        String collectTag = CollectFragment.class.getSimpleName();
        if (fm.findFragmentByTag(collectTag) != null) return;
        String searchTag = SearchFragment.class.getSimpleName();
        FragmentTransaction ft = fm.beginTransaction().setTransition(TRANSIT_FRAGMENT_OPEN);
        ft.add(R.id.container, CollectFragment.newInstance(keyword), collectTag);
        Optional.ofNullable(fm.findFragmentByTag(searchTag)).ifPresent(ft::hide);
        ft.setReorderingAllowed(true).addToBackStack(null).commit();
    }

    private void getHot() {
        mBinding.word.setText(R.string.search_hot);
        mWordAdapter.addAll(Hot.get(Setting.getHot()));
        OkHttp.newCall("https://api.web.360kan.com/v1/rank?cat=1", Headers.of(HttpHeaders.REFERER, "https://www.360kan.com/rank/general")).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<String> items = Hot.get(response.body().string());
                if (mWordAdapter.getItemCount() > 0) return;
                App.post(() -> mWordAdapter.addAll(items));
            }
        });
    }

    private void getSuggest(String text) {
        mBinding.word.setText(R.string.search_suggest);
        OkHttp.newCall("https://suggest.video.iqiyi.com/?if=mobile&key=" + URLEncoder.encode(text)).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (mBinding.keyword.getText().toString().trim().isEmpty()) return;
                List<String> items = Suggest.get(response.body().string());
                App.post(() -> mWordAdapter.addAll(items));
            }
        });
    }

    private void onReset() {
        mBinding.keyword.setText("");
        requireActivity().invalidateOptionsMenu();
    }

    private void onSite() {
        Util.hideKeyboard(mBinding.keyword);
        SiteDialog.create(this).search().show();
    }

    @Override
    public void onItemClick(String text) {
        setKeyword(text);
        search();
    }

    @Override
    public void onDataChanged(int size) {
        mBinding.record.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
        mBinding.recordRecycler.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
        mBinding.recordRecycler.postDelayed(() -> mBinding.recordRecycler.requestLayout(), 250);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_search, menu);
    }

    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        menu.findItem(R.id.action_reset).setVisible(!empty());
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) requireActivity().getOnBackPressedDispatcher().onBackPressed();
        if (menuItem.getItemId() == R.id.action_reset) onReset();
        if (menuItem.getItemId() == R.id.action_site) onSite();
        return true;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) requireActivity().removeMenuProvider(this);
        else initMenu();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().removeMenuProvider(this);
    }
}
