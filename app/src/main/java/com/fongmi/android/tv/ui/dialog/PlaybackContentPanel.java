package com.fongmi.android.tv.ui.dialog;

import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.view.OneShotPreDrawListener;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.text.SubtitleContent;
import androidx.media3.common.text.SubtitleOffsets;
import androidx.media3.common.text.SubtitleSelectionState;
import androidx.media3.ui.danmaku.Danmaku;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.PlaybackContent;
import com.fongmi.android.tv.databinding.DialogPlaybackContentBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.setting.DanmakuSetting;
import com.fongmi.android.tv.ui.activity.PlaybackActivity;
import com.fongmi.android.tv.ui.adapter.PlaybackContentAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class PlaybackContentPanel implements PlaybackContentAdapter.OnClickListener {

    private final DialogPlaybackContentBinding binding;
    private final PlaybackContentAdapter adapter;
    private final PlaybackContentDialog.State state;
    private final PlaybackActivity activity;
    private final Runnable runnable;
    private final Runnable search;
    private final ViewTreeObserver.OnGlobalFocusChangeListener focusListener;
    private final Executor executor;
    private SubtitleContent primary = SubtitleContent.LOADING;
    private SubtitleContent secondary = SubtitleContent.LOADING;
    private List<Danmaku> danmaku = List.of();
    private long danmakuOffsetMs;
    private boolean running;
    private boolean updating;
    private boolean focusList;
    private boolean locateCurrent;
    private boolean restorePending;
    private boolean needsRefresh;
    private boolean retryMode;
    private boolean retrying;
    private volatile int generation;
    private OneShotPreDrawListener pendingFocus;
    private ContentContext requestedContext;
    private ContentContext renderedContext;

    PlaybackContentPanel(DialogPlaybackContentBinding binding, PlaybackActivity activity) {
        this(binding, activity, new ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1), runnable -> new Thread(runnable, "PlaybackContent"), new ThreadPoolExecutor.DiscardOldestPolicy()));
    }

    PlaybackContentPanel(DialogPlaybackContentBinding binding, PlaybackActivity activity, Executor executor) {
        this.binding = binding;
        this.activity = activity;
        this.executor = executor;
        this.state = new ViewModelProvider(activity).get(PlaybackContentDialog.State.class);
        this.adapter = new PlaybackContentAdapter(this);
        this.runnable = this::update;
        this.search = () -> refresh(true);
        this.focusListener = this::onFocusChanged;
    }

    @Nullable
    private PlayerManager player() {
        PlayerManager player = activity.getPlaybackPlayer();
        return player == null || player.isReleased() ? null : player;
    }

    void bind() {
        PlayerManager player = player();
        if (player != null) state.prepare(player);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setAdapter(adapter);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        SettingPanelViews.bindTabs(binding.tabs, new MaterialButton[]{binding.subtitles, binding.danmaku}, this::showTab);
        updateTabNavigation();
        bindKeyword(!state.keyword[state.tab].isEmpty());
        binding.search.setOnClickListener(this::onSearch);
        binding.current.setOnClickListener(this::onCurrent);
        binding.keyword.setOnKeyListener((view, keyCode, event) -> {
            if (!KeyUtil.isActionDown(event)) return false;
            if (KeyUtil.isUpKey(event)) return (state.tab == PlaybackContentDialog.SUBTITLE ? binding.subtitles : binding.danmaku).requestFocus();
            if (!KeyUtil.isDownKey(event)) return false;
            App.removeCallbacks(search);
            focusList = true;
            refresh(true);
            return true;
        });
        binding.keyword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (updating) return;
                cancelFocusList();
                state.keyword[state.tab] = s.toString();
                setFollow(false);
                state.scroll[state.tab] = null;
                restorePending = true;
                App.post(search, 200);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recycler, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) setFollow(false);
            }
        });
        binding.recycler.getViewTreeObserver().addOnGlobalFocusChangeListener(focusListener);
        restorePending = true;
        refresh(true);
        MaterialButton tab = state.tab == PlaybackContentDialog.SUBTITLE ? binding.subtitles : binding.danmaku;
        if (Util.isLeanback()) binding.getRoot().post(() -> {
            tab.requestFocus();
            binding.tabs.check(tab.getId());
        });
        else binding.tabs.check(tab.getId());
    }

    private void updateTabNavigation() {
        int tabId = state.tab == PlaybackContentDialog.SUBTITLE ? R.id.subtitles : R.id.danmaku;
        binding.search.setNextFocusDownId(tabId);
        binding.current.setNextFocusDownId(tabId);
    }

    void start() {
        running = true;
        if (needsRefresh) {
            needsRefresh = false;
            refresh(true);
        }
        update();
    }

    void stop() {
        running = false;
        retrying = false;
        needsRefresh = true;
        generation++;
        cancelFocusList();
        locateCurrent = false;
        PlayerManager player = player();
        if (player != null) player.setSubtitleContentEnabled(false);
        App.removeCallbacks(runnable, search);
        saveScroll();
    }

    void release() {
        stop();
        binding.recycler.getViewTreeObserver().removeOnGlobalFocusChangeListener(focusListener);
        binding.recycler.setAdapter(null);
        if (executor instanceof ExecutorService service) service.shutdownNow();
    }

    private void update() {
        if (!running) return;
        PlayerManager player = player();
        if (player != null) {
            String media = state.media;
            state.prepare(player);
            if (!Objects.equals(media, state.media)) {
                retrying = false;
                bindKeyword(false);
                refresh(true);
            } else refresh(false);
            adapter.setPosition(player.getPosition());
            if (state.follow[state.tab]) scrollToCurrent(false);
        }
        App.post(runnable, 500);
    }

    private void showTab(int tab) {
        if (state.tab == tab) return;
        retrying = false;
        saveScroll();
        App.removeCallbacks(search);
        state.tab = tab;
        updateTabNavigation();
        locateCurrent = false;
        bindKeyword(!state.keyword[tab].isEmpty());
        restorePending = true;
        refresh(true);
    }

    private void refresh(boolean force) {
        PlayerManager player = player();
        if (player == null) return;
        boolean isSubtitle = state.tab == PlaybackContentDialog.SUBTITLE;
        SubtitleSelections selections = isSubtitle ? getSelectedSelections(player) : SubtitleSelections.EMPTY;
        player.setSubtitleContentEnabled(running && isSubtitle);
        int empty = R.string.content_empty;
        String status = "";
        if (isSubtitle) {
            SubtitleContent first = selections.primary() == null ? SubtitleContent.UNSUPPORTED : player.getSubtitleContent(selections.primary());
            SubtitleContent second = selections.secondary() == null ? SubtitleContent.UNSUPPORTED : player.getSubtitleContent(selections.secondary());
            if (first.state != SubtitleContent.State.LOADING && second.state != SubtitleContent.State.LOADING) retrying = false;
            empty = getEmptyMessage(first.state);
            if (!player.supportsSubtitleTranscript()) empty = R.string.content_subtitle_unsupported;
            else if (selections.primary() == null) empty = R.string.content_subtitle_unselected;
            status = getStatus(first, second, selections);
            binding.status.setText(status);
            binding.status.setVisibility(status.isEmpty() || first.entries.isEmpty() && second.entries.isEmpty() ? View.GONE : View.VISIBLE);
            if (!force && primary == first && secondary == second && isCurrent(requestedContext)) {
                return;
            }
            primary = first;
            secondary = second;
        } else {
            binding.status.setVisibility(View.GONE);
            List<Danmaku> source = activity.getDanmakuItems();
            long timeOffset = DanmakuSetting.getConfig().timeOffsetMs;
            if (!force && danmaku == source && danmakuOffsetMs == timeOffset && isCurrent(requestedContext)) {
                return;
            }
            danmaku = source;
            danmakuOffsetMs = timeOffset;
        }
        String keyword = state.keyword[state.tab];
        SubtitleContent first = primary;
        SubtitleContent second = secondary;
        List<Danmaku> comments = danmaku;
        long timeOffset = danmakuOffsetMs;
        String message = status.isEmpty() ? ResUtil.getString(empty) : status;
        Player source = player.getPlayer();
        ContentContext context = new ContentContext(source, player.getKey() + "\n" + player.getUrl(), state.tab, selections, timeOffset, player.getSubtitleOffsets(), source == null ? null : source.getCurrentTimeline());
        requestedContext = context;
        int token = ++generation;
        executor.execute(() -> {
            if (token != generation) return;
            List<PlaybackContent> items;
            if (isSubtitle) items = PlaybackContent.merge(subtitles(first.entries, false), subtitles(second.entries, true));
            else {
                items = new ArrayList<>(comments.size());
                for (Danmaku item : comments) items.add(new PlaybackContent(item.timeMs * 1000, C.TIME_UNSET, item.text, "", true).offset(timeOffset));
                items = PlaybackContent.collapseDanmaku(items);
            }
            if (token != generation) return;
            int total = items.size();
            if (!keyword.isBlank()) items.removeIf(item -> !item.matches(keyword));
            List<PlaybackContent> result = items;
            activity.runOnUiThread(() -> {
                if (token == generation && keyword.equals(state.keyword[context.tab()]) && isCurrent(context)) applyItems(result, keyword, total, message, context);
            });
        });
    }

    private void applyItems(List<PlaybackContent> items, String keyword, int total, String empty, ContentContext context) {
        renderedContext = context;
        View focused = binding.recycler.getFocusedChild();
        PlaybackContent focusedItem = focused == null ? null : adapter.getItem(binding.recycler.getChildAdapterPosition(focused));
        int top = focused == null ? 0 : focused.getTop() - binding.recycler.getPaddingTop();
        Parcelable scroll = layout().onSaveInstanceState();
        adapter.setItems(items, keyword);
        int index = focusedItem == null ? -1 : items.indexOf(focusedItem);
        if (index >= 0) layout().scrollToPositionWithOffset(index, top);
        else layout().onRestoreInstanceState(scroll);
        binding.empty.setText(total > 0 && items.isEmpty() ? ResUtil.getString(R.string.content_no_match) : empty);
        binding.empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recycler.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        retryMode = total == 0 && (canRetry() || retrying);
        binding.current.setEnabled(total > 0 || retryMode);
        setFollow(state.follow[state.tab]);
        if (restorePending) {
            restorePending = false;
            restoreScroll();
        }
        PlayerManager player = player();
        if (player != null) adapter.setPosition(player.getPosition());
        if (locateCurrent || state.follow[state.tab]) scrollToCurrent(locateCurrent);
        locateCurrent = false;
        if (focusList && !items.isEmpty()) {
            cancelFocusList();
            pendingFocus = OneShotPreDrawListener.add(binding.recycler, () -> {
                pendingFocus = null;
                if (binding.keyword.hasFocus()) binding.recycler.requestFocus();
            });
        }
        focusList = false;
    }

    private void cancelFocusList() {
        focusList = false;
        if (pendingFocus != null) {
            pendingFocus.removeListener();
            pendingFocus = null;
        }
    }

    private List<PlaybackContent> subtitles(List<SubtitleContent.Entry> entries, boolean secondary) {
        List<PlaybackContent> items = new ArrayList<>(entries.size());
        for (SubtitleContent.Entry entry : entries) {
            String text = entry.image ? ResUtil.getString(R.string.content_image_subtitle) : entry.text;
            items.add(new PlaybackContent(entry.startTimeUs, entry.endTimeUs, secondary ? "" : text, secondary ? text : "", false));
        }
        items.sort(PlaybackContent.BY_TIME);
        return items;
    }

    private int getEmptyMessage(SubtitleContent.State state) {
        return switch (state) {
            case LOADING -> R.string.content_subtitle_loading;
            case ERROR -> R.string.content_subtitle_error;
            case UNSUPPORTED -> R.string.content_subtitle_unsupported;
            case PARTIAL -> R.string.content_subtitle_partial;
            case TRUNCATED -> R.string.content_subtitle_truncated;
            default -> R.string.content_empty;
        };
    }

    private boolean isCurrent(ContentContext context) {
        PlayerManager player = player();
        if (context == null || player == null || context.source() != player.getPlayer() || context.tab() != state.tab || !context.media().equals(player.getKey() + "\n" + player.getUrl())) return false;
        if (context.source() != null && !Objects.equals(context.timeline(), context.source().getCurrentTimeline())) return false;
        return state.tab == PlaybackContentDialog.SUBTITLE ? context.offsets().equals(player.getSubtitleOffsets()) && context.selections().equals(getSelectedSelections(player)) : context.offset() == DanmakuSetting.getConfig().timeOffsetMs;
    }

    private record SubtitleSelections(@Nullable TrackSelectionOverride primary, @Nullable TrackSelectionOverride secondary) {

        private static final SubtitleSelections EMPTY = new SubtitleSelections(null, null);

        private static SubtitleSelections from(SubtitleSelectionState state) {
            return new SubtitleSelections(state.primarySelection, state.activeSecondarySelection);
        }
    }

    private record ContentContext(Player source, String media, int tab, SubtitleSelections selections, long offset, SubtitleOffsets offsets, Timeline timeline) {
    }

    private String getStatus(SubtitleContent first, SubtitleContent second, SubtitleSelections selections) {
        if (selections.primary() == null) return "";
        if (selections.secondary() == null || first.state == second.state) return first.state == SubtitleContent.State.COMPLETE ? "" : ResUtil.getString(getEmptyMessage(first.state));
        String main = first.state == SubtitleContent.State.COMPLETE ? "" : ResUtil.getString(R.string.subtitle_primary_role) + "：" + ResUtil.getString(getEmptyMessage(first.state));
        String sub = second.state == SubtitleContent.State.COMPLETE ? "" : ResUtil.getString(R.string.subtitle_secondary_role) + "：" + ResUtil.getString(getEmptyMessage(second.state));
        return main.isEmpty() ? sub : sub.isEmpty() ? main : main + "\n" + sub;
    }

    private SubtitleSelections getSelectedSelections(PlayerManager player) {
        return SubtitleSelections.from(player.getSubtitleSelectionState());
    }

    private void onSearch(View view) {
        boolean visible = binding.keyword.getVisibility() == View.VISIBLE;
        if (visible) {
            Util.hideKeyboard(binding.keyword);
            binding.search.requestFocus();
            state.keyword[state.tab] = "";
            bindKeyword(false);
            App.removeCallbacks(search);
            refresh(true);
        } else {
            bindKeyword(true);
            setFollow(false);
            binding.keyword.requestFocus();
        }
    }

    private void bindKeyword(boolean visible) {
        cancelFocusList();
        updating = true;
        binding.keyword.setText(state.keyword[state.tab]);
        binding.keyword.setVisibility(visible ? View.VISIBLE : View.GONE);
        updating = false;
    }

    private void onCurrent(View view) {
        if (retryMode) {
            if (!retrying) retry();
            return;
        }
        if (player() == null) return;
        binding.keyword.setText("");
        App.removeCallbacks(search);
        setFollow(true);
        locateCurrent = true;
        refresh(true);
    }

    private void onFocusChanged(View oldFocus, View newFocus) {
        if (oldFocus == binding.keyword) cancelFocusList();
        if (newFocus == null || newFocus.getParent() != binding.recycler) return;
        setFollow(false);
        if (Util.isLeanback() && !newFocus.isInTouchMode() && !state.hintShown) {
            state.hintShown = true;
            Notify.show(R.string.content_remote_hint);
        }
    }

    private void setFollow(boolean follow) {
        state.follow[state.tab] = follow;
        int title = retryMode ? retrying ? R.string.content_retry_loading : R.string.content_retry : R.string.content_current;
        binding.current.setText(title);
        binding.current.setContentDescription(ResUtil.getString(title));
        boolean selected = !retryMode && follow && binding.current.isEnabled();
        binding.current.setSelected(selected);
        String text = retryMode ? null : binding.current.isEnabled() ? ResUtil.getString(follow ? R.string.content_following : R.string.content_browsing) : null;
        if (!TextUtils.equals(ViewCompat.getStateDescription(binding.current), text)) ViewCompat.setStateDescription(binding.current, text);
        TooltipCompat.setTooltipText(binding.current, text == null ? ResUtil.getString(title) : ResUtil.getString(title) + " · " + text);
    }

    private LinearLayoutManager layout() {
        return (LinearLayoutManager) binding.recycler.getLayoutManager();
    }

    private void saveScroll() {
        if (!restorePending) state.scroll[state.tab] = layout().onSaveInstanceState();
    }

    private void restoreScroll() {
        if (state.scroll[state.tab] != null) layout().onRestoreInstanceState(state.scroll[state.tab]);
        else layout().scrollToPositionWithOffset(0, 0);
    }

    private void scrollToCurrent(boolean force) {
        if (adapter.getItemCount() == 0) return;
        int index = adapter.getSelected();
        if (force || index < layout().findFirstVisibleItemPosition() || index > layout().findLastVisibleItemPosition()) layout().scrollToPositionWithOffset(index, 0);
    }

    @Override
    public void onItemClick(PlaybackContent item) {
        PlayerManager player = player();
        if (player == null || !isCurrent(renderedContext)) return;
        if (!player.getPlayer().isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) || !player.getPlayer().isCurrentMediaItemSeekable()) return;
        player.seekTo(item.getPosition());
        adapter.setPosition(item.getPosition());
    }

    private boolean canRetry() {
        PlayerManager player = player();
        if (state.tab != PlaybackContentDialog.SUBTITLE || player == null || !isCurrent(renderedContext)) return false;
        SubtitleSelections selections = renderedContext.selections();
        return canRetry(player, selections.primary()) || canRetry(player, selections.secondary());
    }

    private void retry() {
        if (!canRetry()) return;
        PlayerManager player = player();
        if (player == null || renderedContext == null) return;
        SubtitleSelections selections = renderedContext.selections();
        retry(player, selections.primary());
        retry(player, selections.secondary());
        retrying = true;
        App.removeCallbacks(search);
        Notify.show(R.string.content_subtitle_loading);
        refresh(true);
    }

    private static boolean canRetry(PlayerManager player, @Nullable TrackSelectionOverride selection) {
        Format format = getFormat(selection);
        return format != null && player.canRetrySubtitleContent(format);
    }

    private static void retry(PlayerManager player, @Nullable TrackSelectionOverride selection) {
        Format format = getFormat(selection);
        if (format != null && player.canRetrySubtitleContent(format)) player.retrySubtitleContent(format);
    }

    @Nullable
    private static Format getFormat(@Nullable TrackSelectionOverride selection) {
        return selection == null || selection.trackIndices.size() != 1 ? null : selection.mediaTrackGroup.getFormat(selection.trackIndices.get(0));
    }
}
