package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.text.Html;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityDetailBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.ErrorEvent;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.player.ExoUtil;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.ui.adapter.EpisodeAdapter;
import com.fongmi.android.tv.ui.adapter.FlagAdapter;
import com.fongmi.android.tv.ui.adapter.ParseAdapter;
import com.fongmi.android.tv.ui.custom.CustomKeyDownVod;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Traffic;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.concurrent.ExecutorService;

import tv.danmaku.ijk.media.player.ui.IjkVideoView;

public class DetailActivity extends BaseActivity implements CustomKeyDownVod.Listener, FlagAdapter.OnClickListener, EpisodeAdapter.OnClickListener, ParseAdapter.OnClickListener, Clock.Callback {

    private ActivityDetailBinding mBinding;
    private ViewGroup.LayoutParams mFrameParams;
    private EpisodeAdapter mEpisodeAdapter;
    private ParseAdapter mParseAdapter;
    private FlagAdapter mFlagAdapter;
    private CustomKeyDownVod mKeyDown;
    private ExecutorService mExecutor;
    private SiteViewModel mViewModel;
    private boolean mFullscreen;
    private boolean mInitTrack;
    private boolean mInitAuto;
    private boolean mAutoMode;
    private History mHistory;
    private Players mPlayers;
    private int mCurrent;
    private Runnable mR1;
    private Runnable mR2;
    private Runnable mR3;

    public static void start(Activity activity, String id, String name) {
        start(activity, ApiConfig.get().getHome().getKey(), id, name);
    }

    public static void start(Activity activity, String key, String id, String name) {
        start(activity, key, id, name, false);
    }

    public static void start(Activity activity, String key, String id, String name, boolean clear) {
        Intent intent = new Intent(activity, DetailActivity.class);
        if (clear) intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("name", name);
        intent.putExtra("key", key);
        intent.putExtra("id", id);
        activity.startActivityForResult(intent, 1000);
    }

    private String getName() {
        return getIntent().getStringExtra("name");
    }

    private String getKey() {
        return getIntent().getStringExtra("key");
    }

    private String getId() {
        return getIntent().getStringExtra("id");
    }

    private String getHistoryKey() {
        return getKey().concat(AppDatabase.SYMBOL).concat(getId());
    }

    private Site getSite() {
        return ApiConfig.get().getSite(getKey());
    }

    private Parse getParse() {
        return mParseAdapter.getActivated();
    }

    private Vod.Flag getFlag() {
        return mFlagAdapter.getActivated();
    }

    private Vod.Flag.Episode getEpisode() {
        return mEpisodeAdapter.getActivated();
    }

    private int getPlayerType() {
        return mHistory != null && mHistory.getPlayer() != -1 ? mHistory.getPlayer() : getSite().getPlayerType() != -1 ? getSite().getPlayerType() : Prefers.getPlayer();
    }

    private StyledPlayerView getExo() {
        return Prefers.getRender() == 0 ? mBinding.surface : mBinding.texture;
    }

    private IjkVideoView getIjk() {
        return mBinding.ijk;
    }

    private boolean isVisible(View view) {
        return view.getVisibility() == View.VISIBLE;
    }

    private boolean isGone(View view) {
        return view.getVisibility() == View.GONE;
    }

    private boolean isReplay() {
        return Prefers.getReset() == 1;
    }

    private boolean isFromSearch() {
        return getCallingActivity().getShortClassName().contains(SearchActivity.class.getSimpleName());
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityDetailBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mKeyDown = CustomKeyDownVod.create(this);
        mFrameParams = mBinding.video.getLayoutParams();
        mBinding.progressLayout.showProgress();
        mPlayers = new Players().init();
        mR1 = this::hideControl;
        mR2 = this::setTraffic;
        mR3 = this::setSensor;
        checkOrientation();
        setRecyclerView();
        setVideoView();
        setViewModel();
        getDetail();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void initEvent() {
        mBinding.control.seek.setListener(mPlayers);
        mBinding.more.setOnClickListener(view -> onMore());
        mBinding.video.setOnClickListener(view -> onVideo());
        mBinding.control.text.setOnClickListener(this::onTrack);
        mBinding.control.audio.setOnClickListener(this::onTrack);
        mBinding.control.video.setOnClickListener(this::onTrack);
        mBinding.control.next.setOnClickListener(view -> checkNext());
        mBinding.control.prev.setOnClickListener(view -> checkPrev());
        mBinding.control.scale.setOnClickListener(view -> onScale());
        mBinding.control.speed.setOnClickListener(view -> onSpeed());
        mBinding.control.reset.setOnClickListener(view -> onReset());
        mBinding.control.player.setOnClickListener(view -> onPlayer());
        mBinding.control.decode.setOnClickListener(view -> onDecode());
        mBinding.control.ending.setOnClickListener(view -> onEnding());
        mBinding.control.opening.setOnClickListener(view -> onOpening());
        mBinding.control.speed.setOnLongClickListener(view -> onSpeedLong());
        mBinding.control.reset.setOnLongClickListener(view -> onResetToggle());
        mBinding.control.ending.setOnLongClickListener(view -> onEndingReset());
        mBinding.control.opening.setOnLongClickListener(view -> onOpeningReset());
        mBinding.video.setOnTouchListener((view, event) -> mKeyDown.onTouchEvent(event));
    }

    private void checkOrientation() {
        if (ResUtil.isLand()) enterFullscreen();
    }

    private void setRecyclerView() {
        mBinding.flag.setHasFixedSize(true);
        mBinding.flag.setItemAnimator(null);
        mBinding.flag.addItemDecoration(new SpaceItemDecoration(8));
        mBinding.flag.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mBinding.flag.setAdapter(mFlagAdapter = new FlagAdapter(this));
        mBinding.episode.setHasFixedSize(true);
        mBinding.episode.setItemAnimator(null);
        mBinding.episode.addItemDecoration(new SpaceItemDecoration(8));
        mBinding.episode.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mBinding.episode.setAdapter(mEpisodeAdapter = new EpisodeAdapter(this));
        mBinding.control.parse.setHasFixedSize(true);
        mBinding.control.parse.setItemAnimator(null);
        mBinding.control.parse.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mBinding.control.parse.setAdapter(mParseAdapter = new ParseAdapter(this));
    }

    private void setPlayerView() {
        mBinding.control.player.setText(mPlayers.getPlayerText());
        getExo().setVisibility(mPlayers.isExo() ? View.VISIBLE : View.GONE);
        getIjk().setVisibility(mPlayers.isIjk() ? View.VISIBLE : View.GONE);
        mBinding.control.reset.setText(ResUtil.getStringArray(R.array.select_reset)[Prefers.getReset()]);
    }

    private void setDecodeView() {
        mBinding.control.decode.setText(mPlayers.getDecodeText());
    }

    private void setVideoView() {
        mPlayers.set(getExo(), getIjk());
        getIjk().setRender(Prefers.getRender());
        getExo().getSubtitleView().setStyle(ExoUtil.getCaptionStyle());
    }

    private void setScale(int scale) {
        getExo().setResizeMode(scale);
        getIjk().setResizeMode(scale);
        mBinding.control.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        //mViewModel.search.observe(this, result -> setSearch(result.getList()));
        mViewModel.player.observe(this, result -> {
            boolean useParse = (result.getPlayUrl().isEmpty() && ApiConfig.get().getFlags().contains(result.getFlag())) || result.getJx() == 1;
            mBinding.control.parseLayout.setVisibility(mParseAdapter.getItemCount() > 0 && useParse ? View.VISIBLE : View.GONE);
            int timeout = getSite().isChangeable() ? Constant.TIMEOUT_PLAY : -1;
            mPlayers.start(result, useParse, timeout);
        });
        mViewModel.result.observe(this, result -> {
            if (result.getList().isEmpty()) setEmpty();
            else setDetail(result.getList().get(0));
            Notify.dismiss();
        });
    }

    private void getDetail() {
        mViewModel.detailContent(getKey(), getId());
    }

    private void getDetail(Vod item) {
        getIntent().putExtra("key", item.getSiteKey());
        getIntent().putExtra("id", item.getVodId());
        Clock.get().setCallback(null);
        Notify.progress(this);
        mPlayers.stop();
        hideProgress();
        getDetail();
    }

    private void getPlayer(Vod.Flag flag, Vod.Flag.Episode episode, boolean replay) {
        mBinding.widget.title.setText(getString(R.string.detail_title, mBinding.name.getText(), episode.getName()));
        mViewModel.playerContent(getKey(), flag.getFlag(), episode.getUrl());
        updateHistory(episode, replay);
        showProgress();
    }

    private void setEmpty() {
        if (isFromSearch()) {
            finish();
        } else if (getName().isEmpty()) {
            mBinding.progressLayout.showEmpty();
        } else {
            //checkSearch();
        }
    }

    private void setDetail(Vod item) {
        mBinding.progressLayout.showContent();
        mBinding.video.setTag(item.getVodPic());
        mBinding.name.setText(item.getVodName());
        setText(mBinding.remark, 0, item.getVodRemarks());
        setText(mBinding.year, R.string.detail_year, item.getVodYear());
        setText(mBinding.area, R.string.detail_area, item.getVodArea());
        setText(mBinding.type, R.string.detail_type, item.getTypeName());
        setText(mBinding.site, R.string.detail_site, getSite().getName());
        setText(mBinding.actor, R.string.detail_actor, Html.fromHtml(item.getVodActor()).toString());
        setText(mBinding.content, 0, Html.fromHtml(item.getVodContent()).toString());
        setText(mBinding.director, R.string.detail_director, Html.fromHtml(item.getVodDirector()).toString());
        mFlagAdapter.addAll(item.getVodFlags());
        checkFlag(item);
        checkLine();
        checkKeep();
    }

    private void setText(TextView view, int resId, String text) {
        view.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
        view.setText(resId > 0 ? getString(resId, text) : text);
        view.setTag(text);
    }

    private void checkLine() {
        mBinding.content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mBinding.content.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int lines = mBinding.content.getLayout().getLineCount() - 1;
                boolean ellipse = mBinding.content.getLayout().getEllipsisCount(lines) > 0;
                mBinding.more.setVisibility(ellipse ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onItemClick(Vod.Flag item) {
        if (item.isActivated()) return;
        mFlagAdapter.setActivated(item);
        mBinding.flag.scrollToPosition(mFlagAdapter.getPosition());
        setEpisodeAdapter(item.getEpisodes());
        seamless(item);
    }

    @Override
    public void onItemClick(Vod.Flag.Episode item) {
        if (item.isActivated()) return;
        mFlagAdapter.toggle(item);
        notifyItemChanged(mEpisodeAdapter);
        mBinding.episode.scrollToPosition(mEpisodeAdapter.getPosition());
        onRefresh();
    }

    @Override
    public void onItemClick(Parse item) {
        ApiConfig.get().setParse(item);
        notifyItemChanged(mParseAdapter);
        onRefresh();
    }

    private void setEpisodeAdapter(List<Vod.Flag.Episode> items) {
        mBinding.episode.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        mEpisodeAdapter.addAll(items);
    }

    private void seamless(Vod.Flag flag) {
        Vod.Flag.Episode episode = flag.find(mHistory.getVodRemarks());
        if (episode == null || episode.isActivated()) return;
        mHistory.setVodRemarks(episode.getName());
        onItemClick(episode);
    }

    private void reverseEpisode() {
        mFlagAdapter.reverse();
        setEpisodeAdapter(getFlag().getEpisodes());
    }

    private void onVideo() {
        if (!isFullscreen()) enterFullscreen();
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
        Vod.Flag.Episode item = mEpisodeAdapter.getNext();
        if (item.isActivated()) Notify.show(mHistory.isRevPlay() ? R.string.error_play_prev : R.string.error_play_next);
        else onItemClick(item);
    }

    private void onPrev() {
        Vod.Flag.Episode item = mEpisodeAdapter.getPrev();
        if (item.isActivated()) Notify.show(mHistory.isRevPlay() ? R.string.error_play_next : R.string.error_play_prev);
        else onItemClick(item);
    }

    private void onScale() {
        int index = mHistory.getScale();
        if (index == -1) index = Prefers.getScale();
        String[] array = ResUtil.getStringArray(R.array.select_scale);
        mHistory.setScale(index = index == array.length - 1 ? 0 : ++index);
        setScale(index);
    }

    private void onSpeed() {
        mBinding.control.speed.setText(mPlayers.addSpeed());
        mHistory.setSpeed(mPlayers.getSpeed());
    }

    private boolean onSpeedLong() {
        mBinding.control.speed.setText(mPlayers.toggleSpeed());
        mHistory.setSpeed(mPlayers.getSpeed());
        return true;
    }

    private void onRefresh() {
        Clock.get().setCallback(null);
        if (mFlagAdapter.getItemCount() == 0) return;
        if (mEpisodeAdapter.getItemCount() == 0) return;
        getPlayer(getFlag(), getEpisode(), false);
    }

    private void onReset() {
        Clock.get().setCallback(null);
        if (mFlagAdapter.getItemCount() == 0) return;
        if (mEpisodeAdapter.getItemCount() == 0) return;
        getPlayer(getFlag(), getEpisode(), isReplay());
    }

    private boolean onResetToggle() {
        Prefers.putReset(Math.abs(Prefers.getReset() - 1));
        mBinding.control.reset.setText(ResUtil.getStringArray(R.array.select_reset)[Prefers.getReset()]);
        return true;
    }

    private void onOpening() {
        long current = mPlayers.getPosition();
        long duration = mPlayers.getDuration();
        if (current < 0 || current > duration / 2) return;
        mHistory.setOpening(current);
        mBinding.control.opening.setText(mPlayers.stringToTime(mHistory.getOpening()));
    }

    private boolean onOpeningReset() {
        mHistory.setOpening(0);
        mBinding.control.opening.setText(mPlayers.stringToTime(mHistory.getOpening()));
        return true;
    }

    private void onEnding() {
        long current = mPlayers.getPosition();
        long duration = mPlayers.getDuration();
        if (current < 0 || current < duration / 2) return;
        mHistory.setEnding(duration - current);
        mBinding.control.ending.setText(mPlayers.stringToTime(mHistory.getEnding()));
    }

    private boolean onEndingReset() {
        mHistory.setEnding(0);
        mBinding.control.ending.setText(mPlayers.stringToTime(mHistory.getEnding()));
        return true;
    }

    private void onPlayer() {
        mPlayers.togglePlayer();
        Prefers.putPlayer(mPlayers.getPlayer());
        mHistory.setPlayer(mPlayers.getPlayer());
        setPlayerView();
        onRefresh();
    }

    private void onDecode() {
        mPlayers.toggleDecode();
        mPlayers.set(getExo(), getIjk());
        setDecodeView();
        onRefresh();
    }

    private void onTrack(View view) {
        int type = Integer.parseInt(view.getTag().toString());
        //TrackDialog.create(this).player(mPlayers).type(type).listener(this).show();
        hideControl();
    }

    private void enterFullscreen() {
        getIjk().getSubtitleView().setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        mBinding.video.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        App.post(() -> setFullscreen(true), 250);
    }

    private void exitFullscreen() {
        getIjk().getSubtitleView().setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mBinding.video.setLayoutParams(mFrameParams);
        App.post(mR3, 3000);
        setFullscreen(false);
        hideControl();
        hideInfo();
    }

    private void onMore() {
        boolean more = getString(R.string.vod_content_expand).equals(mBinding.more.getText().toString());
        mBinding.more.setText(more ? R.string.vod_content_collapse : R.string.vod_content_expand);
        mBinding.content.setMaxLines(more ? Integer.MAX_VALUE : 4);
    }

    private void showProgress() {
        mBinding.widget.progress.setVisibility(View.VISIBLE);
        App.post(mR2, 0);
        hideError();
    }

    private void hideProgress() {
        mBinding.widget.progress.setVisibility(View.GONE);
        App.removeCallbacks(mR2);
        Traffic.reset();
    }

    private void showError(String text) {
        mBinding.widget.text.setText(text);
        mBinding.widget.error.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        mBinding.widget.text.setText("");
        mBinding.widget.error.setVisibility(View.GONE);
    }

    private void showInfo() {
        mBinding.widget.center.setVisibility(View.VISIBLE);
        mBinding.widget.info.setVisibility(View.VISIBLE);
    }

    private void hideInfo() {
        mBinding.widget.center.setVisibility(View.GONE);
        mBinding.widget.info.setVisibility(View.GONE);
    }

    private void showControl(View view) {
        mBinding.control.getRoot().setVisibility(View.VISIBLE);
        view.requestFocus();
        setR1Callback();
    }

    private void hideControl() {
        mBinding.control.getRoot().setVisibility(View.GONE);
        App.removeCallbacks(mR1);
    }

    private void hideCenter() {
        mBinding.widget.action.setImageResource(R.drawable.ic_play);
        hideInfo();
    }

    private void setTraffic() {
        Traffic.setSpeed(mBinding.widget.traffic);
        App.post(mR2, Constant.INTERVAL_TRAFFIC);
    }

    private void setSensor() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    private void setR1Callback() {
        App.post(mR1, Constant.INTERVAL_HIDE);
    }

    private void checkFlag(Vod item) {
        mBinding.flag.setVisibility(item.getVodFlags().isEmpty() ? View.GONE : View.VISIBLE);
        if (isVisible(mBinding.flag)) checkHistory(item);
        else ErrorEvent.episode();
    }

    private void checkHistory(Vod item) {
        mHistory = History.find(getHistoryKey());
        mHistory = mHistory == null ? createHistory(item) : mHistory;
        onItemClick(mHistory.getFlag());
        if (mHistory.isRevSort()) reverseEpisode();
        setScale(mHistory.getScale() == -1 ? Prefers.getScale() : mHistory.getScale());
        mBinding.control.opening.setText(mPlayers.stringToTime(mHistory.getOpening()));
        mBinding.control.ending.setText(mPlayers.stringToTime(mHistory.getEnding()));
        mBinding.control.speed.setText(mPlayers.setSpeed(mHistory.getSpeed()));
        mPlayers.setPlayer(getPlayerType());
        setPlayerView();
        setDecodeView();
    }

    private History createHistory(Vod item) {
        History history = new History();
        history.setKey(getHistoryKey());
        history.setCid(ApiConfig.getCid());
        history.setVodPic(item.getVodPic());
        history.setVodName(item.getVodName());
        history.findEpisode(item.getVodFlags());
        return history;
    }

    private void updateHistory(Vod.Flag.Episode item, boolean replay) {
        replay = replay || !item.equals(mHistory.getEpisode());
        long position = replay ? 0 : mHistory.getPosition();
        mHistory.setPosition(position);
        mHistory.setEpisodeUrl(item.getUrl());
        mHistory.setVodRemarks(item.getName());
        mHistory.setVodFlag(getFlag().getFlag());
        mHistory.setCreateTime(System.currentTimeMillis());
    }

    private void checkKeep() {
        //mBinding.keep.setCompoundDrawablesRelativeWithIntrinsicBounds(Keep.find(getHistoryKey()) == null ? R.drawable.ic_keep_not_yet : R.drawable.ic_keep_added, 0, 0, 0);
    }

    private void createKeep() {
        Keep keep = new Keep();
        keep.setKey(getHistoryKey());
        keep.setCid(ApiConfig.getCid());
        keep.setSiteName(getSite().getName());
        keep.setVodPic(mBinding.video.getTag().toString());
        keep.setVodName(mBinding.name.getText().toString());
        keep.setCreateTime(System.currentTimeMillis());
        keep.save();
    }

    @Override
    public void onTimeChanged() {
        long current = mPlayers.getPosition();
        long duration = mPlayers.getDuration();
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
                setTrackVisible(false);
                break;
            case Player.STATE_IDLE:
                break;
            case Player.STATE_BUFFERING:
                showProgress();
                break;
            case Player.STATE_READY:
                //stopSearch();
                hideProgress();
                mPlayers.reset();
                setDefaultTrack();
                setTrackVisible(true);
                mBinding.widget.size.setText(mPlayers.getSizeText());
                break;
            case Player.STATE_ENDED:
                checkNext();
                break;
        }
    }

    private void checkPosition() {
        mPlayers.seekTo(Math.max(mHistory.getOpening(), mHistory.getPosition()), false);
        Clock.get().setCallback(this);
        setInitTrack(true);
    }

    private void setTrackVisible(boolean visible) {
        mBinding.control.text.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_TEXT) ? View.VISIBLE : View.GONE);
        mBinding.control.audio.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_AUDIO) ? View.VISIBLE : View.GONE);
        mBinding.control.video.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_VIDEO) ? View.VISIBLE : View.GONE);
    }

    private void setDefaultTrack() {
        if (isInitTrack()) {
            setInitTrack(false);
            mPlayers.setTrack(Track.find(getHistoryKey()));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onErrorEvent(ErrorEvent event) {
        if (!event.isRetry() || mPlayers.addRetry() > 3) onError(event);
        else onRefresh();
    }

    private void onError(ErrorEvent event) {
        Clock.get().setCallback(null);
        showError(event.getMsg());
        mPlayers.stop();
        hideProgress();
        //statFlow();
    }

    private void onPause(boolean visible) {
        mBinding.widget.exoDuration.setText(mPlayers.getDurationTime());
        mBinding.widget.exoPosition.setText(mPlayers.getPositionTime(0));
        if (visible) showInfo();
        else hideInfo();
        mPlayers.pause();
    }

    private void onPlay() {
        mPlayers.play();
        hideCenter();
    }

    private boolean isFullscreen() {
        return mFullscreen;
    }

    private void setFullscreen(boolean fullscreen) {
        this.mFullscreen = fullscreen;
    }

    private boolean isInitTrack() {
        return mInitTrack;
    }

    private void setInitTrack(boolean initTrack) {
        this.mInitTrack = initTrack;
    }

    private boolean isInitAuto() {
        return mInitAuto;
    }

    private void setInitAuto(boolean initAuto) {
        this.mInitAuto = initAuto;
    }

    private boolean isAutoMode() {
        return mAutoMode;
    }

    private void setAutoMode(boolean autoMode) {
        this.mAutoMode = autoMode;
    }

    private void notifyItemChanged(RecyclerView.Adapter<?> adapter) {
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
    }

    @Override
    public void onSingleTap() {
        if (isFullscreen()) {
            if (isVisible(mBinding.control.getRoot())) hideControl();
            else showControl(mBinding.control.next);
        }
    }

    @Override
    public void onDoubleTap() {
        if (isFullscreen()) {
            if (mPlayers.isPlaying()) onPause(true);
            else onPlay();
            hideControl();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (ResUtil.isPort()) {
            exitFullscreen();
        } else if (ResUtil.isLand()) {
            enterFullscreen();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Clock.start(mBinding.widget.time);
        onPlay();
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
        if (isVisible(mBinding.control.getRoot())) {
            hideControl();
        } else if (isFullscreen()) {
            exitFullscreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayers.release();
        App.removeCallbacks(mR1, mR2);
    }
}
