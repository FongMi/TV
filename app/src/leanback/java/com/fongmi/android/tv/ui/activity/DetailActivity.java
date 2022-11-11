package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.text.Html;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Part;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityDetailBinding;
import com.fongmi.android.tv.databinding.ViewControllerVodBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.player.ExoUtil;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.ui.custom.CustomKeyDownVod;
import com.fongmi.android.tv.ui.custom.TrackSelectionDialog;
import com.fongmi.android.tv.ui.custom.dialog.DescDialog;
import com.fongmi.android.tv.ui.presenter.ArrayPresenter;
import com.fongmi.android.tv.ui.presenter.EpisodePresenter;
import com.fongmi.android.tv.ui.presenter.FlagPresenter;
import com.fongmi.android.tv.ui.presenter.ParsePresenter;
import com.fongmi.android.tv.ui.presenter.PartPresenter;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.Response;

public class DetailActivity extends BaseActivity implements CustomKeyDownVod.Listener, ArrayPresenter.OnClickListener, Clock.Callback {

    private ActivityDetailBinding mBinding;
    private ViewControllerVodBinding mControl;
    private ViewGroup.LayoutParams mFrameParams;
    private ArrayObjectAdapter mFlagAdapter;
    private ArrayObjectAdapter mArrayAdapter;
    private ArrayObjectAdapter mEpisodeAdapter;
    private ArrayObjectAdapter mParseAdapter;
    private ArrayObjectAdapter mPartAdapter;
    private EpisodePresenter mEpisodePresenter;
    private PartPresenter mPartPresenter;
    private CustomKeyDownVod mKeyDown;
    private SiteViewModel mViewModel;
    private boolean mFullscreen;
    private History mHistory;
    private Players mPlayers;
    private int mCurrent;

    private String getKey() {
        return getIntent().getStringExtra("key");
    }

    private String getId() {
        return getIntent().getStringExtra("id");
    }

    private String getHistoryKey() {
        return getKey().concat(AppDatabase.SYMBOL).concat(getId());
    }

    private Vod.Flag getVodFlag() {
        return (Vod.Flag) mFlagAdapter.get(mBinding.flag.getSelectedPosition());
    }

    private int getEpisodePosition() {
        for (int i = 0; i < mEpisodeAdapter.size(); i++) if (((Vod.Flag.Episode) mEpisodeAdapter.get(i)).isActivated()) return i;
        return 0;
    }

    private StyledPlayerView getPlayerView() {
        return Prefers.getRender() == 0 ? mBinding.surface : mBinding.texture;
    }

    public static void start(Activity activity, String id) {
        start(activity, ApiConfig.get().getHome().getKey(), id);
    }

    public static void start(Activity activity, String key, String id) {
        start(activity, key, id, false);
    }

    public static void start(Activity activity, String key, String id, boolean clear) {
        Intent intent = new Intent(activity, DetailActivity.class);
        if (clear) intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("key", key);
        intent.putExtra("id", id);
        activity.startActivityForResult(intent, 1000);
    }

    @Override
    protected ViewBinding getBinding() {
        mBinding = ActivityDetailBinding.inflate(getLayoutInflater());
        mControl = ViewControllerVodBinding.bind(getPlayerView().findViewById(com.google.android.exoplayer2.ui.R.id.exo_controller));
        return mBinding;
    }

    @Override
    protected void initView() {
        mKeyDown = CustomKeyDownVod.create(this);
        mFrameParams = mBinding.video.getLayoutParams();
        mBinding.progressLayout.showProgress();
        mPlayers = new Players().init();
        setRecyclerView();
        setVideoView();
        setViewModel();
        getDetail();
    }

    @Override
    protected void initEvent() {
        mControl.replay.setOnClickListener(view -> getPlayer(true));
        mBinding.video.setOnClickListener(view -> enterFullscreen());
        mBinding.desc.setOnClickListener(view -> onDesc());
        mBinding.keep.setOnClickListener(view -> onKeep());
        mControl.next.setOnClickListener(view -> checkNext());
        mControl.prev.setOnClickListener(view -> checkPrev());
        mControl.scale.setOnClickListener(view -> onScale());
        mControl.speed.setOnClickListener(view -> onSpeed());
        mControl.tracks.setOnClickListener(view -> onTracks());
        mControl.ending.setOnClickListener(view -> onEnding());
        mControl.opening.setOnClickListener(view -> onOpening());
        mControl.speed.setOnLongClickListener(view -> onSpeedLong());
        mControl.ending.setOnLongClickListener(view -> onEndingReset());
        mControl.opening.setOnLongClickListener(view -> onOpeningReset());
        mBinding.flag.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (mFlagAdapter.size() > 0) setFlagActivated((Vod.Flag) mFlagAdapter.get(position));
            }
        });
        mBinding.array.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (mEpisodeAdapter.size() > 20 && position > 1) mBinding.episode.setSelectedPosition((position - 2) * 20);
            }
        });
    }

    private void setRecyclerView() {
        mBinding.flag.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.flag.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.flag.setAdapter(new ItemBridgeAdapter(mFlagAdapter = new ArrayObjectAdapter(new FlagPresenter(this::setFlagActivated))));
        mBinding.episode.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.episode.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.episode.setAdapter(new ItemBridgeAdapter(mEpisodeAdapter = new ArrayObjectAdapter(mEpisodePresenter = new EpisodePresenter(this::setEpisodeActivated))));
        mBinding.array.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.array.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.array.setAdapter(new ItemBridgeAdapter(mArrayAdapter = new ArrayObjectAdapter(new ArrayPresenter(this))));
        mBinding.part.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.part.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.part.setAdapter(new ItemBridgeAdapter(mPartAdapter = new ArrayObjectAdapter(mPartPresenter = new PartPresenter(item -> CollectActivity.start(this, item)))));
        mControl.parse.setHorizontalSpacing(ResUtil.dp2px(8));
        mControl.parse.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mControl.parse.setAdapter(new ItemBridgeAdapter(mParseAdapter = new ArrayObjectAdapter(new ParsePresenter(this::setParseActivated))));
        mParseAdapter.setItems(ApiConfig.get().getParses(), null);
    }

    private void setVideoView() {
        getPlayerView().setPlayer(mPlayers.exo());
        getPlayerView().setVisibility(View.VISIBLE);
        getPlayerView().getSubtitleView().setStyle(ExoUtil.getCaptionStyle());
        mControl.speed.setText(mPlayers.getSpeed());
        setScale(Prefers.getVodScale());
    }

    private void setScale(int scale) {
        getPlayerView().setResizeMode(scale);
        mControl.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.player.observe(this, result -> {
            boolean useParse = (result.getPlayUrl().isEmpty() && ApiConfig.get().getFlags().contains(result.getFlag())) || result.getJx() == 1;
            mControl.parseLayout.setVisibility(useParse ? View.VISIBLE : View.GONE);
            mPlayers.start(result, useParse);
            resetFocus(useParse);
        });
        mViewModel.result.observe(this, result -> {
            if (result.getList().isEmpty()) mBinding.progressLayout.showEmpty();
            else setDetail(result.getList().get(0));
        });
    }

    private void resetFocus(boolean useParse) {
        mControl.exoProgress.setNextFocusUpId(useParse ? R.id.parse : R.id.next);
        for (int i = 0; i < mControl.actionLayout.getChildCount(); i++) {
            mControl.actionLayout.getChildAt(i).setNextFocusDownId(useParse ? R.id.parse : com.google.android.exoplayer2.ui.R.id.exo_progress);
        }
    }

    private void getDetail() {
        mViewModel.detailContent(getKey(), getId());
    }

    private void getPlayer(boolean replay) {
        Vod.Flag.Episode item = (Vod.Flag.Episode) mEpisodeAdapter.get(getEpisodePosition());
        if (mFullscreen && mPlayers.getRetry() == 0) Notify.show(ResUtil.getString(R.string.play_ready, item.getName()));
        mBinding.widget.title.setText(getString(R.string.detail_title, mBinding.name.getText(), item.getName()));
        mViewModel.playerContent(getKey(), getVodFlag().getFlag(), item.getUrl());
        mBinding.widget.progress.getRoot().setVisibility(View.VISIBLE);
        mBinding.widget.error.setVisibility(View.GONE);
        Clock.get().setCallback(null);
        updateHistory(item, replay);
    }

    private void setDetail(Vod item) {
        mBinding.progressLayout.showContent();
        mBinding.video.setTag(item.getVodPic());
        mBinding.name.setText(item.getVodName());
        setText(mBinding.year, R.string.detail_year, item.getVodYear());
        setText(mBinding.area, R.string.detail_area, item.getVodArea());
        setText(mBinding.type, R.string.detail_type, item.getTypeName());
        setText(mBinding.site, R.string.detail_site, ApiConfig.getSiteName(getKey()));
        setText(mBinding.actor, R.string.detail_actor, Html.fromHtml(item.getVodActor()).toString());
        setText(mBinding.content, R.string.detail_content, Html.fromHtml(item.getVodContent()).toString());
        setText(mBinding.director, R.string.detail_director, Html.fromHtml(item.getVodDirector()).toString());
        mFlagAdapter.setItems(item.getVodFlags(), null);
        mBinding.video.requestFocus();
        if (hasFlag()) checkHistory();
        getPart(item.getVodName());
        checkKeep();
    }

    private void setText(TextView view, int resId, String text) {
        if (text.isEmpty()) view.setVisibility(View.GONE);
        else view.setText(ResUtil.getString(resId, text));
        view.setTag(text);
    }

    private void setFlagActivated(Vod.Flag item) {
        if (mFlagAdapter.size() == 0 || item.isActivated()) return;
        for (int i = 0; i < mFlagAdapter.size(); i++) ((Vod.Flag) mFlagAdapter.get(i)).setActivated(item);
        mBinding.flag.setSelectedPosition(mFlagAdapter.indexOf(item));
        mEpisodeAdapter.setItems(item.getEpisodes(), null);
        notifyItemChanged(mBinding.flag, mFlagAdapter);
        setArray(item.getEpisodes().size());
        seamless(item);
    }

    private void seamless(Vod.Flag flag) {
        Vod.Flag.Episode episode = flag.find(mHistory.getVodRemarks());
        if (episode == null || episode.isActivated()) return;
        mHistory.setVodRemarks(episode.getName());
        setEpisodeActivated(episode);
    }

    private void setEpisodeActivated(Vod.Flag.Episode item) {
        if (shouldEnterFullscreen(item)) return;
        mCurrent = mBinding.flag.getSelectedPosition();
        for (int i = 0; i < mFlagAdapter.size(); i++) ((Vod.Flag) mFlagAdapter.get(i)).toggle(mCurrent == i, item);
        mBinding.episode.setSelectedPosition(getEpisodePosition());
        notifyItemChanged(mBinding.episode, mEpisodeAdapter);
        if (mEpisodeAdapter.size() == 0) return;
        getPlayer(false);
    }

    private void reverseEpisode() {
        for (int i = 0; i < mFlagAdapter.size(); i++) Collections.reverse(((Vod.Flag) mFlagAdapter.get(i)).getEpisodes());
        mEpisodeAdapter.setItems(getVodFlag().getEpisodes(), null);
        setArray(mEpisodeAdapter.size());
    }

    private void setParseActivated(Parse item) {
        ApiConfig.get().setParse(item);
        mBinding.widget.error.setVisibility(View.GONE);
        mBinding.widget.progress.getRoot().setVisibility(View.VISIBLE);
        Result result = mViewModel.getPlayer().getValue();
        if (result != null) mPlayers.start(result, true);
        notifyItemChanged(mControl.parse, mParseAdapter);
    }

    private void setArray(int size) {
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.play_reverse));
        items.add(getString(mHistory.getRevPlayText()));
        mEpisodePresenter.setNextFocusDown(size > 1 ? R.id.array : R.id.part);
        mPartPresenter.setNextFocusUp(size > 1 ? R.id.array : R.id.episode);
        mBinding.array.setVisibility(size > 1 ? View.VISIBLE : View.GONE);
        if (mHistory.isRevSort()) for (int i = size + 1; i > 0; i -= 20) items.add((i - 1) + "-" + Math.max(i - 20, 1));
        else for (int i = 0; i < size; i += 20) items.add((i + 1) + "-" + Math.min(i + 20, size));
        mArrayAdapter.setItems(items, null);
    }

    @Override
    public void onRevSort() {
        mHistory.setRevSort(!mHistory.isRevSort());
        reverseEpisode();
    }

    @Override
    public void onRevPlay(TextView view) {
        mHistory.setRevPlay(!mHistory.isRevPlay());
        view.setText(mHistory.getRevPlayText());
        Notify.show(mHistory.getRevPlayHint());
    }

    private boolean shouldEnterFullscreen(Vod.Flag.Episode item) {
        boolean enter = !mFullscreen && item.isActivated() && !mPlayers.isIdle();
        if (enter) enterFullscreen();
        return enter;
    }

    private void enterFullscreen() {
        mBinding.video.setForeground(null);
        mBinding.video.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        App.post(() -> getPlayerView().setUseController(true), 250);
        mBinding.flag.setSelectedPosition(mCurrent);
        mFullscreen = true;
        onPlay(0);
    }

    private void exitFullscreen() {
        mBinding.widget.info.setVisibility(View.GONE);
        mBinding.widget.center.setVisibility(View.GONE);
        mBinding.video.setForeground(ResUtil.getDrawable(R.drawable.selector_video));
        mBinding.video.setLayoutParams(mFrameParams);
        getPlayerView().setUseController(false);
        mFullscreen = false;
    }

    private void onDesc() {
        String desc = mBinding.content.getTag().toString().trim();
        if (desc.length() > 0) DescDialog.show(this, desc);
    }

    private void onKeep() {
        Keep keep = Keep.find(getHistoryKey());
        Notify.show(keep != null ? R.string.keep_del : R.string.keep_add);
        if (keep != null) keep.delete();
        else createKeep();
        RefreshEvent.keep();
        checkKeep();
    }

    private void checkNext() {
        if (mHistory.isRevPlay()) onPrev();
        else onNext();
    }

    private void checkPrev() {
        if (mHistory.isRevPlay()) onNext();
        else onPrev();
    }

    private void onNext() {
        int current = getEpisodePosition();
        int max = mEpisodeAdapter.size() - 1;
        current = ++current > max ? max : current;
        Vod.Flag.Episode item = (Vod.Flag.Episode) mEpisodeAdapter.get(current);
        if (item.isActivated()) Notify.show(mHistory.isRevPlay() ? R.string.error_play_prev : R.string.error_play_next);
        else setEpisodeActivated(item);
    }

    private void onPrev() {
        int current = getEpisodePosition();
        current = --current < 0 ? 0 : current;
        Vod.Flag.Episode item = (Vod.Flag.Episode) mEpisodeAdapter.get(current);
        if (item.isActivated()) Notify.show(mHistory.isRevPlay() ? R.string.error_play_next : R.string.error_play_prev);
        else setEpisodeActivated(item);
    }

    private void onScale() {
        int scale = getPlayerView().getResizeMode();
        getPlayerView().setResizeMode(scale = scale == 4 ? 0 : scale + 1);
        mControl.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
        mHistory.setScale(scale);
    }

    private void onSpeed() {
        mPlayers.addSpeed();
        mControl.speed.setText(mPlayers.getSpeed());
    }

    private boolean onSpeedLong() {
        mPlayers.toggleSpeed();
        mControl.speed.setText(mPlayers.getSpeed());
        return true;
    }

    private void onOpening() {
        long current = mPlayers.getCurrentPosition();
        long duration = mPlayers.getDuration();
        if (current < 0 || current > duration / 2) return;
        mHistory.setOpening(current);
        mControl.opening.setText(mPlayers.getStringForTime(mHistory.getOpening()));
    }

    private boolean onOpeningReset() {
        mHistory.setOpening(0);
        mControl.opening.setText(mPlayers.getStringForTime(mHistory.getOpening()));
        return true;
    }

    private void onEnding() {
        long current = mPlayers.getCurrentPosition();
        long duration = mPlayers.getDuration();
        if (current < 0 || current < duration / 2) return;
        mHistory.setEnding(duration - current);
        mControl.ending.setText(mPlayers.getStringForTime(mHistory.getEnding()));
    }

    private boolean onEndingReset() {
        mHistory.setEnding(0);
        mControl.ending.setText(mPlayers.getStringForTime(mHistory.getEnding()));
        return true;
    }

    private void onTracks() {
        App.post(() -> getPlayerView().hideController(), 150);
        TrackSelectionDialog.createForPlayer(mPlayers.exo(), dialog -> {
        }).show(getSupportFragmentManager(), "tracks");
    }

    private void getPart(String source) {
        OKHttp.newCall("http://api.pullword.com/get.php?source=" + URLEncoder.encode(source) + "&param1=0&param2=0&json=1").enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<String> items = Part.get(response.body().string());
                if (!items.contains(source)) items.add(0, source);
                App.post(() -> mPartAdapter.setItems(items, null));
            }
        });
    }

    private boolean hasFlag() {
        if (mFlagAdapter.size() > 0) return true;
        mBinding.flag.setVisibility(View.GONE);
        mBinding.array.setVisibility(View.GONE);
        mBinding.episode.setVisibility(View.GONE);
        Notify.show(R.string.error_episode);
        return false;
    }

    private void checkHistory() {
        mHistory = History.find(getHistoryKey());
        mHistory = mHistory == null ? createHistory() : mHistory;
        setFlagActivated(mHistory.getFlag());
        if (mHistory.isRevSort()) reverseEpisode();
        if (mHistory.getScale() != -1) setScale(mHistory.getScale());
        mControl.opening.setText(mPlayers.getStringForTime(mHistory.getOpening()));
        mControl.ending.setText(mPlayers.getStringForTime(mHistory.getEnding()));
    }

    private History createHistory() {
        History history = new History();
        history.setKey(getHistoryKey());
        history.setCid(ApiConfig.getCid());
        history.setVodPic(mBinding.video.getTag().toString());
        history.setVodName(mBinding.name.getText().toString());
        history.findEpisode(mFlagAdapter);
        return history;
    }

    private void updateHistory(Vod.Flag.Episode item, boolean replay) {
        replay = replay || !item.equals(mHistory.getEpisode());
        long position = replay ? 0 : mHistory.getPosition();
        mHistory.setPosition(position);
        mHistory.setEpisodeUrl(item.getUrl());
        mHistory.setVodRemarks(item.getName());
        mHistory.setVodFlag(getVodFlag().getFlag());
        mHistory.setCreateTime(System.currentTimeMillis());
    }

    private void checkKeep() {
        mBinding.keep.setCompoundDrawablesRelativeWithIntrinsicBounds(Keep.find(getHistoryKey()) == null ? R.drawable.ic_keep_not_yet : R.drawable.ic_keep_added, 0, 0, 0);
    }

    private void createKeep() {
        Keep keep = new Keep();
        keep.setKey(getHistoryKey());
        keep.setCid(ApiConfig.getCid());
        keep.setSiteName(ApiConfig.getSiteName(getKey()));
        keep.setVodPic(mBinding.video.getTag().toString());
        keep.setVodName(mBinding.name.getText().toString());
        keep.setCreateTime(System.currentTimeMillis());
        keep.save();
    }

    private final Runnable mHideCenter = new Runnable() {
        @Override
        public void run() {
            mBinding.widget.action.setImageResource(R.drawable.ic_play);
            mBinding.widget.center.setVisibility(View.GONE);
            mBinding.widget.info.setVisibility(View.GONE);
        }
    };

    @Override
    public void onTimeChanged() {
        long duration = mPlayers.getDuration();
        long current = mPlayers.getCurrentPosition();
        if (current >= 0 && duration > 0) App.execute(() -> mHistory.update(current, duration));
        if (mHistory.getEnding() > 0 && duration > 0 && mHistory.getEnding() + current >= duration) {
            Clock.get().setCallback(null);
            checkNext();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerEvent(PlayerEvent event) {
        switch (event.getState()) {
            case 0:
                checkPosition();
                break;
            case Player.STATE_IDLE:
                break;
            case Player.STATE_BUFFERING:
                mBinding.widget.progress.getRoot().setVisibility(View.VISIBLE);
                break;
            case Player.STATE_READY:
                mPlayers.reset();
                mBinding.widget.progress.getRoot().setVisibility(View.GONE);
                TrackSelectionDialog.setVisible(mPlayers.exo(), mControl.tracks);
                break;
            case Player.STATE_ENDED:
                if (mPlayers.canNext()) checkNext();
                break;
            default:
                if (!event.isRetry() || mPlayers.addRetry() > 2) onError(event.getMsg());
                else getPlayer(false);
                break;
        }
    }

    private void checkPosition() {
        mPlayers.seekTo(Math.max(mHistory.getOpening(), mHistory.getPosition()));
        Clock.get().setCallback(this);
    }

    private void onError(String msg) {
        int position = mBinding.flag.getSelectedPosition();
        if (position == mFlagAdapter.size() - 1) {
            mBinding.widget.progress.getRoot().setVisibility(View.GONE);
            mBinding.widget.error.setVisibility(View.VISIBLE);
            mBinding.widget.text.setText(msg);
            Clock.get().setCallback(null);
            mPlayers.stop();
        } else {
            mPlayers.reset();
            Vod.Flag flag = (Vod.Flag) mFlagAdapter.get(position + 1);
            Notify.show(ResUtil.getString(R.string.play_switching, flag.getFlag()));
            setFlagActivated(flag);
        }
    }

    private void onPause(boolean visible) {
        mBinding.widget.exoPosition.setText(mPlayers.getTime(0));
        mBinding.widget.exoDuration.setText(mControl.exoDuration.getText());
        mBinding.widget.info.setVisibility(visible ? View.VISIBLE : View.GONE);
        mBinding.widget.center.setVisibility(visible ? View.VISIBLE : View.GONE);
        mPlayers.pause();
    }

    private void onPlay(int delay) {
        App.post(mHideCenter, delay);
        mPlayers.play();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mFullscreen && mControl.tracks.getVisibility() == View.VISIBLE && Utils.isMenuKey(event)) onTracks();
        else if (mFullscreen && !getPlayerView().isControllerFullyVisible() && mKeyDown.hasEvent(event)) return mKeyDown.onKeyDown(event);
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onSeeking(int time) {
        mBinding.widget.exoDuration.setText(mControl.exoDuration.getText());
        mBinding.widget.exoPosition.setText(mPlayers.getTime(time));
        mBinding.widget.action.setImageResource(time > 0 ? R.drawable.ic_forward : R.drawable.ic_rewind);
        mBinding.widget.center.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSeekTo(int time) {
        mPlayers.seekTo(time);
        mKeyDown.resetTime();
        onPlay(500);
    }

    @Override
    public void onKeyUp() {
        long current = mPlayers.getCurrentPosition();
        long half = mPlayers.getDuration() / 2;
        if (current < half) mControl.opening.requestFocus();
        else mControl.ending.requestFocus();
        getPlayerView().showController();
    }

    @Override
    public void onKeyDown() {
        getPlayerView().showController();
        mControl.next.requestFocus();
    }

    @Override
    public void onKeyCenter() {
        if (mPlayers.isPlaying()) onPause(true);
        else onPlay(0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Clock.start(mBinding.widget.time);
        onPlay(0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        RefreshEvent.history();
        Clock.get().release();
        onPause(false);
    }

    @Override
    public void onBackPressed() {
        if (getPlayerView().isControllerFullyVisible()) {
            getPlayerView().hideController();
        } else if (mFullscreen) {
            exitFullscreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayers.release();
    }
}
