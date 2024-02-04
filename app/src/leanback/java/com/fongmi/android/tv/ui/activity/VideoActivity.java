package com.fongmi.android.tv.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.media.MediaMetadataCompat;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.BaseGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Part;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityVideoBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.event.ErrorEvent;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.SubtitleCallback;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.player.ExoUtil;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.player.danmu.Parser;
import com.fongmi.android.tv.ui.adapter.QualityAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomKeyDownVod;
import com.fongmi.android.tv.ui.custom.CustomMovement;
import com.fongmi.android.tv.ui.dialog.DescDialog;
import com.fongmi.android.tv.ui.dialog.EpisodeDialog;
import com.fongmi.android.tv.ui.dialog.TrackDialog;
import com.fongmi.android.tv.ui.presenter.ArrayPresenter;
import com.fongmi.android.tv.ui.presenter.EpisodePresenter;
import com.fongmi.android.tv.ui.presenter.FlagPresenter;
import com.fongmi.android.tv.ui.presenter.ParsePresenter;
import com.fongmi.android.tv.ui.presenter.PartPresenter;
import com.fongmi.android.tv.ui.presenter.QuickPresenter;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Sniffer;
import com.fongmi.android.tv.utils.Traffic;
import com.fongmi.android.tv.utils.Util;
import com.github.bassaer.library.MDColor;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Trans;
import com.permissionx.guolindev.PermissionX;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import okhttp3.Call;
import okhttp3.Response;
import tv.danmaku.ijk.media.player.ui.IjkVideoView;

public class VideoActivity extends BaseActivity implements CustomKeyDownVod.Listener, TrackDialog.Listener, ArrayPresenter.OnClickListener, Clock.Callback, SubtitleCallback {

    private ActivityVideoBinding mBinding;
    private ViewGroup.LayoutParams mFrameParams;
    private EpisodePresenter mEpisodePresenter;
    private ArrayObjectAdapter mEpisodeAdapter;
    private ArrayObjectAdapter mArrayAdapter;
    private ArrayObjectAdapter mParseAdapter;
    private ArrayObjectAdapter mQuickAdapter;
    private ArrayObjectAdapter mFlagAdapter;
    private ArrayObjectAdapter mPartAdapter;
    private QualityAdapter mQualityAdapter;
    private DanmakuContext mDanmakuContext;
    private ArrayPresenter mArrayPresenter;
    private FlagPresenter mFlagPresenter;
    private PartPresenter mPartPresenter;
    private CustomKeyDownVod mKeyDown;
    private ExecutorService mExecutor;
    private SiteViewModel mViewModel;
    private List<String> mBroken;
    private History mHistory;
    private Players mPlayers;
    private boolean background;
    private boolean fullscreen;
    private boolean initTrack;
    private boolean initAuto;
    private boolean autoMode;
    private boolean useParse;
    private int currentFlag;
    private int toggleCount;
    private int groupSize;
    private Runnable mR1;
    private Runnable mR2;
    private Runnable mR3;
    private Runnable mR4;
    private Clock mClock;
    private View mFocus1;
    private View mFocus2;

    public static void push(FragmentActivity activity, String text) {
        if (FileChooser.isValid(activity, Uri.parse(text))) file(activity, FileChooser.getPathFromUri(activity, Uri.parse(text)));
        else start(activity, Sniffer.getUrl(text));
    }

    public static void file(FragmentActivity activity, String path) {
        if (TextUtils.isEmpty(path)) return;
        String name = new File(path).getName();
        PermissionX.init(activity).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> start(activity, "push_agent", "file://" + path, name, true));
    }

    public static void cast(Activity activity, History history) {
        start(activity, history.getSiteKey(), history.getVodId(), history.getVodName(), history.getVodPic(), null, true, true, false);
    }

    public static void collect(Activity activity, String key, String id, String name, String pic) {
        start(activity, key, id, name, pic, null, false, false, true);
    }

    public static void start(Activity activity, String url) {
        start(activity, url, true);
    }

    public static void start(Activity activity, String url, boolean clear) {
        start(activity, "push_agent", url, url, clear);
    }

    public static void start(Activity activity, String id, String name, String pic) {
        start(activity, VodConfig.get().getHome().getKey(), id, name, pic);
    }

    public static void start(Activity activity, String key, String id, String name, String pic) {
        start(activity, key, id, name, pic, null, false);
    }

    public static void start(Activity activity, String key, String id, String name, String pic, String mark) {
        start(activity, key, id, name, pic, mark, false);
    }

    public static void start(Activity activity, String key, String id, String name, boolean clear) {
        start(activity, key, id, name, null, null, clear, false, false);
    }

    public static void start(Activity activity, String key, String id, String name, String pic, String mark, boolean clear) {
        start(activity, key, id, name, pic, mark, clear, false, false);
    }

    public static void start(Activity activity, String key, String id, String name, String pic, String mark, boolean clear, boolean cast, boolean collect) {
        Intent intent = new Intent(activity, VideoActivity.class);
        if (clear) intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("collect", collect);
        intent.putExtra("cast", cast);
        intent.putExtra("mark", mark);
        intent.putExtra("name", name);
        intent.putExtra("pic", pic);
        intent.putExtra("key", key);
        intent.putExtra("id", id);
        activity.startActivityForResult(intent, 1000);
    }

    private boolean isCast() {
        return getIntent().getBooleanExtra("cast", false);
    }

    private String getName() {
        return Objects.toString(getIntent().getStringExtra("name"), "");
    }

    private String getPic() {
        return Objects.toString(getIntent().getStringExtra("pic"), "");
    }

    private String getMark() {
        return Objects.toString(getIntent().getStringExtra("mark"), "");
    }

    private String getKey() {
        return Objects.toString(getIntent().getStringExtra("key"), "");
    }

    private String getId() {
        return Objects.toString(getIntent().getStringExtra("id"), "");
    }

    private String getHistoryKey() {
        return getKey().concat(AppDatabase.SYMBOL).concat(getId()).concat(AppDatabase.SYMBOL) + VodConfig.getCid();
    }

    private Site getSite() {
        return VodConfig.get().getSite(getKey());
    }

    private Flag getFlag() {
        return (Flag) mFlagAdapter.get(mBinding.flag.getSelectedPosition());
    }

    private Episode getEpisode() {
        return (Episode) mEpisodeAdapter.get(getEpisodePosition());
    }

    private int getEpisodePosition() {
        for (int i = 0; i < mEpisodeAdapter.size(); i++) if (((Episode) mEpisodeAdapter.get(i)).isActivated()) return i;
        return 0;
    }

    private int getParsePosition() {
        for (int i = 0; i < mParseAdapter.size(); i++) if (((Parse) mParseAdapter.get(i)).isActivated()) return i;
        return 0;
    }

    private int getPlayer() {
        return mHistory != null && mHistory.getPlayer() != -1 ? mHistory.getPlayer() : getSite().getPlayerType() != -1 ? getSite().getPlayerType() : Setting.getPlayer();
    }

    private int getScale() {
        return mHistory != null && mHistory.getScale() != -1 ? mHistory.getScale() : Setting.getScale();
    }

    private PlayerView getExo() {
        return Setting.getRender() == 0 ? mBinding.surface : mBinding.texture;
    }

    private IjkVideoView getIjk() {
        return mBinding.ijk;
    }

    private BaseGridView getEpisodeView() {
        return Setting.getEpisode() == 0 ? mBinding.episodeHori : mBinding.episodeVert;
    }

    private void setEpisodeSelectedPosition(int position) {
        getEpisodeView().setSelectedPosition(position);
    }

    private boolean isReplay() {
        return Setting.getReset() == 1;
    }

    private boolean isFromCollect() {
        return getIntent().getBooleanExtra("collect", false);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityVideoBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mKeyDown = CustomKeyDownVod.create(this, mBinding.video);
        mFrameParams = mBinding.video.getLayoutParams();
        mClock = Clock.create(mBinding.display.time);
        mDanmakuContext = DanmakuContext.create();
        mPlayers = new Players().init(this);
        mBroken = new ArrayList<>();
        mR1 = this::hideControl;
        mR2 = this::updateFocus;
        mR3 = this::setTraffic;
        mR4 = this::showEmpty;
        setBackground(false);
        setRecyclerView();
        setEpisodeView();
        setVideoView();
        setDisplayView();
        setDanmuView();
        setViewModel();
        checkCast();
        checkId();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void initEvent() {
        mBinding.control.seek.setListener(mPlayers);
        mBinding.desc.setOnClickListener(view -> onDesc());
        mBinding.keep.setOnClickListener(view -> onKeep());
        mBinding.video.setOnClickListener(view -> onVideo());
        mBinding.change1.setOnClickListener(view -> onChange());
        mBinding.control.text.setOnClickListener(this::onTrack);
        mBinding.control.audio.setOnClickListener(this::onTrack);
        mBinding.control.video.setOnClickListener(this::onTrack);
        mBinding.control.text.setAddListener(this::onTextAdd);
        mBinding.control.text.setSubListener(this::onTextSub);
        mBinding.control.speed.setAddListener(this::onSpeedAdd);
        mBinding.control.speed.setSubListener(this::onSpeedSub);
        mBinding.control.ending.setAddListener(this::onEndingAdd);
        mBinding.control.ending.setSubListener(this::onEndingSub);
        mBinding.control.opening.setAddListener(this::onOpeningAdd);
        mBinding.control.opening.setSubListener(this::onOpeningSub);
        mBinding.control.loop.setOnClickListener(view -> onLoop());
        mBinding.control.danmu.setOnClickListener(view -> onDanmu());
        mBinding.control.next.setOnClickListener(view -> checkNext());
        mBinding.control.prev.setOnClickListener(view -> checkPrev());
        mBinding.control.episodes.setOnClickListener(view -> onEpisodes());
        mBinding.control.scale.setOnClickListener(view -> onScale());
        mBinding.control.speed.setOnClickListener(view -> onSpeed());
        mBinding.control.reset.setOnClickListener(view -> onReset());
        mBinding.control.player.setOnClickListener(view -> onPlayer());
        mBinding.control.decode.setOnClickListener(view -> onDecode());
        mBinding.control.ending.setOnClickListener(view -> onEnding());
        mBinding.control.change2.setOnClickListener(view -> onChange());
        mBinding.control.opening.setOnClickListener(view -> onOpening());
        mBinding.control.player.setOnLongClickListener(view -> onChoose());
        mBinding.control.speed.setOnLongClickListener(view -> onSpeedLong());
        mBinding.control.reset.setOnLongClickListener(view -> onResetToggle());
        mBinding.control.ending.setOnLongClickListener(view -> onEndingReset());
        mBinding.control.opening.setOnLongClickListener(view -> onOpeningReset());
        mBinding.video.setOnTouchListener((view, event) -> mKeyDown.onTouchEvent(event));
        mBinding.flag.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (mFlagAdapter.size() > 0) setFlagActivated((Flag) mFlagAdapter.get(position));
            }
        });
        getEpisodeView().addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (child != null && mBinding.video != mFocus1) mFocus1 = child.itemView;
            }
        });
        mBinding.array.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (mEpisodeAdapter.size() > getGroupSize() && position > 1) setEpisodeSelectedPosition((position - 2) * getGroupSize());
            }
        });
    }

    private void setRecyclerView() {
        mBinding.flag.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.flag.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.flag.setAdapter(new ItemBridgeAdapter(mFlagAdapter = new ArrayObjectAdapter(mFlagPresenter = new FlagPresenter(this::setFlagActivated))));
        mBinding.quality.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.quality.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.quality.setAdapter(mQualityAdapter = new QualityAdapter(this::setQualityActivated));
        mBinding.array.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.array.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.array.setAdapter(new ItemBridgeAdapter(mArrayAdapter = new ArrayObjectAdapter(mArrayPresenter = new ArrayPresenter(this))));
        mBinding.part.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.part.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.part.setAdapter(new ItemBridgeAdapter(mPartAdapter = new ArrayObjectAdapter(mPartPresenter = new PartPresenter(item -> initSearch(item, false)))));
        mBinding.quick.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.quick.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.quick.setAdapter(new ItemBridgeAdapter(mQuickAdapter = new ArrayObjectAdapter(new QuickPresenter(this::setSearch))));
        mBinding.control.parse.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.control.parse.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.control.parse.setAdapter(new ItemBridgeAdapter(mParseAdapter = new ArrayObjectAdapter(new ParsePresenter(this::setParseActivated))));
        mParseAdapter.setItems(VodConfig.get().getParses(), null);
    }

    private void setEpisodeView() {
        mBinding.episodeVert.setVerticalSpacing(ResUtil.dp2px(8));
        mBinding.episodeHori.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.episodeVert.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.episodeHori.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        getEpisodeView().setAdapter(new ItemBridgeAdapter(mEpisodeAdapter = new ArrayObjectAdapter(mEpisodePresenter = new EpisodePresenter(this::setEpisodeActivated))));
    }

    private void setVideoView() {
        mPlayers.set(getExo(), getIjk());
        getExo().getSubtitleView().setStyle(ExoUtil.getCaptionStyle());
        getIjk().getSubtitleView().setStyle(ExoUtil.getCaptionStyle());
        mBinding.control.reset.setText(ResUtil.getStringArray(R.array.select_reset)[Setting.getReset()]);
        setSubtitle(16);
    }

    private void setDanmuView() {
        int maxLine = Setting.getDanmuLine(3);
        mPlayers.setDanmuView(mBinding.danmaku);
        float[] range = {2.4f, 1.8f, 1.2f, 0.8f};
        float speed = range[Setting.getDanmuSpeed()];
        float alpha = Setting.getDanmuAlpha() / 100.0f;
        float sizeScale = Setting.getDanmuSize();
        HashMap<Integer, Integer> maxLines = new HashMap<>();
        maxLines.put(BaseDanmaku.TYPE_FIX_TOP, maxLine);
        maxLines.put(BaseDanmaku.TYPE_SCROLL_RL, maxLine);
        maxLines.put(BaseDanmaku.TYPE_SCROLL_LR, maxLine);
        maxLines.put(BaseDanmaku.TYPE_FIX_BOTTOM, maxLine);
        mDanmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3).setMaximumLines(maxLines).setScrollSpeedFactor(speed).setDanmakuTransparency(alpha).setDanmakuMargin(12).setScaleTextSize(sizeScale);
        mBinding.control.danmu.setActivated(Setting.isDanmu());
    }

    private void setDisplayView() {
        mBinding.display.getRoot().setVisibility(View.VISIBLE);
        showDisplayInfo();
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(this, this::setDetail);
        mViewModel.player.observe(this, this::setPlayer);
        mViewModel.search.observe(this, result -> setSearch(result.getList()));
    }

    private void checkCast() {
        if (isCast()) onVideo();
        else mBinding.progressLayout.showProgress();
    }

    private void checkId() {
        if (getId().startsWith("push://")) getIntent().putExtra("key", "push_agent").putExtra("id", getId().substring(7));
        if (getId().isEmpty() || getId().startsWith("msearch:")) setEmpty(false);
        else getDetail();
    }

    private void setPlayerView() {
        getIjk().setPlayer(mPlayers.getPlayer());
        mBinding.control.player.setText(mPlayers.getPlayerText());
        getExo().setVisibility(mPlayers.isExo() ? View.VISIBLE : View.GONE);
        getIjk().setVisibility(mPlayers.isIjk() ? View.VISIBLE : View.GONE);
    }

    private void setDecodeView() {
        mBinding.control.decode.setText(mPlayers.getDecodeText());
    }

    private void setScale(int scale) {
        getExo().setResizeMode(scale);
        getIjk().setResizeMode(scale);
        mBinding.control.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
    }

    private void getDetail() {
        mViewModel.detailContent(getKey(), getId());
    }

    private void getDetail(Vod item) {
        getIntent().putExtra("key", item.getSiteKey());
        getIntent().putExtra("pic", item.getVodPic());
        getIntent().putExtra("id", item.getVodId());
        mBinding.scroll.scrollTo(0, 0);
        mClock.setCallback(null);
        mPlayers.stop();
        getDetail();
    }

    private void setDetail(Result result) {
        if (result.getList().isEmpty()) setEmpty(result.hasMsg());
        else setDetail(result.getList().get(0));
        Notify.show(result.getMsg());
    }

    private void getPlayer(Flag flag, Episode episode, boolean replay) {
        mBinding.widget.title.setText(getString(R.string.detail_title, mBinding.name.getText(), episode.getName()));
        mViewModel.playerContent(getKey(), flag.getFlag(), episode.getUrl());
        updateHistory(episode, replay);
        mPlayers.clear();
        showProgress();
        setMetadata();
        hidePreview();
        hideCenter();
    }

    private void setPlayer(Result result) {
        result.getUrl().set(mQualityAdapter.getPosition());
        setUseParse(VodConfig.hasParse() && ((result.getPlayUrl().isEmpty() && VodConfig.get().getFlags().contains(result.getFlag())) || result.getJx() == 1));
        mPlayers.start(result, isUseParse(), getSite().isChangeable() ? getSite().getTimeout() : -1);
        mBinding.control.parse.setVisibility(isUseParse() ? View.VISIBLE : View.GONE);
        setQualityVisible(result.getUrl().isMulti());
        checkDanmu(result.getDanmaku());
        mQualityAdapter.addAll(result);
    }

    private void checkDanmu(String danmu) {
        mBinding.danmaku.release();
        if (!Setting.isDanmuLoad()) return;
        mBinding.danmaku.setVisibility(danmu.isEmpty() ? View.GONE : View.VISIBLE);
        if (danmu.length() > 0) App.execute(() -> mBinding.danmaku.prepare(new Parser(danmu), mDanmakuContext));
    }

    private void setEmpty(boolean finish) {
        if (isFromCollect() || finish) {
            finish();
        } else if (getName().isEmpty()) {
            showEmpty();
        } else {
            mBinding.name.setText(getName());
            App.post(mR4, 10000);
            checkSearch(false);
        }
    }

    private void showEmpty() {
        mBinding.progressLayout.showEmpty();
        stopSearch();
    }

    private void setDetail(Vod item) {
        mBinding.progressLayout.showContent();
        mBinding.video.setTag(item.getVodPic(getPic()));
        mBinding.name.setText(item.getVodName(getName()));
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
        setArtwork(item.getVodPic());
        getPart(item.getVodName());
        App.removeCallbacks(mR4);
        checkHistory(item);
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
        view.setText(getSpan(resId, text), TextView.BufferType.SPANNABLE);
        view.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
        view.setLinkTextColor(MDColor.YELLOW_500);
        CustomMovement.bind(view);
        view.setTag(text);
    }

    private SpannableStringBuilder getSpan(int resId, String text) {
        if (resId > 0) text = getString(resId, text);
        Map<String, String> map = new HashMap<>();
        Matcher m = Sniffer.CLICKER.matcher(text);
        while (m.find()) {
            String key = Trans.s2t(m.group(2)).trim();
            text = text.replace(m.group(), key);
            map.put(key, m.group(1));
        }
        SpannableStringBuilder span = SpannableStringBuilder.valueOf(text);
        for (String s : map.keySet()) {
            int index = text.indexOf(s);
            Result result = Result.type(map.get(s));
            span.setSpan(getClickSpan(result), index, index + s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return span;
    }

    private ClickableSpan getClickSpan(Result result) {
        return new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                VodActivity.start(getActivity(), getKey(), result);
            }
        };
    }

    private void setFlagActivated(Flag item) {
        if (mFlagAdapter.size() == 0 || item.isActivated()) return;
        for (int i = 0; i < mFlagAdapter.size(); i++) ((Flag) mFlagAdapter.get(i)).setActivated(item);
        mBinding.flag.setSelectedPosition(mFlagAdapter.indexOf(item));
        notifyItemChanged(mBinding.flag, mFlagAdapter);
        setEpisodeAdapter(item.getEpisodes());
        setQualityVisible(false);
        seamless(item);
    }

    private void setEpisodeAdapter(List<Episode> items) {
        getEpisodeView().setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        if (isVisible(mBinding.episodeVert)) setEpisodeView(items);
        mEpisodeAdapter.setItems(items, null);
        setArrayAdapter(items.size());
        setR2Callback();
    }

    private void setEpisodeView(List<Episode> items) {
        int size = items.size();
        int episodeNameLength = items.isEmpty() ? 0 : items.get(0).getName().length();
        for (int i = 0; i < size; i++) {
            items.get(i).setIndex(i);
            int length = items.get(i).getName() == null ? 0 : items.get(i).getName().length();
            if (length > episodeNameLength) episodeNameLength = length;
        }
        int numColumns = 10;
        if (episodeNameLength > 30) numColumns = 2;
        else if (episodeNameLength > 15) numColumns = 3;
        else if (episodeNameLength > 10) numColumns = 4;
        else if (episodeNameLength > 6) numColumns = 6;
        else if (episodeNameLength > 4) numColumns = 8;
        int rowNum = (int) Math.ceil((double) size / (double) numColumns);
        int width = ResUtil.getScreenWidth() - ResUtil.dp2px(48);
        ViewGroup.LayoutParams params = mBinding.episodeVert.getLayoutParams();
        params.width = ResUtil.getScreenWidth();
        params.height = rowNum > 6 ? ResUtil.dp2px(300) : ResUtil.dp2px(rowNum * 44);
        mBinding.episodeVert.setNumColumns(numColumns);
        mBinding.episodeVert.setColumnWidth((width - ((numColumns - 1) * ResUtil.dp2px(8))) / numColumns);
        mBinding.episodeVert.setLayoutParams(params);
        mBinding.episodeVert.setWindowAlignmentOffsetPercent(10f);
        mEpisodePresenter.setNumColumns(numColumns);
        mEpisodePresenter.setNumRows(rowNum);
    }

    private void seamless(Flag flag) {
        Episode episode = flag.find(mHistory.getVodRemarks(), getMark().isEmpty());
        setQualityVisible(episode != null && episode.isActivated() && mQualityAdapter.getItemCount() > 1);
        if (episode == null || episode.isActivated()) return;
        if (Setting.getFlag() == 1) {
            episode.setActivated(true);
            getEpisodeView().requestFocus();
            setEpisodeSelectedPosition(getEpisodePosition());
            episode.setActivated(false);
        } else {
            mHistory.setVodRemarks(episode.getName());
            setEpisodeActivated(episode);
            hidePreview();
        }
    }

    public void setEpisodeActivated(Episode item) {
        if (shouldEnterFullscreen(item)) return;
        setCurrentFlag(mBinding.flag.getSelectedPosition());
        for (int i = 0; i < mFlagAdapter.size(); i++) ((Flag) mFlagAdapter.get(i)).toggle(getCurrentFlag() == i, item);
        setEpisodeSelectedPosition(getEpisodePosition());
        notifyItemChanged(getEpisodeView(), mEpisodeAdapter);
        if (mEpisodeAdapter.size() == 0) return;
        if (isFullscreen()) Notify.show(getString(R.string.play_ready, item.getName()));
        onRefresh();
    }

    private void setQualityVisible(boolean visible) {
        mBinding.quality.setVisibility(visible ? View.VISIBLE : View.GONE);
        setR2Callback();
    }

    private void setQualityActivated(Result result) {
        try {
            result.setUrl(Source.get().fetch(result));
            mPlayers.start(result, isUseParse(), getSite().isChangeable() ? getSite().getTimeout() : -1);
            mBinding.danmaku.hide();
        } catch (Exception e) {
            ErrorEvent.extract(e.getMessage());
            e.printStackTrace();
        }
    }

    private void reverseEpisode(boolean scroll) {
        for (int i = 0; i < mFlagAdapter.size(); i++) Collections.reverse(((Flag) mFlagAdapter.get(i)).getEpisodes());
        setEpisodeAdapter(getFlag().getEpisodes());
        if (scroll) setEpisodeSelectedPosition(getEpisodePosition());
    }

    private void setParseActivated(Parse item) {
        VodConfig.get().setParse(item);
        notifyItemChanged(mBinding.control.parse, mParseAdapter);
        onRefresh();
    }

    private void setArrayAdapter(int size) {
        if (size > 200) setGroupSize(100);
        else if (size > 100) setGroupSize(40);
        else setGroupSize(20);
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.play_reverse));
        items.add(getString(mHistory.getRevPlayText()));
        mBinding.array.setVisibility(size > 1 ? View.VISIBLE : View.GONE);
        if (mHistory.isRevSort()) for (int i = size; i > 0; i -= getGroupSize()) items.add(i + "-" + Math.max(i - (getGroupSize() - 1), 1));
        else for (int i = 0; i < size; i += getGroupSize()) items.add((i + 1) + "-" + Math.min(i + getGroupSize(), size));
        mArrayAdapter.setItems(items, null);
    }

    private int findFocusDown(int index) {
        List<Integer> orders = Arrays.asList(R.id.flag, R.id.quality, R.id.episodeHori, R.id.array, R.id.episodeVert, R.id.part, R.id.quick);
        for (int i = 0; i < orders.size(); i++) if (i > index) if (isVisible(findViewById(orders.get(i)))) return orders.get(i);
        return 0;
    }

    private int findFocusUp(int index) {
        List<Integer> orders = Arrays.asList(R.id.flag, R.id.quality, R.id.episodeHori, R.id.array, R.id.episodeVert, R.id.part, R.id.quick);
        for (int i = orders.size() - 1; i >= 0; i--) if (i < index) if (isVisible(findViewById(orders.get(i)))) return orders.get(i);
        return 0;
    }

    private void updateFocus() {
        mEpisodePresenter.setNextFocusDown(findFocusDown(Setting.getEpisode() == 0 ? 2 : 4));
        mEpisodePresenter.setNextFocusUp(findFocusUp(Setting.getEpisode() == 0 ? 2 : 4));
        mQualityAdapter.setNextFocusDown(findFocusDown(1));
        mArrayPresenter.setNextFocusDown(findFocusDown(3));
        mFlagPresenter.setNextFocusDown(findFocusDown(0));
        mArrayPresenter.setNextFocusUp(findFocusUp(3));
        mPartPresenter.setNextFocusUp(findFocusUp(5));
        notifyItemChanged(getEpisodeView(), mEpisodeAdapter);
        notifyItemChanged(mBinding.quality, mQualityAdapter);
        notifyItemChanged(mBinding.part, mPartAdapter);
        notifyItemChanged(mBinding.flag, mFlagAdapter);
    }

    private void showDisplayInfo() {
        mBinding.display.time.setVisibility(Setting.isDisplayTime() || isVisible(mBinding.widget.info)  ? View.VISIBLE : View.GONE);
        mBinding.display.netspeed.setVisibility(Setting.isDisplaySpeed() && !isVisible(mBinding.control.getRoot()) ? View.VISIBLE : View.GONE);
        mBinding.display.duration.setVisibility(Setting.isDisplayDuration() && !isVisible(mBinding.control.getRoot()) ? View.VISIBLE : View.GONE);
    }

    private void onTimeChangeDisplaySpeed() {
        boolean visible = isVisible(mBinding.control.getRoot());
        long position = mPlayers.getPosition();
        if (Setting.isDisplaySpeed() && !visible) Traffic.setSpeed(mBinding.display.netspeed);
        if (Setting.isDisplayDuration() && !visible && position > 0) mBinding.display.duration.setText(mPlayers.getPositionTime(0) + "/" + mPlayers.getDurationTime());
        showDisplayInfo();
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

    private boolean shouldEnterFullscreen(Episode item) {
        boolean enter = !isFullscreen() && item.isActivated();
        if (enter) enterFullscreen();
        return enter;
    }

    private void enterFullscreen() {
        mFocus1 = getCurrentFocus();
        mBinding.video.requestFocus();
        mBinding.video.setForeground(null);
        mBinding.video.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        mBinding.flag.setSelectedPosition(getCurrentFlag());
        mDanmakuContext.setScaleTextSize(1.2f * Setting.getDanmuSize());
        setSubtitle(Setting.getSubtitle());
        mKeyDown.setFull(true);
        setFullscreen(true);
        mFocus2 = null;
        onPlay();
    }

    private void exitFullscreen() {
        mBinding.video.setForeground(ResUtil.getDrawable(R.drawable.selector_video));
        mBinding.video.setLayoutParams(mFrameParams);
        mDanmakuContext.setScaleTextSize(0.8f * Setting.getDanmuSize());
        getFocus1().requestFocus();
        mKeyDown.setFull(false);
        setFullscreen(false);
        setSubtitle(16);
        mFocus2 = null;
        hideInfo();
    }

    private void onDesc() {
        CharSequence desc = mBinding.content.getText();
        if (desc.length() > 3) DescDialog.show(this, desc.subSequence(3, desc.length()));
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

    private void onChange() {
        checkSearch(true);
    }

    private void onLoop() {
        mBinding.control.loop.setActivated(!mBinding.control.loop.isActivated());
    }

    private void onDanmu() {
        Setting.putDanmu(!Setting.isDanmu());
        mBinding.control.danmu.setActivated(Setting.isDanmu());
        showDanmu();
    }

    private void showDanmu() {
        if (Setting.isDanmu()) mBinding.danmaku.show();
        else mBinding.danmaku.hide();
    }

    private void onEpisodes() {
        EpisodeDialog.create().episodes(getFlag().getEpisodes()).show(this);
        hideControl();
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
        Episode item = (Episode) mEpisodeAdapter.get(current);
        if (item.isActivated()) Notify.show(mHistory.isRevPlay() ? R.string.error_play_prev : R.string.error_play_next);
        else setEpisodeActivated(item);
    }

    private void onPrev() {
        int current = getEpisodePosition();
        current = --current < 0 ? 0 : current;
        Episode item = (Episode) mEpisodeAdapter.get(current);
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

    private void onSpeedAdd() {
        mBinding.control.speed.setText(mPlayers.addSpeed(0.25f));
        mHistory.setSpeed(mPlayers.getSpeed());
    }

    private void onSpeedSub() {
        mBinding.control.speed.setText(mPlayers.subSpeed(0.25f));
        mHistory.setSpeed(mPlayers.getSpeed());
    }

    private boolean onSpeedLong() {
        mBinding.control.speed.setText(mPlayers.toggleSpeed());
        mHistory.setSpeed(mPlayers.getSpeed());
        return true;
    }

    private void onRefresh() {
        onReset(false);
    }

    private void onReset() {
        onReset(isReplay());
    }

    private void onReset(boolean replay) {
        mClock.setCallback(null);
        mBinding.control.seek.reset();
        if (mFlagAdapter.size() == 0) return;
        if (mEpisodeAdapter.size() == 0) return;
        getPlayer(getFlag(), getEpisode(), replay);
    }

    private boolean onResetToggle() {
        Setting.putReset(Math.abs(Setting.getReset() - 1));
        mBinding.control.reset.setText(ResUtil.getStringArray(R.array.select_reset)[Setting.getReset()]);
        return true;
    }

    private void onOpening() {
        long current = mPlayers.getPosition();
        long duration = mPlayers.getDuration();
        if (current < 0 || current > duration / 2) return;
        setOpening(current);
    }

    private void onOpeningAdd() {
        setOpening(Math.min(mHistory.getOpening() + 1000, mPlayers.getDuration() / 2));
    }

    private void onOpeningSub() {
        setOpening(Math.max(0, mHistory.getOpening() - 1000));
    }

    private boolean onOpeningReset() {
        setOpening(0);
        return true;
    }

    private void setOpening(long opening) {
        mHistory.setOpening(opening);
        mBinding.control.opening.setText(opening == 0 ? getString(R.string.play_op) : mPlayers.stringToTime(mHistory.getOpening()));
    }

    private void onEnding() {
        long current = mPlayers.getPosition();
        long duration = mPlayers.getDuration();
        if (current < 0 || current < duration / 2) return;
        setEnding(duration - current);
    }

    private void onEndingAdd() {
        setEnding(Math.min(mPlayers.getDuration() / 2, mHistory.getEnding() + 1000));
    }

    private void onEndingSub() {
        setEnding(Math.max(0, mHistory.getEnding() - 1000));
    }

    private boolean onEndingReset() {
        setEnding(0);
        return true;
    }

    private void setEnding(long ending) {
        mHistory.setEnding(ending);
        mBinding.control.ending.setText(ending == 0 ? getString(R.string.play_ed) : mPlayers.stringToTime(mHistory.getEnding()));
    }

    private boolean onChoose() {
        if (mPlayers.isEmpty()) return false;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra("return_result", true);
        intent.putExtra("headers", mPlayers.getHeaderArray());
        intent.putExtra("position", (int) mPlayers.getPosition());
        intent.putExtra("title", mBinding.widget.title.getText());
        intent.setDataAndType(mPlayers.getUri(), "video/*");
        startActivityForResult(Util.getChooser(intent), 1001);
        return true;
    }

    private void onPlayer() {
        mPlayers.togglePlayer();
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

    private void onTextAdd() {
        int size = Math.min(Setting.getSubtitle() + 1, 48);
        mBinding.control.text.setText(String.valueOf(size));
        Setting.putSubtitle(size);
        setSubtitle(size);
    }

    private void onTextSub() {
        int size = Math.max(Setting.getSubtitle() - 1, 14);
        mBinding.control.text.setText(String.valueOf(size));
        Setting.putSubtitle(size);
        setSubtitle(size);
    }

    private void onToggle() {
        if (isVisible(mBinding.control.getRoot())) hideControl();
        else showControl(getFocus2());
    }

    private void showProgress() {
        mBinding.widget.progress.setVisibility(View.VISIBLE);
        App.post(mR3, 0);
        hideError();
    }

    private void hideProgress() {
        mBinding.widget.progress.setVisibility(View.GONE);
        App.removeCallbacks(mR3);
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
        mBinding.widget.info.setVisibility(View.VISIBLE);
        showDisplayInfo();
    }

    private void hideInfo() {
        mBinding.widget.info.setVisibility(View.GONE);
        showDisplayInfo();
    }

    private void showInfoAndCenter() {
        showInfo();
        mBinding.widget.center.setVisibility(View.VISIBLE);
    }

    private void hideInfoAndCenter() {
        hideInfo();
        mBinding.widget.center.setVisibility(View.GONE);
    }

    private void showControl(View view) {
        mBinding.control.danmu.setVisibility(mBinding.danmaku.isPrepared() ? View.VISIBLE : View.GONE);
        mBinding.control.getRoot().setVisibility(View.VISIBLE);
        view.requestFocus();
        setR1Callback();
    }

    private void hideControl() {
        hideControl(true);
    }

    private void hideControl(boolean hideInfo) {
        if (hideInfo) hideInfo();
        mBinding.control.text.setText(R.string.play_track_text);
        mBinding.control.getRoot().setVisibility(View.GONE);
        App.removeCallbacks(mR1);
    }

    private void hideCenter() {
        mBinding.widget.action.setImageResource(R.drawable.ic_widget_play);
        mBinding.widget.center.setVisibility(View.GONE);
    }

    private void showPreview(Drawable preview) {
        if (Setting.getFlag() == 0 || isGone(mBinding.widget.preview)) return;
        mBinding.widget.preview.setVisibility(View.VISIBLE);
        mBinding.widget.preview.setImageDrawable(preview);
    }

    private void hidePreview() {
        mBinding.widget.preview.setVisibility(View.GONE);
        mBinding.widget.preview.setImageDrawable(null);
    }

    private void setTraffic() {
        Traffic.setSpeed(mBinding.widget.traffic);
        App.post(mR3, Constant.INTERVAL_TRAFFIC);
    }

    private void setR1Callback() {
        App.post(mR1, Constant.INTERVAL_HIDE);
    }

    private void setR2Callback() {
        App.post(mR2, 500);
    }

    private void setArtwork(String url) {
        ImgUtil.load(url, R.drawable.radio, new CustomTarget<>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                getExo().setDefaultArtwork(resource);
                getIjk().setDefaultArtwork(resource);
                showPreview(resource);
                setMetadata();
            }

            @Override
            public void onLoadFailed(@Nullable Drawable error) {
                getExo().setDefaultArtwork(error);
                getIjk().setDefaultArtwork(error);
                hideProgress();
                hidePreview();
                setMetadata();
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        });
    }

    private void getPart(String source) {
        OkHttp.newCall("http://api.pullword.com/get.php?source=" + URLEncoder.encode(source.trim()) + "&param1=0&param2=0&json=1").enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<String> items = Part.get(response.body().string());
                if (!items.contains(source)) items.add(0, source);
                App.post(() -> setPartAdapter(items));
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                List<String> items = List.of(source);
                App.post(() -> setPartAdapter(items));
            }
        });
    }

    private void setPartAdapter(List<String> items) {
        mBinding.part.setVisibility(View.VISIBLE);
        mPartAdapter.setItems(items, null);
        setR2Callback();
    }

    private void checkFlag(Vod item) {
        boolean empty = item.getVodFlags().isEmpty();
        mBinding.flag.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            ErrorEvent.flag();
        } else {
            setFlagActivated(mHistory.getFlag());
            if (mHistory.isRevSort()) reverseEpisode(true);
        }
    }

    private void checkHistory(Vod item) {
        mHistory = History.find(getHistoryKey());
        mHistory = mHistory == null ? createHistory(item) : mHistory;
        if (!TextUtils.isEmpty(getMark())) mHistory.setVodRemarks(getMark());
        if (!getSite().isRecordable() && mHistory.getKey().equals(getHistoryKey())) mHistory.delete();
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
        history.setCid(VodConfig.getCid());
        history.setVodPic(item.getVodPic());
        history.setVodName(item.getVodName());
        history.findEpisode(item.getVodFlags());
        history.setSpeed(Setting.getPlaySpeed());
        return history;
    }

    private void updateHistory(Episode item, boolean replay) {
        replay = replay || !item.equals(mHistory.getEpisode());
        long position = replay ? 0 : mHistory.getPosition();
        mHistory.setPosition(position);
        mHistory.setEpisodeUrl(item.getUrl());
        mHistory.setVodRemarks(item.getName());
        mHistory.setVodFlag(getFlag().getFlag());
        mHistory.setCreateTime(System.currentTimeMillis());
        mPlayers.setPosition(Math.max(mHistory.getOpening(), mHistory.getPosition()));
    }

    private void checkKeep() {
        mBinding.keep.setCompoundDrawablesWithIntrinsicBounds(Keep.find(getHistoryKey()) == null ? R.drawable.ic_detail_keep_off : R.drawable.ic_detail_keep_on, 0, 0, 0);
    }

    private void createKeep() {
        Keep keep = new Keep();
        keep.setKey(getHistoryKey());
        keep.setCid(VodConfig.getCid());
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
        onTimeChangeDisplaySpeed();
        long position, duration;
        mHistory.setPosition(position = mPlayers.getPosition());
        mHistory.setDuration(duration = mPlayers.getDuration());
        if (position >= 0 && duration > 0 && getSite().isRecordable()) App.execute(() -> mHistory.update());
        if (mHistory.getEnding() > 0 && duration > 0 && mHistory.getEnding() + position >= duration) {
            mClock.setCallback(null);
            checkNext();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onActionEvent(ActionEvent event) {
        if (isBackground()) return;
        if (ActionEvent.PLAY.equals(event.getAction()) || ActionEvent.PAUSE.equals(event.getAction())) {
            onKeyCenter();
        } else if (ActionEvent.NEXT.equals(event.getAction())) {
            mBinding.control.next.performClick();
        } else if (ActionEvent.PREV.equals(event.getAction())) {
            mBinding.control.prev.performClick();
        } else if (ActionEvent.STOP.equals(event.getAction())) {
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (isBackground()) return;
        if (event.getType() == RefreshEvent.Type.DETAIL) getDetail();
        else if (event.getType() == RefreshEvent.Type.PLAYER) onRefresh();
        else if (event.getType() == RefreshEvent.Type.DANMAKU) checkDanmu(event.getPath());
        else if (event.getType() == RefreshEvent.Type.SUBTITLE) mPlayers.setSub(Sub.from(event.getPath()));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerEvent(PlayerEvent event) {
        if (isBackground()) return;
        switch (event.getState()) {
            case 0:
                setInitTrack(true);
                setTrackVisible(false);
                mClock.setCallback(this);
                mBinding.control.seek.start();
                break;
            case Player.STATE_IDLE:
                break;
            case Player.STATE_BUFFERING:
                showProgress();
                break;
            case Player.STATE_READY:
                stopSearch();
                setMetadata();
                resetToggle();
                hideProgress();
                mPlayers.reset();
                setDefaultTrack();
                setTrackVisible(true);
                mHistory.setPlayer(mPlayers.getPlayer());
                mBinding.widget.size.setText(mPlayers.getSizeText());
                break;
            case Player.STATE_ENDED:
                checkEnded();
                break;
        }
    }

    private void checkEnded() {
        if (mBinding.control.loop.isActivated()) {
            onReset(true);
        } else {
            checkNext();
        }
    }

    private void setTrackVisible(boolean visible) {
        mBinding.control.text.setVisibility(visible && (mPlayers.haveTrack(C.TRACK_TYPE_TEXT) || mPlayers.isExo()) ? View.VISIBLE : View.GONE);
        mBinding.control.audio.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_AUDIO) ? View.VISIBLE : View.GONE);
        mBinding.control.video.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_VIDEO) ? View.VISIBLE : View.GONE);
    }

    private void setDefaultTrack() {
        if (isInitTrack()) {
            setInitTrack(false);
            mPlayers.prepared();
            mPlayers.setTrack(Track.find(getHistoryKey()));
        }
    }

    private void setMetadata() {
        String title = mHistory == null ? getName() : mHistory.getVodName();
        String artist = mEpisodeAdapter.size() == 0 ? "" : getEpisode().getName();
        artist = title.equals(artist) ? "" : getString(R.string.play_now, artist);
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
        builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title);
        builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist);
        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, getIjk().getDefaultArtwork());
        builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mPlayers.getDuration());
        mPlayers.setMetadata(builder.build());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onErrorEvent(ErrorEvent event) {
        if (isBackground()) return;
        if (mPlayers.addRetry() > event.getRetry()) checkError(event);
        else onRefresh();
    }

    private void checkError(ErrorEvent event) {
        if (getSite().getPlayerType() == -1 && event.isUrl() && event.getRetry() > 0 && getToggleCount() < 2 && mPlayers.getPlayer() != Players.SYS) {
            toggleCount++;
            nextPlayer();
        } else {
            resetToggle();
            onError(event);
        }
    }

    private void nextPlayer() {
        mPlayers.nextPlayer();
        setPlayerView();
        onRefresh();
    }

    private void onError(ErrorEvent event) {
        showError(event.getMsg());
        mClock.setCallback(null);
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
        VodConfig.get().setParse((Parse) mParseAdapter.get(0));
        notifyItemChanged(mBinding.control.parse, mParseAdapter);
    }

    private void checkFlag() {
        int position = isGone(mBinding.flag) ? -1 : mBinding.flag.getSelectedPosition();
        if (position == mFlagAdapter.size() - 1) checkSearch(false);
        else nextFlag(position);
    }

    private void checkSearch(boolean force) {
        if (mQuickAdapter.size() == 0) initSearch(mBinding.name.getText().toString(), true);
        else if (isAutoMode() || force) nextSite();
    }

    private void initSearch(String keyword, boolean auto) {
        stopSearch();
        setAutoMode(auto);
        setInitAuto(auto);
        startSearch(keyword);
        mBinding.part.setTag(keyword);
    }

    private boolean isPass(Site item) {
        if (isAutoMode() && !item.isChangeable()) return false;
        return item.isSearchable();
    }

    private void startSearch(String keyword) {
        mQuickAdapter.clear();
        List<Site> sites = new ArrayList<>();
        mExecutor = Executors.newFixedThreadPool(Constant.THREAD_POOL);
        for (Site site : VodConfig.get().getSites()) if (isPass(site)) sites.add(site);
        for (Site site : sites) mExecutor.execute(() -> search(site, keyword));
    }

    private void stopSearch() {
        if (mExecutor == null) return;
        mExecutor.shutdownNow();
        mExecutor = null;
    }

    private void search(Site site, String keyword) {
        try {
            mViewModel.searchContent(site, keyword, true);
        } catch (Throwable ignored) {
        }
    }

    private void setSearch(List<Vod> items) {
        Iterator<Vod> iterator = items.iterator();
        while (iterator.hasNext()) if (mismatch(iterator.next())) iterator.remove();
        mQuickAdapter.addAll(mQuickAdapter.size(), items);
        mBinding.quick.setVisibility(View.VISIBLE);
        if (isInitAuto()) nextSite();
        if (items.isEmpty()) return;
        App.removeCallbacks(mR4);
    }

    private void setSearch(Vod item) {
        setAutoMode(false);
        getDetail(item);
    }

    private boolean mismatch(Vod item) {
        if (getId().equals(item.getVodId())) return true;
        if (mBroken.contains(item.getVodId())) return true;
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
        Flag flag = (Flag) mFlagAdapter.get(position + 1);
        Notify.show(getString(R.string.play_switch_flag, flag.getFlag()));
        setFlagActivated(flag);
    }

    private void nextSite() {
        if (mQuickAdapter.size() == 0) return;
        Vod item = (Vod) mQuickAdapter.get(0);
        Notify.show(getString(R.string.play_switch_site, item.getSiteName()));
        mQuickAdapter.removeItems(0, 1);
        mBroken.add(getId());
        setInitAuto(false);
        getDetail(item);
    }

    private void onPaused(boolean visible) {
        mBinding.widget.exoDuration.setText(mPlayers.getDurationTime());
        mBinding.widget.exoPosition.setText(mPlayers.getPositionTime(0));
        if (visible) showInfoAndCenter();
        else hideInfoAndCenter();
        mPlayers.pause();
    }

    private void onPlay() {
        mPlayers.play();
        hideCenter();
    }

    public boolean isBackground() {
        return background;
    }

    public void setBackground(boolean background) {
        this.background = background;
    }

    public boolean isFullscreen() {
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

    public int getCurrentFlag() {
        return currentFlag;
    }

    public void setCurrentFlag(int currentFlag) {
        this.currentFlag = currentFlag;
    }

    public int getToggleCount() {
        return toggleCount;
    }

    public void resetToggle() {
        this.toggleCount = 0;
    }

    public int getGroupSize() {
        return groupSize;
    }

    public void setGroupSize(int size) {
        groupSize = size;
    }

    private View getFocus1() {
        return mFocus1 == null ? mBinding.video : mFocus1;
    }

    private View getFocus2() {
        return mFocus2 == null || mFocus2 == mBinding.control.opening || mFocus2 == mBinding.control.ending ? mBinding.control.next : mFocus2;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isFullscreen() && KeyUtil.isMenuKey(event)) onToggle();
        if (isVisible(mBinding.control.getRoot())) setR1Callback();
        if (isVisible(mBinding.control.getRoot())) mFocus2 = getCurrentFocus();
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
    public void onSpeedUp() {
        if (!mPlayers.isPlaying()) return;
        mBinding.control.speed.setText(mPlayers.setSpeed(mPlayers.getSpeed() < 3 ? 3 : 5));
        mBinding.widget.speed.startAnimation(ResUtil.getAnim(R.anim.forward));
        mBinding.widget.speed.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSpeedEnd() {
        mBinding.control.speed.setText(mPlayers.setSpeed(mHistory.getSpeed()));
        mBinding.widget.speed.setVisibility(View.GONE);
        mBinding.widget.speed.clearAnimation();
    }

    @Override
    public void onKeyUp() {
        long current = mPlayers.getPosition();
        long half = mPlayers.getDuration() / 2;
        showInfo();
        showControl(current < half ? mBinding.control.opening : mBinding.control.ending);
    }

    @Override
    public void onKeyDown() {
        showControl(getFocus2());
    }

    @Override
    public void onKeyCenter() {
        if (mPlayers.isPlaying()) {
            onPaused(true);
            hideControl(false);
        } else {
            onPlay();
            hideControl(true);
        }
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
    public void setSubtitle(int size) {
        getExo().getSubtitleView().setFixedTextSize(Dimension.SP, size);
        getIjk().getSubtitleView().setFixedTextSize(Dimension.SP, size);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        switch (requestCode) {
            case 1000:
                setResult(RESULT_OK);
                finish();
                break;
            case 1001:
                mPlayers.checkData(data);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setBackground(false);
        mClock.start();
        onPlay();
    }

    @Override
    protected void onPause() {
        super.onPause();
        setBackground(true);
        onPaused(false);
        mClock.stop();
    }

    @Override
    public void onBackPressed() {
        if (isVisible(mBinding.control.getRoot())) {
            hideControl();
        } else if (isVisible(mBinding.widget.center)) {
            hideCenter();
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
        stopSearch();
        mClock.release();
        mPlayers.release();
        Source.get().stop();
        RefreshEvent.history();
        App.removeCallbacks(mR1, mR2, mR3, mR4);
    }
}
