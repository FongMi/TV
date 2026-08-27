package com.fongmi.android.tv.ui.dialog;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.MediaMetadata;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.SubtitleApi;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.bean.SubtitleSearchItem;
import com.fongmi.android.tv.bean.SubtitleSearchPage;
import com.fongmi.android.tv.databinding.DialogSubtitleSearchBinding;
import com.fongmi.android.tv.impl.SubtitleListener;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.setting.SubtitleSetting;
import com.fongmi.android.tv.ui.adapter.SubtitleAdapter;
import com.fongmi.android.tv.ui.custom.CustomScroller;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

import java.util.ArrayDeque;
import java.util.List;

public final class SubtitleSearchDialog extends BaseBottomSheetDialog implements SubtitleAdapter.OnClickListener, SubtitleListener {

    private final ArrayDeque<State> states;
    private final SubtitleAdapter adapter;
    private final CustomScroller scroller;

    private DialogSubtitleSearchBinding binding;
    private PlayerManager player;
    private String keyword;
    private int nextPos;

    public SubtitleSearchDialog() {
        this.states = new ArrayDeque<>();
        this.adapter = new SubtitleAdapter(this);
        this.scroller = new CustomScroller(ignored -> loadMore());
    }

    public static SubtitleSearchDialog create() {
        return new SubtitleSearchDialog();
    }

    public SubtitleSearchDialog player(PlayerManager player) {
        this.player = player;
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof SubtitleSearchDialog) return;
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogSubtitleSearchBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(false);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.recycler.addOnScrollListener(scroller);
        Util.showKeyboard(binding.keyword);
        setKeyword(getTitle());
    }

    @Override
    protected void initEvent() {
        binding.setting.setOnClickListener(this::onSetting);
        requireDialog().setOnKeyListener((dialog, keyCode, event) -> KeyUtil.isActionDown(event) && KeyUtil.isBackKey(event) && restoreState());
        binding.keyword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH && !getKeyword().isEmpty()) search();
            return true;
        });
        binding.keyword.setOnKeyListener((view, keyCode, event) -> {
            if (KeyUtil.isActionDown(event) && KeyUtil.isDownKey(event) && binding.recycler.getVisibility() == VISIBLE) return binding.recycler.requestFocus();
            return false;
        });
    }

    @Override
    protected void onBackInvoked() {
        restoreState();
    }

    @Override
    public void onItemClick(SubtitleSearchItem item) {
        if (item.hasUrl()) applySub(item);
        else detail(item);
    }

    private CharSequence getTitle() {
        MediaMetadata metadata = player.getMetadata();
        return metadata == null || TextUtils.isEmpty(metadata.title) ? "" : metadata.title;
    }

    private String getKeyword() {
        CharSequence text = binding.keyword.getText();
        return text == null ? "" : text.toString().trim();
    }

    private void setKeyword(CharSequence text) {
        if (text == null) text = "";
        binding.keyword.setText(text);
        binding.keyword.setSelection(text.length());
    }

    private void onSetting(View view) {
        SubtitleApiDialog.show(this);
    }

    private void showProgress() {
        binding.recycler.setVisibility(GONE);
        binding.progress.setVisibility(VISIBLE);
    }

    private void showResults(boolean empty) {
        binding.progress.setVisibility(GONE);
        binding.recycler.setVisibility(empty ? GONE : VISIBLE);
        if (!empty) binding.recycler.requestFocus();
        updateBackCallback();
    }

    private void search() {
        if (!SubtitleApi.hasToken()) return;
        Util.hideKeyboard(binding.keyword);
        showProgress();
        states.clear();
        adapter.clear();
        scroller.reset();
        keyword = getKeyword();
        nextPos = 0;
        requestSearch();
    }

    @Override
    public void setSubtitleToken(String token) {
        SubtitleSetting.putSearchToken(token);
        if (SubtitleApi.hasToken() && !getKeyword().isEmpty()) search();
    }

    private void detail(SubtitleSearchItem item) {
        showProgress();
        updateBackCallback();
        scroller.endLoading(hasMore());
        SubtitleApi.detail(item.getId(), this::showItems, this::onError);
    }

    private void applySub(SubtitleSearchItem item) {
        if (item.isRemote()) downloadSub(item);
        else applySub(item.toSub());
    }

    private void applySub(Sub sub) {
        player.setSub(sub);
        dismiss();
    }

    private void downloadSub(SubtitleSearchItem item) {
        showProgress();
        updateBackCallback();
        scroller.endLoading(hasMore());
        SubtitleApi.loadSubtitle(item, this::showDownloaded, this::onError);
    }

    private boolean loadMore() {
        boolean load = hasMore();
        if (load) requestSearch();
        return load;
    }

    private boolean hasMore() {
        return !scroller.isDisable();
    }

    private void requestSearch() {
        updateBackCallback();
        SubtitleApi.search(keyword, nextPos, this::onSearch, this::onSearchError);
    }

    private void onSearchError(Exception e) {
        scroller.endLoading(false);
        onError(e);
    }

    private void onSearch(SubtitleSearchPage page) {
        boolean hasMore = page.getResultCount() >= SubtitleApi.SEARCH_COUNT;
        nextPos += page.getResultCount();
        scroller.endLoading(hasMore);
        adapter.addAll(page.getItems());
        if (adapter.getItemCount() == 0) {
            if (hasMore) loadMore();
            else onEmpty();
        } else {
            showSearchResults();
        }
    }

    private void showSearchResults() {
        showResults(false);
        RecyclerView recycler = binding.recycler;
        recycler.post(() -> scroller.checkMore(recycler));
    }

    private void showDownloaded(List<SubtitleSearchItem> items) {
        if (items.isEmpty()) onEmpty();
        else if (items.size() == 1) applySub(items.get(0));
        else showItems(items);
    }

    private void showItems(List<SubtitleSearchItem> items) {
        saveState();
        adapter.setItems(items);
        scroller.endLoading(false);
        showResults(false);
    }

    private void onEmpty() {
        showResults(adapter.getItemCount() == 0);
        Notify.show(R.string.error_empty);
    }

    private void onError(Exception e) {
        showResults(adapter.getItemCount() == 0);
        Notify.show(TextUtils.isEmpty(e.getMessage()) ? ResUtil.getString(R.string.error_empty) : e.getMessage());
    }

    private void saveState() {
        states.push(new State(adapter.getItems(), keyword, nextPos, hasMore()));
    }

    private boolean restoreState() {
        if (canCancelRequest()) {
            SubtitleApi.cancel();
            showResults(false);
            return true;
        } else if (!states.isEmpty()) {
            restore(states.pop());
            return true;
        } else {
            return false;
        }
    }

    private void restore(State state) {
        scroller.reset();
        SubtitleApi.cancel();
        nextPos = state.nextPos();
        adapter.setItems(state.items());
        scroller.endLoading(state.hasMore());
        setKeyword(keyword = state.keyword());
        showResults(adapter.getItemCount() == 0);
    }

    private void updateBackCallback() {
        setBackCallbackEnabled(!states.isEmpty() || canCancelRequest());
    }

    private boolean canCancelRequest() {
        return binding.progress.getVisibility() == VISIBLE && adapter.getItemCount() > 0;
    }

    @Override
    public void onDestroyView() {
        states.clear();
        binding = null;
        SubtitleApi.cancel();
        super.onDestroyView();
    }

    private record State(List<SubtitleSearchItem> items, String keyword, int nextPos, boolean hasMore) {
    }
}
