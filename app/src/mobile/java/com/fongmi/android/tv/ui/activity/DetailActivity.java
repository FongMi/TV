package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.text.Html;
import android.text.Layout;
import android.util.Rational;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ShareCompat;
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
import com.fongmi.android.tv.ui.custom.dialog.TrackDialog;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Traffic;
import com.fongmi.android.tv.utils.Utils;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.concurrent.ExecutorService;

import tv.danmaku.ijk.media.player.ui.IjkVideoView;

public class DetailActivity extends BaseActivity implements CustomKeyDownVod.Listener, TrackDialog.Listener, Clock.Callback, FlagAdapter.OnClickListener, EpisodeAdapter.OnClickListener, ParseAdapter.OnClickListener {

    private ViewGroup.LayoutParams mFrameParams;
    private ActivityDetailBinding mBinding;
    private EpisodeAdapter mEpisodeAdapter;
    private ParseAdapter mParseAdapter;
    private CustomKeyDownVod mKeyDown;
    private ExecutorService mExecutor;
    private SiteViewModel mViewModel;
    private FlagAdapter mFlagAdapter;
    private History mHistory;
    private Players mPlayers;
    private boolean fullscreen;
    private boolean initTrack;
    private boolean initAuto;
    private boolean autoMode;
    private boolean useParse;
    private boolean rotate;
    private boolean stop;
    private boolean lock;
    private Runnable mR1;
    private Runnable mR2;
    private Runnable mR3;

    public static void push(Activity activity, String url) {
        start(activity, "push_agent", url, url);
    }

    public static void start(Activity activity, String id, String name) {
        start(activity, ApiConfig.get().getHome().getKey(), id, name);
    }

    public static void start(Activity activity, String key, String id, String name) {
        Intent intent = new Intent(activity, DetailActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("name", name);
        intent.putExtra("key", key);
        intent.putExtra("id", id);
        activity.startActivity(intent);
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

    private int getPlayer() {
        return mHistory != null && mHistory.getPlayer() != -1 ? mHistory.getPlayer() : getSite().getPlayerType() != -1 ? getSite().getPlayerType() : Prefers.getPlayer();
    }

    private int getScale() {
        return mHistory != null && mHistory.getScale() != -1 ? mHistory.getScale() : Prefers.getScale();
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

    private boolean isFromSearch() {
        return getCallingActivity() != null && getCallingActivity().getShortClassName().contains(CollectActivity.class.getSimpleName());
    }

    private int getLockOrient() {
        if (isLock()) {
            return ResUtil.isLand(this) ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        } else if (isRotate()) {
            return ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT;
        } else if (Utils.isAutoRotate()) {
            return ActivityInfo.SCREEN_ORIENTATION_FULL_USER;
        } else {
            return ResUtil.isLand(this) ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT;
        }
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityDetailBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getIntent().putExtras(intent);
        getDetail();
    }

    @Override
    protected void initView() {
        mKeyDown = CustomKeyDownVod.create(this);
        mFrameParams = mBinding.video.getLayoutParams();
        mPlayers = new Players().init();
        mR1 = this::hideControl;
        mR2 = this::setTraffic;
        mR3 = this::setOrient;
        setRecyclerView();
        setVideoView();
        setViewModel();
        showProgress();
        getDetail();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void initEvent() {
        mBinding.control.seek.setListener(mPlayers);
        mBinding.more.setOnClickListener(view -> onMore());
        mBinding.reverse.setOnClickListener(view -> onReverse());
        mBinding.control.full.setOnClickListener(view -> onFull());
        mBinding.control.keep.setOnClickListener(view -> onKeep());
        mBinding.control.lock.setOnClickListener(view -> onLock());
        mBinding.control.play.setOnClickListener(view -> checkPlay());
        mBinding.control.next.setOnClickListener(view -> checkNext());
        mBinding.control.prev.setOnClickListener(view -> checkPrev());
        mBinding.control.share.setOnClickListener(view -> onShare());
        mBinding.control.scale.setOnClickListener(view -> onScale());
        mBinding.control.speed.setOnClickListener(view -> onSpeed());
        mBinding.control.rotate.setOnClickListener(view -> onRotate());
        mBinding.control.player.setOnClickListener(view -> onPlayer());
        mBinding.control.decode.setOnClickListener(view -> onDecode());
        mBinding.control.ending.setOnClickListener(view -> onEnding());
        mBinding.control.opening.setOnClickListener(view -> onOpening());
        mBinding.control.setting.setOnClickListener(view -> onSetting());
        mBinding.control.speed.setOnLongClickListener(view -> onSpeedLong());
        mBinding.control.ending.setOnLongClickListener(view -> onEndingReset());
        mBinding.control.opening.setOnLongClickListener(view -> onOpeningReset());
        mBinding.video.setOnTouchListener((view, event) -> mKeyDown.onTouchEvent(event));
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
        mBinding.control.parse.addItemDecoration(new SpaceItemDecoration(8));
        mBinding.control.parse.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mBinding.control.parse.setAdapter(mParseAdapter = new ParseAdapter(this));
    }

    private void setPlayerView() {
        mBinding.control.player.setText(mPlayers.getPlayerText());
        getExo().setVisibility(mPlayers.isExo() ? View.VISIBLE : View.GONE);
        getIjk().setVisibility(mPlayers.isIjk() ? View.VISIBLE : View.GONE);
    }

    private void setDecodeView() {
        mBinding.control.decode.setText(mPlayers.getDecodeText());
    }

    private void setVideoView() {
        mPlayers.set(getExo(), getIjk());
        getIjk().setRender(Prefers.getRender());
        if (ResUtil.isLand(this)) enterFullscreen();
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
            setUseParse(ApiConfig.hasParse() && ((result.getPlayUrl().isEmpty() && ApiConfig.get().getFlags().contains(result.getFlag())) || result.getJx() == 1));
            mBinding.control.parse.setVisibility(isFullscreen() && isUseParse() ? View.VISIBLE : View.GONE);
            int timeout = getSite().isChangeable() ? Constant.TIMEOUT_PLAY : -1;
            mPlayers.start(result, isUseParse(), timeout);
        });
        mViewModel.result.observe(this, result -> {
            if (result.getList().isEmpty()) setEmpty();
            else setDetail(result.getList().get(0));
            Notify.dismiss();
        });
    }

    private void getDetail() {
        mBinding.progressLayout.showProgress();
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
        mBinding.control.title.setText(getString(R.string.detail_title, mBinding.name.getText(), episode.getName()));
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
        checkKeepImg();
        checkLine();
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
                Layout layout = mBinding.content.getLayout();
                if (layout != null) {
                    int lines = layout.getLineCount() - 1;
                    boolean ellipse = layout.getEllipsisCount(lines) > 0;
                    mBinding.more.setVisibility(ellipse ? View.VISIBLE : View.GONE);
                }
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

    private void reverseEpisode(boolean scroll) {
        mFlagAdapter.reverse();
        setEpisodeAdapter(getFlag().getEpisodes());
        if (scroll) mBinding.episode.scrollToPosition(mEpisodeAdapter.getPosition());
    }

    private void onMore() {
        boolean more = getString(R.string.detail_content_expand).equals(mBinding.more.getText().toString());
        mBinding.more.setText(more ? R.string.detail_content_collapse : R.string.detail_content_expand);
        mBinding.content.setMaxLines(more ? Integer.MAX_VALUE : 4);
    }

    private void onReverse() {
        mHistory.setRevSort(!mHistory.isRevSort());
        reverseEpisode(false);
    }

    private void onFull() {
        setR1Callback();
        toggleFullscreen();
    }

    private void onKeep() {
        Keep keep = Keep.find(getHistoryKey());
        Notify.show(keep != null ? R.string.keep_del : R.string.keep_add);
        if (keep != null) keep.delete();
        else createKeep();
        RefreshEvent.keep();
        checkKeepImg();
    }

    private void onLock() {
        setLock(!isLock());
        setRequestedOrientation(getLockOrient());
        checkLockImg();
        showControl();
    }

    private void checkPlay() {
        setR1Callback();
        checkPlayImg(!mPlayers.isPlaying());
        if (mPlayers.isPlaying()) onPause(false);
        else onPlay();
    }

    private void checkNext() {
        setR1Callback();
        if (mHistory.isRevPlay()) onPrev();
        else onNext();
    }

    private void checkPrev() {
        setR1Callback();
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

    private void onShare() {
        new ShareCompat.IntentBuilder(this).setType("text/plain").setText(getId()).startChooser();
    }

    private void onScale() {
        int index = getScale();
        String[] array = ResUtil.getStringArray(R.array.select_scale);
        mHistory.setScale(index = index == array.length - 1 ? 0 : ++index);
        setScale(index);
        setR1Callback();
    }

    private void onSpeed() {
        setR1Callback();
        mBinding.control.speed.setText(mPlayers.addSpeed());
        mHistory.setSpeed(mPlayers.getSpeed());
    }

    private boolean onSpeedLong() {
        mBinding.control.speed.setText(mPlayers.toggleSpeed());
        mHistory.setSpeed(mPlayers.getSpeed());
        setR1Callback();
        return true;
    }

    private void onRefresh() {
        Clock.get().setCallback(null);
        if (mFlagAdapter.getItemCount() == 0) return;
        if (mEpisodeAdapter.getItemCount() == 0) return;
        getPlayer(getFlag(), getEpisode(), false);
    }

    private void onRotate() {
        setR1Callback();
        setRotate(!isRotate());
        setRequestedOrientation(ResUtil.isLand(this) ? ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    private void onPlayer() {
        mPlayers.togglePlayer();
        Prefers.putPlayer(mPlayers.getPlayer());
        mHistory.setPlayer(mPlayers.getPlayer());
        setPlayerView();
        setR1Callback();
        onRefresh();
    }

    private void onDecode() {
        mPlayers.toggleDecode();
        mPlayers.set(getExo(), getIjk());
        setDecodeView();
        setR1Callback();
        onRefresh();
    }

    private void onEnding() {
        long current = mPlayers.getPosition();
        long duration = mPlayers.getDuration();
        if (current < 0 || current < duration / 2) return;
        mHistory.setEnding(duration - current);
        mBinding.control.ending.setText(mPlayers.stringToTime(mHistory.getEnding()));
        setR1Callback();
    }

    private boolean onEndingReset() {
        mHistory.setEnding(0);
        mBinding.control.ending.setText(R.string.play_ed);
        setR1Callback();
        return true;
    }

    private void onOpening() {
        long current = mPlayers.getPosition();
        long duration = mPlayers.getDuration();
        if (current < 0 || current > duration / 2) return;
        mHistory.setOpening(current);
        mBinding.control.opening.setText(mPlayers.stringToTime(mHistory.getOpening()));
        setR1Callback();
    }

    private boolean onOpeningReset() {
        mHistory.setOpening(0);
        mBinding.control.opening.setText(R.string.play_op);
        setR1Callback();
        return true;
    }

    private void onSetting() {
        PopupMenu popup = new PopupMenu(this, mBinding.control.setting);
        if (mPlayers.haveTrack(C.TRACK_TYPE_TEXT)) popup.getMenu().add(0, C.TRACK_TYPE_TEXT, 0, R.string.play_track_text);
        if (mPlayers.haveTrack(C.TRACK_TYPE_AUDIO)) popup.getMenu().add(0, C.TRACK_TYPE_AUDIO, 1, R.string.play_track_audio);
        if (mPlayers.haveTrack(C.TRACK_TYPE_VIDEO)) popup.getMenu().add(0, C.TRACK_TYPE_VIDEO, 2, R.string.play_track_video);
        popup.setOnMenuItemClickListener(item -> {
            TrackDialog.create(this).player(mPlayers).type(item.getItemId()).show(getSupportFragmentManager(), null);
            return true;
        });
        if (popup.getMenu().size() > 0) {
            popup.show();
        }
    }

    private void toggleFullscreen() {
        if (isFullscreen()) exitFullscreen();
        else enterFullscreen();
    }

    private void enterFullscreen() {
        mBinding.video.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        getIjk().getSubtitleView().setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        mBinding.control.full.setImageResource(R.drawable.ic_control_full_off);
        App.post(mR3, 2000);
        setFullscreen(true);
        setRotate(false);
        hideControl();
    }

    private void exitFullscreen() {
        getIjk().getSubtitleView().setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
        mBinding.episode.scrollToPosition(mEpisodeAdapter.getPosition());
        mBinding.control.full.setImageResource(R.drawable.ic_control_full_on);
        mBinding.video.setLayoutParams(mFrameParams);
        App.post(mR3, 2000);
        setFullscreen(false);
        setRotate(false);
        hideControl();
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

    private void showControl() {
        mBinding.control.parse.setVisibility(isFullscreen() && isUseParse() ? View.VISIBLE : View.GONE);
        mBinding.control.action.setVisibility(isFullscreen() ? View.VISIBLE : View.GONE);
        mBinding.control.rotate.setVisibility(isFullscreen() ? View.VISIBLE : View.GONE);
        mBinding.control.share.setVisibility(isFullscreen() ? View.GONE : View.VISIBLE);
        mBinding.control.keep.setVisibility(isFullscreen() ? View.GONE : View.VISIBLE);
        mBinding.control.bottom.setVisibility(isLock() ? View.GONE : View.VISIBLE);
        mBinding.control.right.setVisibility(isLock() ? View.GONE : View.VISIBLE);
        mBinding.control.top.setVisibility(isLock() ? View.GONE : View.VISIBLE);
        mBinding.control.getRoot().setVisibility(View.VISIBLE);
        checkPlayImg(mPlayers.isPlaying());
        setR1Callback();
    }

    private void hideControl() {
        mBinding.control.getRoot().setVisibility(View.GONE);
        App.removeCallbacks(mR1);
    }

    private void setTraffic() {
        Traffic.setSpeed(mBinding.widget.traffic);
        App.post(mR2, Constant.INTERVAL_TRAFFIC);
    }

    private void setOrient() {
        if (Utils.isAutoRotate()) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
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

    private void checkKeepImg() {
        mBinding.control.keep.setImageResource(Keep.find(getHistoryKey()) == null ? R.drawable.ic_control_keep_off : R.drawable.ic_control_keep_on);
    }

    private void checkPlayImg(boolean playing) {
        mBinding.control.play.setImageResource(playing ? R.drawable.ic_control_pause : R.drawable.ic_control_play);
    }

    private void checkLockImg() {
        mBinding.control.lock.setImageResource(isLock() ? R.drawable.ic_control_lock_on : R.drawable.ic_control_lock_off);
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
                mBinding.control.size.setText(mPlayers.getSizeText());
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
        //startFlow();
    }

    private void onPause(boolean visible) {
        mBinding.widget.state.setVisibility(visible ? View.VISIBLE : View.GONE);
        mBinding.widget.duration.setText(mPlayers.getDurationTime());
        mBinding.widget.position.setText(mPlayers.getPositionTime(0));
        mPlayers.pause();
    }

    private void onPlay() {
        mBinding.widget.state.setVisibility(View.GONE);
        mPlayers.play();
    }

    private boolean isFullscreen() {
        return fullscreen;
    }

    private void setFullscreen(boolean fullscreen) {
        Utils.toggleFullscreen(this, this.fullscreen = fullscreen);
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

    public boolean isRotate() {
        return rotate;
    }

    public void setRotate(boolean rotate) {
        this.rotate = rotate;
    }

    public boolean isStop() {
        return stop;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public boolean isLock() {
        return lock;
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }

    private void notifyItemChanged(RecyclerView.Adapter<?> adapter) {
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
    }

    @Override
    public void onSingleTap() {
        if (isVisible(mBinding.control.getRoot())) hideControl();
        else showControl();
    }

    @Override
    public void onDoubleTap() {
        if (mPlayers.isPlaying()) onPause(true);
        else onPlay();
        hideControl();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        Rect sourceRectHint = new Rect();
        mBinding.video.getGlobalVisibleRect(sourceRectHint);
        Utils.enterPIP(this, sourceRectHint, getScale() == 2 ? new Rational(4, 3) : new Rational(16, 9));
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        mBinding.progressLayout.setVisibility(isInPictureInPictureMode ? View.GONE : View.VISIBLE);
        if (isInPictureInPictureMode) hideControl();
        else if (isStop()) finish();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (Utils.hasPIP() && isInPictureInPictureMode()) return;
        if (isFullscreen()) Utils.hideSystemUI(this);
        if (ResUtil.isLand(this)) enterFullscreen();
        if (ResUtil.isPort(this) && !isRotate()) exitFullscreen();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (isFullscreen() && hasFocus) Utils.hideSystemUI(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setStop(false);
        onPlay();
    }

    @Override
    protected void onStop() {
        super.onStop();
        onPause(false);
        setStop(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Clock.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Clock.stop();
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
        App.removeCallbacks(mR1, mR2, mR3);
    }
}
