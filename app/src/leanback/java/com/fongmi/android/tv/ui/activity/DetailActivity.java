package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.text.Html;
import android.util.TypedValue;
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
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Part;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityDetailBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.ErrorEvent;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.player.ExoUtil;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomKeyDownVod;
import com.fongmi.android.tv.ui.custom.dialog.DescDialog;
import com.fongmi.android.tv.ui.custom.dialog.TrackDialog;
import com.fongmi.android.tv.ui.presenter.ArrayPresenter;
import com.fongmi.android.tv.ui.presenter.EpisodePresenter;
import com.fongmi.android.tv.ui.presenter.FlagPresenter;
import com.fongmi.android.tv.ui.presenter.ParsePresenter;
import com.fongmi.android.tv.ui.presenter.PartPresenter;
import com.fongmi.android.tv.ui.presenter.SearchPresenter;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Traffic;
import com.fongmi.android.tv.utils.Utils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Response;
import tv.danmaku.ijk.media.player.ui.IjkVideoView;

public class DetailActivity extends BaseActivity implements CustomKeyDownVod.Listener, TrackDialog.Listener, ArrayPresenter.OnClickListener, Clock.Callback {

    private ViewGroup.LayoutParams mFrameParams;
    private EpisodePresenter mEpisodePresenter;
    private ArrayObjectAdapter mEpisodeAdapter;
    private ArrayObjectAdapter mSearchAdapter;
    private ArrayObjectAdapter mArrayAdapter;
    private ArrayObjectAdapter mParseAdapter;
    private ArrayObjectAdapter mFlagAdapter;
    private ArrayObjectAdapter mPartAdapter;
    private ActivityDetailBinding mBinding;
    private PartPresenter mPartPresenter;
    private CustomKeyDownVod mKeyDown;
    private ExecutorService mExecutor;
    private SiteViewModel mViewModel;
    private History mHistory;
    private Players mPlayers;
    private String mSiteKey;
    private boolean fullscreen;
    private boolean initTrack;
    private boolean initAuto;
    private boolean autoMode;
    private boolean useParse;
    private int mCurrent;
    private Runnable mR1;
    private Runnable mR2;

    public static void cast(Activity activity, History history) {
        start(activity, history.getSiteKey(), history.getVodId(), history.getVodName(), true, true);
    }

    public static void push(Activity activity, String url, boolean clear) {
        start(activity, "push_agent", url, url, clear);
    }

    public static void start(Activity activity, String id, String name) {
        start(activity, ApiConfig.get().getHome().getKey(), id, name);
    }

    public static void start(Activity activity, String key, String id, String name) {
        start(activity, key, id, name, false);
    }

    public static void start(Activity activity, String key, String id, String name, boolean clear) {
        start(activity, key, id, name, clear, false);
    }

    public static void start(Activity activity, String key, String id, String name, boolean clear, boolean cast) {
        Intent intent = new Intent(activity, DetailActivity.class);
        if (clear) intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("cast", cast);
        intent.putExtra("name", name);
        intent.putExtra("key", key);
        intent.putExtra("id", id);
        activity.startActivityForResult(intent, 1000);
    }

    private boolean isCast() {
        return getIntent().getBooleanExtra("cast", false);
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
        return getKey().concat(AppDatabase.SYMBOL).concat(getId()).concat(AppDatabase.SYMBOL) + ApiConfig.getCid();
    }

    private Site getSite() {
        return ApiConfig.get().getSite(getKey());
    }

    private Vod.Flag getFlag() {
        return (Vod.Flag) mFlagAdapter.get(mBinding.flag.getSelectedPosition());
    }

    private Vod.Flag.Episode getEpisode() {
        return (Vod.Flag.Episode) mEpisodeAdapter.get(getEpisodePosition());
    }

    private int getEpisodePosition() {
        for (int i = 0; i < mEpisodeAdapter.size(); i++) if (((Vod.Flag.Episode) mEpisodeAdapter.get(i)).isActivated()) return i;
        return 0;
    }

    private int getParsePosition() {
        for (int i = 0; i < mParseAdapter.size(); i++) if (((Parse) mParseAdapter.get(i)).isActivated()) return i;
        return 0;
    }

    private int getPlayer() {
        return mHistory != null && mHistory.getPlayer() != -1 ? mHistory.getPlayer() : getSite().getPlayerType() != -1 ? getSite().getPlayerType() : Prefers.getPlayer();
    }

    private int getScale() {
        return mHistory != null && mHistory.getScale() != -1 ? mHistory.getScale() : Prefers.getScale();
    }

    private PlayerView getExo() {
        return Prefers.getRender() == 0 ? mBinding.surface : mBinding.texture;
    }

    private IjkVideoView getIjk() {
        return mBinding.ijk;
    }

    private boolean isReplay() {
        return Prefers.getReset() == 1;
    }

    private boolean isFromCollect() {
        return getCallingActivity() != null && getCallingActivity().getShortClassName().contains(CollectActivity.class.getSimpleName());
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityDetailBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mKeyDown = CustomKeyDownVod.create(this, mBinding.video);
        mFrameParams = mBinding.video.getLayoutParams();
        mPlayers = new Players().init();
        mR1 = this::hideControl;
        mR2 = this::setTraffic;
        mSiteKey = getKey();
        setRecyclerView();
        setVideoView();
        setViewModel();
        getDetail();
        checkCast();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void initEvent() {
        mBinding.control.seek.setListener(mPlayers);
        mBinding.desc.setOnClickListener(view -> onDesc());
        mBinding.keep.setOnClickListener(view -> onKeep());
        mBinding.video.setOnClickListener(view -> onVideo());
        mBinding.control.text.setOnClickListener(this::onTrack);
        mBinding.control.audio.setOnClickListener(this::onTrack);
        mBinding.control.video.setOnClickListener(this::onTrack);
        mBinding.control.loop.setOnClickListener(view -> onLoop());
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
        mBinding.flag.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (mFlagAdapter.size() > 0) setFlagActivated((Vod.Flag) mFlagAdapter.get(position), false);
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
        mBinding.flag.setAdapter(new ItemBridgeAdapter(mFlagAdapter = new ArrayObjectAdapter(new FlagPresenter(item -> setFlagActivated(item, false)))));
        mBinding.episode.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.episode.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.episode.setAdapter(new ItemBridgeAdapter(mEpisodeAdapter = new ArrayObjectAdapter(mEpisodePresenter = new EpisodePresenter(this::setEpisodeActivated))));
        mBinding.array.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.array.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.array.setAdapter(new ItemBridgeAdapter(mArrayAdapter = new ArrayObjectAdapter(new ArrayPresenter(this))));
        mBinding.part.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.part.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.part.setAdapter(new ItemBridgeAdapter(mPartAdapter = new ArrayObjectAdapter(mPartPresenter = new PartPresenter(item -> initSearch(item, false)))));
        mBinding.search.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.search.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.search.setAdapter(new ItemBridgeAdapter(mSearchAdapter = new ArrayObjectAdapter(new SearchPresenter(this::setSearch))));
        mBinding.control.parse.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.control.parse.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.control.parse.setAdapter(new ItemBridgeAdapter(mParseAdapter = new ArrayObjectAdapter(new ParsePresenter(this::setParseActivated))));
        mParseAdapter.setItems(ApiConfig.get().getParses(), null);
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
        getIjk().getSubtitleView().setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    }

    private void setScale(int scale) {
        getExo().setResizeMode(scale);
        getIjk().setResizeMode(scale);
        mBinding.control.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.search.observe(this, result -> setSearch(result.getList()));
        mViewModel.player.observe(this, result -> {
            setUseParse(ApiConfig.hasParse() && ((result.getPlayUrl().isEmpty() && ApiConfig.get().getFlags().contains(result.getFlag())) || result.getJx() == 1));
            mBinding.control.parseLayout.setVisibility(isUseParse() ? View.VISIBLE : View.GONE);
            int timeout = getSite().isChangeable() ? Constant.TIMEOUT_PLAY : -1;
            mPlayers.start(result, isUseParse(), timeout);
            resetFocus();
        });
        mViewModel.result.observe(this, result -> {
            if (result.getList().isEmpty()) setEmpty();
            else setDetail(result.getList().get(0));
            Notify.dismiss();
        });
    }

    private void resetFocus() {
        findViewById(R.id.timeBar).setNextFocusUpId(isUseParse() ? R.id.parse : R.id.next);
        for (int i = 0; i < mBinding.control.actionLayout.getChildCount(); i++) {
            mBinding.control.actionLayout.getChildAt(i).setNextFocusDownId(isUseParse() ? R.id.parse : R.id.timeBar);
        }
    }

    private void checkCast() {
        if (isCast()) onVideo();
        else mBinding.progressLayout.showProgress();
    }

    private void getDetail() {
        mViewModel.detailContent(getKey(), getId());
    }

    private void getDetail(Vod item) {
        getIntent().putExtra("key", item.getSiteKey());
        getIntent().putExtra("id", item.getVodId());
        mBinding.scroll.scrollTo(0, 0);
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
        hideCenter();
    }

    private void setEmpty() {
        if (isFromCollect()) {
            finish();
        } else if (getName().isEmpty()) {
            mBinding.progressLayout.showEmpty();
        } else {
            checkSearch();
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
        setText(mBinding.content, R.string.detail_content, Html.fromHtml(item.getVodContent()).toString());
        setText(mBinding.director, R.string.detail_director, Html.fromHtml(item.getVodDirector()).toString());
        mFlagAdapter.setItems(item.getVodFlags(), null);
        mBinding.content.setMaxLines(getMaxLines());
        mBinding.video.requestFocus();
        getPart(item.getVodName());
        checkFlag(item);
        checkKeep();
    }

    private int getMaxLines() {
        int lines = 1;
        if (isGone(mBinding.actor)) ++lines;
        if (isGone(mBinding.remark)) ++lines;
        if (isGone(mBinding.director)) ++lines;
        return lines;
    }

    private void setText(TextView view, int resId, String text) {
        view.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
        view.setText(resId > 0 ? getString(resId, text) : text);
        view.setTag(text);
    }

    private void setFlagActivated(Vod.Flag item, boolean force) {
        if (mFlagAdapter.size() == 0 || item.isActivated()) return;
        for (int i = 0; i < mFlagAdapter.size(); i++) ((Vod.Flag) mFlagAdapter.get(i)).setActivated(item);
        mBinding.flag.setSelectedPosition(mFlagAdapter.indexOf(item));
        notifyItemChanged(mBinding.flag, mFlagAdapter);
        setEpisodeAdapter(item.getEpisodes());
        seamless(item, force);
    }

    private void setEpisodeAdapter(List<Vod.Flag.Episode> items) {
        mBinding.episode.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        mEpisodeAdapter.setItems(items, null);
        setArray(items.size());
    }

    private void seamless(Vod.Flag flag, boolean force) {
        if (!force && !getSite().isChangeable()) return;
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
        if (isFullscreen()) Notify.show(getString(R.string.play_ready, item.getName()));
        onRefresh();
    }

    private void reverseEpisode(boolean scroll) {
        for (int i = 0; i < mFlagAdapter.size(); i++) Collections.reverse(((Vod.Flag) mFlagAdapter.get(i)).getEpisodes());
        setEpisodeAdapter(getFlag().getEpisodes());
        if (scroll) mBinding.episode.setSelectedPosition(getEpisodePosition());
    }

    private void setParseActivated(Parse item) {
        ApiConfig.get().setParse(item);
        notifyItemChanged(mBinding.control.parse, mParseAdapter);
        onRefresh();
    }

    private void setArray(int size) {
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.play_reverse));
        items.add(getString(mHistory.getRevPlayText()));
        mEpisodePresenter.setNextFocusDown(size > 1 ? R.id.array : R.id.part);
        mPartPresenter.setNextFocusUp(size > 1 ? R.id.array : R.id.episode);
        mBinding.array.setVisibility(size > 1 ? View.VISIBLE : View.GONE);
        if (mHistory.isRevSort()) for (int i = size; i > 0; i -= 20) items.add(i + "-" + Math.max(i - 19, 1));
        else for (int i = 0; i < size; i += 20) items.add((i + 1) + "-" + Math.min(i + 20, size));
        mArrayAdapter.setItems(items, null);
    }

    @Override
    public void onRevSort() {
        mHistory.setRevSort(!mHistory.isRevSort());
        reverseEpisode(false);
    }

    @Override
    public void onRevPlay(TextView view) {
        mHistory.setRevPlay(!mHistory.isRevPlay());
        view.setText(mHistory.getRevPlayText());
        Notify.show(mHistory.getRevPlayHint());
    }

    private boolean shouldEnterFullscreen(Vod.Flag.Episode item) {
        boolean enter = !isFullscreen() && item.isActivated();
        if (enter) enterFullscreen();
        return enter;
    }

    private void enterFullscreen() {
        mBinding.video.setForeground(null);
        getIjk().getSubtitleView().setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        mBinding.video.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        mBinding.flag.setSelectedPosition(mCurrent);
        mKeyDown.setFull(true);
        setFullscreen(true);
        onPlay();
    }

    private void exitFullscreen() {
        mBinding.video.setForeground(ResUtil.getDrawable(R.drawable.selector_video));
        getIjk().getSubtitleView().setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        mBinding.video.setLayoutParams(mFrameParams);
        mKeyDown.setFull(false);
        setFullscreen(false);
        hideInfo();
    }

    private void onDesc() {
        String desc = mBinding.content.getTag().toString();
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

    private void onVideo() {
        if (!isFullscreen()) enterFullscreen();
    }

    private void onLoop() {
        mBinding.control.loop.setActivated(!mBinding.control.loop.isActivated());
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
        int index = getScale();
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
        if (mFlagAdapter.size() == 0) return;
        if (mEpisodeAdapter.size() == 0) return;
        getPlayer(getFlag(), getEpisode(), false);
    }

    private void onReset() {
        onReset(isReplay());
    }

    private void onReset(boolean replay) {
        Clock.get().setCallback(null);
        if (mFlagAdapter.size() == 0) return;
        if (mEpisodeAdapter.size() == 0) return;
        getPlayer(getFlag(), getEpisode(), replay);
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
        mBinding.control.opening.setText(R.string.play_op);
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
        mBinding.control.ending.setText(R.string.play_ed);
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
        TrackDialog.create().player(mPlayers).type(Integer.parseInt(view.getTag().toString())).show(this);
        hideControl();
    }

    private void onToggle() {
        if (isVisible(mBinding.control.getRoot())) hideControl();
        else showControl(mBinding.control.next);
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
        mBinding.widget.error.setVisibility(View.VISIBLE);
        mBinding.widget.text.setText(text);
        hideProgress();
    }

    private void hideError() {
        mBinding.widget.error.setVisibility(View.GONE);
        mBinding.widget.text.setText("");
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
        mBinding.widget.action.setImageResource(R.drawable.ic_widget_play);
        hideInfo();
    }

    private void setTraffic() {
        Traffic.setSpeed(mBinding.widget.traffic);
        App.post(mR2, Constant.INTERVAL_TRAFFIC);
    }

    private void setR1Callback() {
        App.post(mR1, Constant.INTERVAL_HIDE);
    }

    private void getPart(String source) {
        OkHttp.newCall("http://api.pullword.com/get.php?source=" + URLEncoder.encode(source.trim()) + "&param1=0&param2=0&json=1").enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<String> items = Part.get(response.body().string());
                if (!items.contains(source)) items.add(0, source);
                App.post(() -> mPartAdapter.setItems(items, null));
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                List<String> items = List.of(source);
                App.post(() -> mPartAdapter.setItems(items, null));
            }
        });
    }

    private void checkFlag(Vod item) {
        mBinding.flag.setVisibility(item.getVodFlags().isEmpty() ? View.GONE : View.VISIBLE);
        if (isVisible(mBinding.flag)) checkHistory(item);
        else ErrorEvent.episode();
    }

    private void checkHistory(Vod item) {
        mHistory = History.find(getHistoryKey());
        mHistory = mHistory == null ? createHistory(item) : mHistory;
        setFlagActivated(mHistory.getFlag(), true);
        if (mHistory.isRevSort()) reverseEpisode(true);
        mBinding.control.opening.setText(mHistory.getOpening() == 0 ? getString(R.string.play_op) : mPlayers.stringToTime(mHistory.getOpening()));
        mBinding.control.ending.setText(mHistory.getEnding() == 0 ? getString(R.string.play_ed) : mPlayers.stringToTime(mHistory.getEnding()));
        mBinding.control.speed.setText(mPlayers.setSpeed(mHistory.getSpeed()));
        mPlayers.setPlayer(getPlayer());
        setScale(getScale());
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
        mBinding.keep.setCompoundDrawablesRelativeWithIntrinsicBounds(Keep.find(getHistoryKey()) == null ? R.drawable.ic_detail_keep_off : R.drawable.ic_detail_keep_on, 0, 0, 0);
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
    public void onTrackClick(Track item) {
        item.setKey(getHistoryKey());
        item.save();
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
                stopSearch();
                hideProgress();
                mPlayers.reset();
                setDefaultTrack();
                setTrackVisible(true);
                mBinding.widget.size.setText(mPlayers.getSizeText());
                break;
            case Player.STATE_ENDED:
                checkEnded();
                break;
        }
    }

    private void checkPosition() {
        mPlayers.seekTo(Math.max(mHistory.getOpening(), mHistory.getPosition()), false);
        Clock.get().setCallback(this);
        setInitTrack(true);
    }

    private void checkEnded() {
        if (mBinding.control.loop.isActivated()) {
            onReset(true);
        } else {
            checkNext();
        }
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
        startFlow();
    }

    private void startFlow() {
        if (!getSite().isChangeable()) return;
        if (isUseParse()) checkParse();
        else checkFlag();
    }

    private void checkParse() {
        int position = getParsePosition();
        boolean last = position == mParseAdapter.size() - 1;
        boolean pass = position == 0 || last;
        if (last) initParse();
        if (pass) checkFlag();
        else nextParse(position);
    }

    private void initParse() {
        if (mParseAdapter.size() == 0) return;
        ApiConfig.get().setParse((Parse) mParseAdapter.get(0));
        notifyItemChanged(mBinding.control.parse, mParseAdapter);
    }

    private void checkFlag() {
        int position = isGone(mBinding.flag) ? -1 : mBinding.flag.getSelectedPosition();
        if (position == mFlagAdapter.size() - 1) checkSearch();
        else nextFlag(position);
    }

    private void checkSearch() {
        if (mSearchAdapter.size() == 0) initSearch(getName(), true);
        else if (isAutoMode()) nextSite();
    }

    private void initSearch(String keyword, boolean auto) {
        stopSearch();
        setAutoMode(auto);
        setInitAuto(auto);
        startSearch(keyword);
        mBinding.part.setTag(keyword);
    }

    private void startSearch(String keyword) {
        mSearchAdapter.clear();
        mExecutor = Executors.newFixedThreadPool(Constant.THREAD_POOL);
        for (Site site : ApiConfig.get().getSites()) {
            if (isAutoMode() && !site.isChangeable()) continue;
            if (isAutoMode() && site.getKey().equals(getKey())) continue;
            if (site.isSearchable()) mExecutor.execute(() -> search(site, keyword));
        }
    }

    private void stopSearch() {
        if (mExecutor != null) mExecutor.shutdownNow();
    }

    private void search(Site site, String keyword) {
        try {
            mViewModel.searchContent(site, keyword);
        } catch (Throwable ignored) {
        }
    }

    private void setSearch(List<Vod> items) {
        Iterator<Vod> iterator = items.iterator();
        while (iterator.hasNext()) if (mismatch(iterator.next())) iterator.remove();
        mSearchAdapter.addAll(mSearchAdapter.size(), items);
        mBinding.search.setVisibility(View.VISIBLE);
        if (isInitAuto()) nextSite();
    }

    private void setSearch(Vod item) {
        setAutoMode(false);
        getDetail(item);
    }

    private boolean mismatch(Vod item) {
        String keyword = mBinding.part.getTag().toString();
        if (isAutoMode()) return !item.getVodName().equals(keyword);
        else return !item.getVodName().contains(keyword);
    }

    private void nextParse(int position) {
        Parse parse = (Parse) mParseAdapter.get(position + 1);
        Notify.show(getString(R.string.play_switch_parse, parse.getName()));
        setParseActivated(parse);
    }

    private void nextFlag(int position) {
        Vod.Flag flag = (Vod.Flag) mFlagAdapter.get(position + 1);
        Notify.show(getString(R.string.play_switch_flag, flag.getFlag()));
        setFlagActivated(flag, true);
    }

    private void nextSite() {
        if (mSearchAdapter.size() == 0) return;
        Vod vod = (Vod) mSearchAdapter.get(0);
        if (vod.getSiteKey().equals(mSiteKey)) return;
        Notify.show(getString(R.string.play_switch_site, vod.getSiteName()));
        mSearchAdapter.removeItems(0, 1);
        setInitAuto(false);
        getDetail(vod);
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
        return fullscreen;
    }

    private void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    private boolean isInitTrack() {
        return initTrack;
    }

    private void setInitTrack(boolean initTrack) {
        this.initTrack = initTrack;
    }

    private boolean isInitAuto() {
        return initAuto;
    }

    private void setInitAuto(boolean initAuto) {
        this.initAuto = initAuto;
    }

    private boolean isAutoMode() {
        return autoMode;
    }

    private void setAutoMode(boolean autoMode) {
        this.autoMode = autoMode;
    }

    public boolean isUseParse() {
        return useParse;
    }

    public void setUseParse(boolean useParse) {
        this.useParse = useParse;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isFullscreen() && Utils.isMenuKey(event)) onToggle();
        if (isVisible(mBinding.control.getRoot())) setR1Callback();
        if (isFullscreen() && isGone(mBinding.control.getRoot()) && mKeyDown.hasEvent(event)) return mKeyDown.onKeyDown(event);
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBright(int progress) {
        mBinding.widget.bright.setVisibility(View.VISIBLE);
        mBinding.widget.brightProgress.setProgress(progress);
        if (progress < 35) mBinding.widget.brightIcon.setImageResource(R.drawable.ic_widget_bright_low);
        else if (progress < 70) mBinding.widget.brightIcon.setImageResource(R.drawable.ic_widget_bright_medium);
        else mBinding.widget.brightIcon.setImageResource(R.drawable.ic_widget_bright_high);
    }

    @Override
    public void onBrightEnd() {
        mBinding.widget.bright.setVisibility(View.GONE);
    }

    @Override
    public void onVolume(int progress) {
        mBinding.widget.volume.setVisibility(View.VISIBLE);
        mBinding.widget.volumeProgress.setProgress(progress);
        if (progress < 35) mBinding.widget.volumeIcon.setImageResource(R.drawable.ic_widget_volume_low);
        else if (progress < 70) mBinding.widget.volumeIcon.setImageResource(R.drawable.ic_widget_volume_medium);
        else mBinding.widget.volumeIcon.setImageResource(R.drawable.ic_widget_volume_high);
    }

    @Override
    public void onVolumeEnd() {
        mBinding.widget.volume.setVisibility(View.GONE);
    }

    @Override
    public void onSeeking(int time) {
        mBinding.widget.exoDuration.setText(mPlayers.getDurationTime());
        mBinding.widget.exoPosition.setText(mPlayers.getPositionTime(time));
        mBinding.widget.action.setImageResource(time > 0 ? R.drawable.ic_widget_forward : R.drawable.ic_widget_rewind);
        mBinding.widget.center.setVisibility(View.VISIBLE);
        hideProgress();
    }

    @Override
    public void onSeekTo(int time) {
        mKeyDown.resetTime();
        mPlayers.seekTo(time);
        showProgress();
        onPlay();
    }

    @Override
    public void onKeyUp() {
        long current = mPlayers.getPosition();
        long half = mPlayers.getDuration() / 2;
        showControl(current < half ? mBinding.control.opening : mBinding.control.ending);
    }

    @Override
    public void onKeyDown() {
        showControl(mBinding.control.next);
    }

    @Override
    public void onKeyCenter() {
        if (mPlayers.isPlaying()) onPause(true);
        else onPlay();
        hideControl();
    }

    @Override
    public void onSingleTap() {
        if (isFullscreen()) onToggle();
    }

    @Override
    public void onDoubleTap() {
        if (isFullscreen()) onKeyCenter();
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
        onPlay();
    }

    @Override
    protected void onPause() {
        super.onPause();
        RefreshEvent.history();
        onPause(false);
        Clock.stop();
    }

    @Override
    public void onBackPressed() {
        if (isVisible(mBinding.widget.center)) {
            hideCenter();
        } else if (isVisible(mBinding.control.getRoot())) {
            hideControl();
        } else if (isFullscreen()) {
            exitFullscreen();
        } else {
            stopSearch();
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
