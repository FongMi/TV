package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
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

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityDetailBinding;
import com.fongmi.android.tv.databinding.ViewControllerBottomBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.ui.presenter.EpisodePresenter;
import com.fongmi.android.tv.ui.presenter.FlagPresenter;
import com.fongmi.android.tv.ui.presenter.GroupPresenter;
import com.fongmi.android.tv.utils.KeyDown;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.exoplayer2.Player;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends BaseActivity implements KeyDown.Listener {

    private ActivityDetailBinding mBinding;
    private ViewControllerBottomBinding mControl;
    private ViewGroup.LayoutParams mFrameParams;
    private ArrayObjectAdapter mFlagAdapter;
    private ArrayObjectAdapter mGroupAdapter;
    private ArrayObjectAdapter mEpisodeAdapter;
    private EpisodePresenter mEpisodePresenter;
    private FlagPresenter mFlagPresenter;
    private SiteViewModel mSiteViewModel;
    private boolean mFullscreen;
    private KeyDown mKeyDown;
    private Handler mHandler;
    private int mCurrent;

    private String getKey() {
        return getIntent().getStringExtra("key");
    }

    private String getId() {
        return getIntent().getStringExtra("id");
    }

    private String getHistoryKey() {
        return getKey().concat("_").concat(getId());
    }

    private Vod.Flag getVodFlag() {
        return (Vod.Flag) mFlagAdapter.get(mBinding.flag.getSelectedPosition());
    }

    private int getEpisodePosition() {
        for (int i = 0; i < mEpisodeAdapter.size(); i++) if (((Vod.Flag.Episode) mEpisodeAdapter.get(i)).isActivated()) return i;
        return 0;
    }

    public static void start(Activity activity, String id) {
        start(activity, ApiConfig.get().getHome().getKey(), id);
    }

    public static void start(Activity activity, String key, String id) {
        Intent intent = new Intent(activity, DetailActivity.class);
        intent.putExtra("key", key);
        intent.putExtra("id", id);
        activity.startActivity(intent);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityDetailBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mKeyDown = KeyDown.create(this);
        mHandler = new Handler(Looper.getMainLooper());
        mFrameParams = mBinding.video.getLayoutParams();
        mBinding.progressLayout.showProgress();
        setRecyclerView();
        setVideoView();
        setViewModel();
        getDetail();
    }

    @Override
    protected void initEvent() {
        EventBus.getDefault().register(this);
        mControl.next.setOnClickListener(view -> onNext());
        mControl.prev.setOnClickListener(view -> onPrev());
        mControl.scale.setOnClickListener(view -> onScale());
        mControl.reset.setOnClickListener(view -> getPlayer(true));
        mControl.speed.setOnClickListener(view -> mControl.speed.setText(Players.get().addSpeed()));
        mBinding.flag.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                setFlagActivated((Vod.Flag) mFlagAdapter.get(position));
            }
        });
        mBinding.group.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (mEpisodeAdapter.size() > 20) mBinding.episode.setSelectedPosition(position * 20);
            }
        });
        mBinding.video.setOnClickListener(view -> enterFullscreen());
        mFlagPresenter.setOnClickListener(this::setFlagActivated);
        mEpisodePresenter.setOnClickListener(this::setEpisodeActivated);
    }

    private void setRecyclerView() {
        mBinding.flag.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.flag.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.flag.setAdapter(new ItemBridgeAdapter(mFlagAdapter = new ArrayObjectAdapter(mFlagPresenter = new FlagPresenter())));
        mBinding.episode.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.episode.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.episode.setAdapter(new ItemBridgeAdapter(mEpisodeAdapter = new ArrayObjectAdapter(mEpisodePresenter = new EpisodePresenter())));
        mBinding.group.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.group.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.group.setAdapter(new ItemBridgeAdapter(mGroupAdapter = new ArrayObjectAdapter(new GroupPresenter())));
    }

    private void setVideoView() {
        mControl = ViewControllerBottomBinding.bind(mBinding.video.findViewById(com.google.android.exoplayer2.ui.R.id.exo_controller));
        mControl.scale.setText(ResUtil.getStringArray(R.array.select_scale)[Prefers.getScale()]);
        mControl.speed.setText(Players.get().getSpeed());
        mBinding.video.setResizeMode(Prefers.getScale());
        mBinding.video.setPlayer(Players.get().exo());
    }

    private void getDetail() {
        mSiteViewModel.detailContent(getKey(), getId());
    }

    private void getPlayer(boolean reset) {
        Vod.Flag.Episode item = (Vod.Flag.Episode) mEpisodeAdapter.get(getEpisodePosition());
        if (mFullscreen) Notify.show(ResUtil.getString(R.string.play_ready, item.getName()));
        mSiteViewModel.playerContent(getKey(), getVodFlag().getFlag(), item.getUrl());
        mBinding.progress.getRoot().setVisibility(View.VISIBLE);
        updateHistory(item, reset);
    }

    private void setViewModel() {
        mSiteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mSiteViewModel.player.observe(this, result -> Players.get().setMediaSource(result));
        mSiteViewModel.result.observe(this, result -> {
            if (result.getList().isEmpty()) mBinding.progressLayout.showErrorText();
            else setDetail(result.getList().get(0));
        });
    }

    private void setDetail(Vod item) {
        Players.get().setKey(getHistoryKey());
        mBinding.progressLayout.showContent();
        mBinding.video.setTag(item.getVodPic());
        mBinding.name.setText(item.getVodName());
        setText(mBinding.year, R.string.detail_year, item.getVodYear());
        setText(mBinding.area, R.string.detail_area, item.getVodArea());
        setText(mBinding.type, R.string.detail_type, item.getTypeName());
        setText(mBinding.actor, R.string.detail_actor, item.getVodActor());
        setText(mBinding.site, R.string.detail_site, ApiConfig.getSiteName(getKey()));
        setText(mBinding.director, R.string.detail_director, item.getVodDirector());
        setText(mBinding.content, R.string.detail_content, Html.fromHtml(item.getVodContent()).toString());
        mFlagAdapter.addAll(0, item.getVodFlags());
        mBinding.video.requestFocus();
        checkHistory();
    }

    private void setText(TextView view, int resId, String text) {
        if (text.isEmpty()) view.setVisibility(View.GONE);
        else view.setText(ResUtil.getString(resId, text));
    }

    private void setFlagActivated(Vod.Flag item) {
        if (mBinding.flag.isComputingLayout()) return;
        for (int i = 0; i < mFlagAdapter.size(); i++) {
            Vod.Flag flag = (Vod.Flag) mFlagAdapter.get(i);
            flag.setActivated(flag.equals(item));
            if (!flag.isActivated()) continue;
            mBinding.flag.setSelectedPosition(i);
            mEpisodeAdapter.setItems(flag.getEpisodes(), null);
            setGroup(flag.getEpisodes().size());
        }
        mFlagAdapter.notifyArrayItemRangeChanged(0, mFlagAdapter.size());
    }

    private void setEpisodeActivated(Vod.Flag.Episode item) {
        mCurrent = mBinding.flag.getSelectedPosition();
        for (int i = 0; i < mFlagAdapter.size(); i++) ((Vod.Flag) mFlagAdapter.get(i)).toggle(mCurrent == i, item);
        mEpisodeAdapter.notifyArrayItemRangeChanged(0, mEpisodeAdapter.size());
        mHandler.post(() -> mBinding.episode.setSelectedPosition(getEpisodePosition()));
        getPlayer(false);
    }

    private void setGroup(int size) {
        List<String> items = new ArrayList<>();
        int itemSize = (int) Math.ceil(size / 20.0f);
        for (int i = 0; i < itemSize; i++) items.add(String.valueOf(i * 20 + 1));
        mBinding.group.setVisibility(itemSize > 1 ? View.VISIBLE : View.GONE);
        mGroupAdapter.setItems(items, null);
    }

    private void enterFullscreen() {
        mBinding.video.setForeground(null);
        mBinding.video.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        mHandler.postDelayed(() -> mBinding.video.setUseController(true), 250);
        mBinding.flag.setSelectedPosition(mCurrent);
        mFullscreen = true;
    }

    private void exitFullscreen() {
        mBinding.video.setForeground(ResUtil.getDrawable(R.drawable.selector_video));
        mBinding.video.setLayoutParams(mFrameParams);
        mBinding.video.setUseController(false);
        mFullscreen = false;
    }

    private void onNext() {
        int current = getEpisodePosition();
        int max = mEpisodeAdapter.size() - 1;
        current = ++current > max ? max : current;
        Vod.Flag.Episode item = (Vod.Flag.Episode) mEpisodeAdapter.get(current);
        if (item.isActivated()) Notify.show(R.string.error_play_next);
        else setEpisodeActivated(item);
    }

    private void onPrev() {
        int current = getEpisodePosition();
        current = --current < 0 ? 0 : current;
        Vod.Flag.Episode item = (Vod.Flag.Episode) mEpisodeAdapter.get(current);
        if (item.isActivated()) Notify.show(R.string.error_play_prev);
        else setEpisodeActivated(item);
    }

    private void onScale() {
        int scale = mBinding.video.getResizeMode();
        mBinding.video.setResizeMode(scale = scale >= 4 ? 0 : scale + 1);
        mControl.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
        Prefers.putScale(scale);
    }

    private void checkHistory() {
        History history = AppDatabase.get().getHistoryDao().find(getHistoryKey());
        if (history != null) {
            setFlagActivated(history.getFlag());
            setEpisodeActivated(history.getEpisode());
        } else {
            createHistory();
            setFlagActivated((Vod.Flag) mFlagAdapter.get(0));
            setEpisodeActivated((Vod.Flag.Episode) mEpisodeAdapter.get(0));
        }
    }

    private void createHistory() {
        History history = new History();
        history.setKey(getHistoryKey());
        history.setVodPic(mBinding.video.getTag().toString());
        history.setVodName(mBinding.name.getText().toString());
        AppDatabase.get().getHistoryDao().insertOrUpdate(history);
    }

    private void updateHistory(Vod.Flag.Episode item, boolean reset) {
        History history = AppDatabase.get().getHistoryDao().find(getHistoryKey());
        reset = reset || !item.getUrl().equals(history.getEpisodeUrl());
        long duration = reset ? 0 : history.getDuration();
        history.setDuration(duration);
        history.setEpisodeUrl(item.getUrl());
        history.setVodRemarks(item.getName());
        history.setVodFlag(getVodFlag().getFlag());
        history.setCreateTime(System.currentTimeMillis());
        AppDatabase.get().getHistoryDao().update(history);
        EventBus.getDefault().post(RefreshEvent.history());
    }

    private void updateHistory() {
        History history = AppDatabase.get().getHistoryDao().find(getHistoryKey());
        if (history != null) {
            history.setDuration(Players.get().getCurrentPosition());
            AppDatabase.get().getHistoryDao().update(history);
        }
    }

    private final Runnable mHideCenter = new Runnable() {
        @Override
        public void run() {
            mBinding.center.action.setImageResource(R.drawable.ic_play);
            mBinding.center.getRoot().setVisibility(View.GONE);
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerEvent(PlayerEvent event) {
        mBinding.progress.getRoot().setVisibility(event.getState() == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
        if (event.getState() == Player.STATE_ENDED) onNext();
        Notify.show(event.getMsg());
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mFullscreen && !mBinding.video.isControllerFullyVisible() && mKeyDown.hasEvent(event)) return mKeyDown.onKeyDown(event);
        else return super.dispatchKeyEvent(event);
    }

    @Override
    public void onSeeking(int time) {
        mBinding.center.exoDuration.setText(mControl.exoDuration.getText());
        mBinding.center.exoPosition.setText(Players.get().getTime(time));
        mBinding.center.action.setImageResource(time > 0 ? R.drawable.ic_forward : R.drawable.ic_rewind);
        mBinding.center.getRoot().setVisibility(View.VISIBLE);
    }

    @Override
    public void onSeekTo(int time) {
        mHandler.postDelayed(mHideCenter, 250);
        Players.get().seekTo(time);
        Players.get().play();
        mKeyDown.resetTime();
    }

    @Override
    public void onKeyDown() {
        mBinding.video.showController();
        mControl.next.requestFocus();
    }

    @Override
    public void onKeyCenter() {
        if (Players.get().isPlaying()) {
            Players.get().pause();
            mBinding.center.getRoot().setVisibility(View.VISIBLE);
            mBinding.center.exoPosition.setText(Players.get().getTime(0));
            mBinding.center.exoDuration.setText(mControl.exoDuration.getText());
        } else {
            Players.get().play();
            mBinding.center.getRoot().setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        if (mBinding.video.isControllerFullyVisible()) {
            mBinding.video.hideController();
        } else if (mFullscreen) {
            exitFullscreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        updateHistory();
        Players.get().stop();
        EventBus.getDefault().unregister(this);
    }
}
