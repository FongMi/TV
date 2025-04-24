package com.fongmi.android.tv.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.C;
import androidx.media3.common.Player;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Danmaku;
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
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.player.exo.ExoUtil;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.ui.adapter.QualityAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomKeyDownVod;
import com.fongmi.android.tv.ui.custom.CustomMovement;
import com.fongmi.android.tv.ui.dialog.DanmakuDialog;
import com.fongmi.android.tv.ui.dialog.DescDialog;
import com.fongmi.android.tv.ui.dialog.SubtitleDialog;
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
import com.github.bassaer.library.MDColor;
import com.github.catvod.utils.Trans;
import com.permissionx.guolindev.PermissionX;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

public class VideoActivity extends BaseActivity implements CustomKeyDownVod.Listener, TrackDialog.Listener, ArrayPresenter.OnClickListener, Clock.Callback {

    private ActivityVideoBinding mBinding;
    private ViewGroup.LayoutParams mFrameParams;
    private EpisodePresenter mEpisodePresenter;
    private ArrayObjectAdapter mEpisodeAdapter;
    private ArrayObjectAdapter mArrayAdapter;
    private ArrayObjectAdapter mParseAdapter;
    private ArrayObjectAdapter mQuickAdapter;
    private ArrayObjectAdapter mFlagAdapter;
    private ArrayObjectAdapter mPartAdapter;
    private Observer<Result> mObserveDetail;
    private Observer<Result> mObservePlayer;
    private Observer<Result> mObserveSearch;
    private QualityAdapter mQualityAdapter;
    private FlagPresenter mFlagPresenter;
    private PartPresenter mPartPresenter;
    private CustomKeyDownVod mKeyDown;
    private ExecutorService mExecutor;
    private SiteViewModel mViewModel;
    private List<String> mBroken;
    private History mHistory;
    private Players mPlayers;
    private boolean fullscreen;
    private boolean initAuto;
    private boolean autoMode;
    private boolean useParse;
    private boolean redirect;
    private Runnable mR1;
    private Runnable mR2;
    private Runnable mR3;
    private Runnable mR4;
    private Clock mClock;
    private View mFocus1;
    private View mFocus2;
    private String tag;

    public static void push(FragmentActivity activity, String text) {
        if (FileChooser.isValid(activity, Uri.parse(text))) file(activity, FileChooser.getPathFromUri(activity, Uri.parse(text)));
        else start(activity, Sniffer.getUrl(text));
    }

    public static void file(FragmentActivity activity, String path) {
        if (TextUtils.isEmpty(path)) return;
        String name = new File(path).getName();
        PermissionX.init(activity).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> start(activity, "push_agent", "file://" + path, name));
    }

    public static void cast(Activity activity, History history) {
        start(activity, history.getSiteKey(), history.getVodId(), history.getVodName(), history.getVodPic(), null, true, false);
    }

    public static void collect(Activity activity, String key, String id, String name, String pic) {
        start(activity, key, id, name, pic, null, false, true);
    }

    public static void start(Activity activity, String url) {
        start(activity, "push_agent", url, url);
    }

    public static void start(Activity activity, String key, String id, String name) {
        start(activity, key, id, name, null);
    }

    public static void start(Activity activity, String key, String id, String name, String pic) {
        start(activity, key, id, name, pic, null);
    }

    public static void start(Activity activity, String key, String id, String name, String pic, String mark) {
        start(activity, key, id, name, pic, mark, false, false);
    }

    public static void start(Activity activity, String key, String id, String name, String pic, String mark, boolean cast, boolean collect) {
        Intent intent = new Intent(activity, VideoActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("collect", collect);
        intent.putExtra("cast", cast);
        intent.putExtra("mark", mark);
        intent.putExtra("name", name);
        intent.putExtra("pic", pic);
        intent.putExtra("key", key);
        intent.putExtra("id", id);
        activity.startActivity(intent);
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
        return (Flag) mFlagAdapter.get(getFlagPosition());
    }

    private Episode getEpisode() {
        return (Episode) mEpisodeAdapter.get(getEpisodePosition());
    }

    private int getFlagPosition() {
        for (int i = 0; i < mFlagAdapter.size(); i++) if (((Flag) mFlagAdapter.get(i)).isActivated()) return i;
        return 0;
    }

    private int getEpisodePosition() {
        for (int i = 0; i < mEpisodeAdapter.size(); i++) if (((Episode) mEpisodeAdapter.get(i)).isActivated()) return i;
        return 0;
    }

    private int getParsePosition() {
        for (int i = 0; i < mParseAdapter.size(); i++) if (((Parse) mParseAdapter.get(i)).isActivated()) return i;
        return 0;
    }

    private int getScale() {
        return mHistory != null && mHistory.getScale() != -1 ? mHistory.getScale() : Setting.getScale();
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String id = Objects.toString(intent.getStringExtra("id"), "");
        if (TextUtils.isEmpty(id) || id.equals(getId())) return;
        getIntent().putExtras(intent);
        stopSearch();
        checkId();
    }

    @Override
    protected void initView() {
        mFrameParams = mBinding.video.getLayoutParams();
        mClock = Clock.create(mBinding.widget.clock);
        mKeyDown = CustomKeyDownVod.create(this);
        mObserveDetail = this::setDetail;
        mObservePlayer = this::setPlayer;
        mObserveSearch = this::setSearch;
        mPlayers = Players.create(this);
        mBroken = new ArrayList<>();
        mR1 = this::hideControl;
        mR2 = this::updateFocus;
        mR3 = this::setTraffic;
        mR4 = this::showEmpty;
        setRecyclerView();
        setVideoView();
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
        mBinding.control.speed.setUpListener(this::onSpeedAdd);
        mBinding.control.speed.setDownListener(this::onSpeedSub);
        mBinding.control.ending.setUpListener(this::onEndingAdd);
        mBinding.control.ending.setDownListener(this::onEndingSub);
        mBinding.control.opening.setUpListener(this::onOpeningAdd);
        mBinding.control.opening.setDownListener(this::onOpeningSub);
        mBinding.control.text.setUpListener(this::onSubtitleClick);
        mBinding.control.text.setDownListener(this::onSubtitleClick);
        mBinding.control.loop.setOnClickListener(view -> onLoop());
        mBinding.control.next.setOnClickListener(view -> checkNext());
        mBinding.control.prev.setOnClickListener(view -> checkPrev());
        mBinding.control.scale.setOnClickListener(view -> onScale());
        mBinding.control.speed.setOnClickListener(view -> onSpeed());
        mBinding.control.reset.setOnClickListener(view -> onReset());
        mBinding.control.player.setOnClickListener(view -> onChoose());
        mBinding.control.decode.setOnClickListener(view -> onDecode());
        mBinding.control.ending.setOnClickListener(view -> onEnding());
        mBinding.control.change2.setOnClickListener(view -> onChange());
        mBinding.control.danmaku.setOnClickListener(view -> onDanmaku());
        mBinding.control.opening.setOnClickListener(view -> onOpening());
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
        mBinding.episode.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (child != null && mBinding.video != mFocus1) mFocus1 = child.itemView;
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
        mBinding.flag.setAdapter(new ItemBridgeAdapter(mFlagAdapter = new ArrayObjectAdapter(mFlagPresenter = new FlagPresenter(this::setFlagActivated))));
        mBinding.episode.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.episode.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.episode.setAdapter(new ItemBridgeAdapter(mEpisodeAdapter = new ArrayObjectAdapter(mEpisodePresenter = new EpisodePresenter(this::setEpisodeActivated))));
        mBinding.quality.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.quality.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.quality.setAdapter(mQualityAdapter = new QualityAdapter(this::setQualityActivated));
        mBinding.array.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.array.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.array.setAdapter(new ItemBridgeAdapter(mArrayAdapter = new ArrayObjectAdapter(new ArrayPresenter(this))));
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

    private void setVideoView() {
        mPlayers.init(mBinding.exo);
        PlaybackService.start(mPlayers);
        ExoUtil.setSubtitleView(mBinding.exo);
        mPlayers.setDanmakuView(mBinding.danmaku);
        mPlayers.setTag(tag = UUID.randomUUID().toString());
        mBinding.control.decode.setText(mPlayers.getDecodeText());
        mBinding.control.danmaku.setVisibility(Setting.isDanmakuLoad() ? View.VISIBLE : View.GONE);
        mBinding.control.reset.setText(ResUtil.getStringArray(R.array.select_reset)[Setting.getReset()]);
    }

    private void setDecode() {
        mBinding.control.decode.setText(mPlayers.getDecodeText());
    }

    private void setScale(int scale) {
        mBinding.exo.setResizeMode(scale);
        mBinding.control.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observeForever(mObserveDetail);
        mViewModel.player.observeForever(mObservePlayer);
        mViewModel.search.observeForever(mObserveSearch);
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

    private void getDetail() {
        mViewModel.detailContent(getKey(), getId());
    }

    private void getDetail(Vod item) {
        getIntent().putExtra("key", item.getSiteKey());
        getIntent().putExtra("pic", item.getVodPic());
        getIntent().putExtra("id", item.getVodId());
        mBinding.scroll.scrollTo(0, 0);
        mClock.setCallback(null);
        mPlayers.reset();
        mPlayers.stop();
        getDetail();
    }

    private void setDetail(Result result) {
        if (result.getList().isEmpty()) setEmpty(result.hasMsg());
        else setDetail(result.getList().get(0));
        Notify.show(result.getMsg());
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
        setPartAdapter(Part.get(item.getVodName()));
        mBinding.content.setMaxLines(getMaxLines());
        mBinding.video.requestFocus();
        setArtwork(item.getVodPic());
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
                setRedirect(true);
            }
        };
    }

    private void getPlayer(Flag flag, Episode episode, boolean replay) {
        mBinding.widget.title.setText(getString(R.string.detail_title, mBinding.name.getText(), episode.getName()));
        mViewModel.playerContent(getKey(), flag.getFlag(), episode.getUrl());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mBinding.widget.title.setSelected(true);
        updateHistory(episode, replay);
        showProgress();
        setMetadata();
        hideCenter();
    }

    private void setPlayer(Result result) {
        result.getUrl().set(mQualityAdapter.getPosition());
        if (!result.getDesc().isEmpty()) setText(mBinding.content, R.string.detail_content, Html.fromHtml(result.getDesc()).toString());
        setUseParse(VodConfig.hasParse() && ((result.getPlayUrl().isEmpty() && VodConfig.get().getFlags().contains(result.getFlag())) || result.getJx() == 1));
        mPlayers.start(result, isUseParse(), getSite().isChangeable() ? getSite().getTimeout() : -1);
        mBinding.control.parse.setVisibility(isUseParse() ? View.VISIBLE : View.GONE);
        setQualityVisible(result.getUrl().isMulti());
        mQualityAdapter.addAll(result);
    }

    private void setFlagActivated(Flag item) {
        if (mFlagAdapter.size() == 0 || item.isActivated()) return;
        if (mFlagAdapter.indexOf(item) == -1) item.setFlag(((Flag) mFlagAdapter.get(0)).getFlag());
        for (int i = 0; i < mFlagAdapter.size(); i++) ((Flag) mFlagAdapter.get(i)).setActivated(item);
        mBinding.flag.setSelectedPosition(mFlagAdapter.indexOf(item));
        notifyItemChanged(mBinding.flag, mFlagAdapter);
        setEpisodeAdapter(item.getEpisodes());
        setQualityVisible(false);
        seamless(item);
    }

    private void setEpisodeAdapter(List<Episode> items) {
        mBinding.episode.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        mEpisodeAdapter.setItems(items, null);
        setArrayAdapter(items.size());
        setR2Callback();
    }

    private void seamless(Flag flag) {
        Episode episode = flag.find(mHistory.getVodRemarks(), getMark().isEmpty());
        setQualityVisible(episode != null && episode.isActivated() && mQualityAdapter.getItemCount() > 1);
        if (episode == null || episode.isActivated()) return;
        mHistory.setVodRemarks(episode.getName());
        setEpisodeActivated(episode);
    }

    private void setEpisodeActivated(Episode item) {
        int flagPosition = getFlagPosition();
        if (shouldEnterFullscreen(item)) return;
        if (isFullscreen()) Notify.show(getString(R.string.play_ready, item.getName()));
        for (int i = 0; i < mFlagAdapter.size(); i++) ((Flag) mFlagAdapter.get(i)).toggle(flagPosition == i, item);
        mBinding.episode.setSelectedPosition(getEpisodePosition());
        notifyItemChanged(mBinding.episode, mEpisodeAdapter);
        onRefresh();
    }

    private void setQualityVisible(boolean visible) {
        mBinding.quality.setVisibility(visible ? View.VISIBLE : View.GONE);
        setR2Callback();
    }

    private void setQualityActivated(Result result) {
        try {
            mPlayers.start(result, isUseParse(), getSite().isChangeable() ? getSite().getTimeout() : -1);
        } catch (Exception e) {
            ErrorEvent.extract(tag, e.getMessage());
            e.printStackTrace();
        }
    }

    private void reverseEpisode(boolean scroll) {
        for (int i = 0; i < mFlagAdapter.size(); i++) Collections.reverse(((Flag) mFlagAdapter.get(i)).getEpisodes());
        setEpisodeAdapter(getFlag().getEpisodes());
        if (scroll) mBinding.episode.setSelectedPosition(getEpisodePosition());
    }

    private void setParseActivated(Parse item) {
        VodConfig.get().setParse(item);
        notifyItemChanged(mBinding.control.parse, mParseAdapter);
        onRefresh();
    }

    private void setArrayAdapter(int size) {
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.play_reverse));
        items.add(getString(mHistory.getRevPlayText()));
        mBinding.array.setVisibility(size > 1 ? View.VISIBLE : View.GONE);
        if (mHistory.isRevSort()) for (int i = size; i > 0; i -= 20) items.add(i + "-" + Math.max(i - 19, 1));
        else for (int i = 0; i < size; i += 20) items.add((i + 1) + "-" + Math.min(i + 20, size));
        mArrayAdapter.setItems(items, null);
    }

    private int findFocusDown(int index) {
        List<Integer> orders = Arrays.asList(R.id.flag, R.id.quality, R.id.episode, R.id.array, R.id.part, R.id.quick);
        for (int i = 0; i < orders.size(); i++) if (i > index) if (isVisible(findViewById(orders.get(i)))) return orders.get(i);
        return 0;
    }

    private int findFocusUp(int index) {
        List<Integer> orders = Arrays.asList(R.id.flag, R.id.quality, R.id.episode, R.id.array, R.id.part, R.id.quick);
        for (int i = orders.size() - 1; i >= 0; i--) if (i < index) if (isVisible(findViewById(orders.get(i)))) return orders.get(i);
        return 0;
    }

    private void updateFocus() {
        mPartPresenter.setNextFocusUp(findFocusUp(4));
        mEpisodePresenter.setNextFocusUp(findFocusUp(2));
        mFlagPresenter.setNextFocusDown(findFocusDown(0));
        mEpisodePresenter.setNextFocusDown(findFocusDown(2));
        notifyItemChanged(mBinding.episode, mEpisodeAdapter);
        notifyItemChanged(mBinding.part, mPartAdapter);
        notifyItemChanged(mBinding.flag, mFlagAdapter);
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
        mBinding.flag.setSelectedPosition(getFlagPosition());
        mPlayers.setDanmakuSize(1.2f);
        mKeyDown.setFull(true);
        setFullscreen(true);
        mFocus2 = null;
    }

    private void exitFullscreen() {
        mBinding.video.setForeground(ResUtil.getDrawable(R.drawable.selector_video));
        mBinding.video.setLayoutParams(mFrameParams);
        mPlayers.setDanmakuSize(0.8f);
        getFocus1().requestFocus();
        mKeyDown.setFull(false);
        setFullscreen(false);
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

    private void checkNext() {
        checkNext(true);
    }

    private void checkNext(boolean notify) {
        if (mHistory.isRevPlay()) onPrev(notify);
        else onNext(notify);
    }

    private void checkPrev() {
        if (mHistory.isRevPlay()) onNext(true);
        else onPrev(true);
    }

    private void onNext(boolean notify) {
        int current = getEpisodePosition();
        int max = mEpisodeAdapter.size() - 1;
        current = ++current > max ? max : current;
        Episode item = (Episode) mEpisodeAdapter.get(current);
        if (!item.isActivated()) setEpisodeActivated(item);
        else if (notify) Notify.show(mHistory.isRevPlay() ? R.string.error_play_prev : R.string.error_play_next);
    }

    private void onPrev(boolean notify) {
        int current = getEpisodePosition();
        current = --current < 0 ? 0 : current;
        Episode item = (Episode) mEpisodeAdapter.get(current);
        if (!item.isActivated()) setEpisodeActivated(item);
        else if (notify) Notify.show(mHistory.isRevPlay() ? R.string.error_play_next : R.string.error_play_prev);
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
        mPlayers.stop();
        mPlayers.clear();
        mClock.setCallback(null);
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
        if (current > TimeUnit.MINUTES.toMillis(10)) return;
        if (current < 0 || duration < 0 || current > duration / 2) return;
        setOpening(current);
    }

    private void onOpeningAdd() {
        long current = mPlayers.getPosition();
        long duration = mPlayers.getDuration();
        if (current < 0 || duration < 0) return;
        if (mHistory.getOpening() < 0) mHistory.setOpening(0);
        setOpening(Math.min(mHistory.getOpening() + 1000, duration / 2));
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
        mBinding.control.opening.setText(opening <= 0 ? getString(R.string.play_op) : mPlayers.stringToTime(mHistory.getOpening()));
    }

    private void onEnding() {
        long current = mPlayers.getPosition();
        long duration = mPlayers.getDuration();
        if (duration - current > TimeUnit.MINUTES.toMillis(10)) return;
        if (current < 0 || duration < 0 || current < duration / 2) return;
        setEnding(duration - current);
    }

    private void onEndingAdd() {
        long current = mPlayers.getPosition();
        long duration = mPlayers.getDuration();
        if (current < 0 || duration < 0) return;
        setEnding(Math.min(duration / 2, mHistory.getEnding() + 1000));
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
        mBinding.control.ending.setText(ending <= 0 ? getString(R.string.play_ed) : mPlayers.stringToTime(mHistory.getEnding()));
    }

    private void onChoose() {
        mPlayers.choose(this, mBinding.widget.title.getText());
        setRedirect(true);
    }

    private void onDecode() {
        mPlayers.toggleDecode();
        setDecode();
    }

    private void onTrack(View view) {
        TrackDialog.create().player(mPlayers).type(Integer.parseInt(view.getTag().toString())).show(this);
        hideControl();
    }

    private void onDanmaku() {
        DanmakuDialog.create().player(mPlayers).show(this);
        hideControl();
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
        mBinding.widget.center.setVisibility(View.VISIBLE);
        mBinding.widget.exoDuration.setText(mPlayers.getDurationTime());
        mBinding.widget.exoPosition.setText(mPlayers.getPositionTime(0));
    }

    private void hideInfo() {
        mBinding.widget.info.setVisibility(View.GONE);
        mBinding.widget.center.setVisibility(View.GONE);
    }

    private void showControl(View view) {
        mBinding.control.getRoot().setVisibility(View.VISIBLE);
        view.requestFocus();
        setR1Callback();
    }

    private void hideControl() {
        mBinding.control.text.setText(R.string.play_track_text);
        mBinding.control.getRoot().setVisibility(View.GONE);
        App.removeCallbacks(mR1);
    }

    private void hideCenter() {
        mBinding.widget.action.setImageResource(R.drawable.ic_widget_play);
        hideInfo();
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
        ImgUtil.load(url, R.drawable.radio, new CustomTarget<>(ResUtil.getScreenWidth(), ResUtil.getScreenHeight()) {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                mBinding.exo.setDefaultArtwork(resource);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable error) {
                mBinding.exo.setDefaultArtwork(error);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
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
            ErrorEvent.flag(tag);
        } else {
            setFlagActivated(mHistory.getFlag());
            if (mHistory.isRevSort()) reverseEpisode(true);
        }
    }

    private void checkHistory(Vod item) {
        mHistory = History.find(getHistoryKey());
        mHistory = mHistory == null ? createHistory(item) : mHistory;
        if (!TextUtils.isEmpty(getMark())) mHistory.setVodRemarks(getMark());
        if (Setting.isIncognito() && mHistory.getKey().equals(getHistoryKey())) mHistory.delete();
        mBinding.control.opening.setText(mHistory.getOpening() <= 0 ? getString(R.string.play_op) : mPlayers.stringToTime(mHistory.getOpening()));
        mBinding.control.ending.setText(mHistory.getEnding() <= 0 ? getString(R.string.play_ed) : mPlayers.stringToTime(mHistory.getEnding()));
        mBinding.control.speed.setText(mPlayers.setSpeed(mHistory.getSpeed()));
        mHistory.setVodPic(item.getVodPic());
        setScale(getScale());
    }

    private History createHistory(Vod item) {
        History history = new History();
        history.setKey(getHistoryKey());
        history.setCid(VodConfig.getCid());
        history.setVodName(item.getVodName());
        history.findEpisode(item.getVodFlags());
        return history;
    }

    private void updateHistory(Episode item, boolean replay) {
        replay = replay || !item.equals(mHistory.getEpisode());
        mHistory.setEpisodeUrl(item.getUrl());
        mHistory.setVodRemarks(item.getName());
        mHistory.setVodFlag(getFlag().getFlag());
        mHistory.setCreateTime(System.currentTimeMillis());
        mHistory.setPosition(replay ? C.TIME_UNSET : mHistory.getPosition());
    }

    private void checkPlayImg() {
        ActionEvent.update();
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
    public void onSubtitleClick() {
        App.post(this::hideControl, 200);
        App.post(() -> SubtitleDialog.create().view(mBinding.exo.getSubtitleView()).full(isFullscreen()).show(this), 200);
    }

    @Override
    public void onTimeChanged() {
        long position, duration;
        mHistory.setPosition(position = mPlayers.getPosition());
        mHistory.setDuration(duration = mPlayers.getDuration());
        if (position >= 0 && duration > 0 && !Setting.isIncognito()) App.execute(() -> mHistory.update());
        if (mHistory.getEnding() > 0 && duration > 0 && mHistory.getEnding() + position >= duration) {
            checkEnded(false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onActionEvent(ActionEvent event) {
        if (isRedirect()) return;
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
        if (isRedirect()) return;
        if (event.getType() == RefreshEvent.Type.DETAIL) getDetail();
        else if (event.getType() == RefreshEvent.Type.PLAYER) onRefresh();
        else if (event.getType() == RefreshEvent.Type.SUBTITLE) mPlayers.setSub(Sub.from(event.getPath()));
        else if (event.getType() == RefreshEvent.Type.DANMAKU) mPlayers.setDanmaku(Danmaku.from(event.getPath()));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerEvent(PlayerEvent event) {
        if (!event.getTag().equals(tag)) return;
        switch (event.getState()) {
            case PlayerEvent.PREPARE:
                setDecode();
                setPosition();
                break;
            case Player.STATE_BUFFERING:
                showProgress();
                break;
            case Player.STATE_READY:
                hideProgress();
                checkPlayImg();
                mPlayers.reset();
                break;
            case Player.STATE_ENDED:
                checkEnded(true);
                break;
            case PlayerEvent.TRACK:
                setMetadata();
                setTrackVisible();
                mClock.setCallback(this);
                break;
            case PlayerEvent.SIZE:
                mBinding.widget.size.setText(mPlayers.getSizeText());
                break;
        }
    }

    private void setPosition() {
        if (mHistory != null) mPlayers.seekTo(Math.max(mHistory.getOpening(), mHistory.getPosition()));
    }

    private void checkEnded(boolean notify) {
        if (mBinding.control.loop.isActivated()) {
            onReset(true);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            checkNext(notify);
            checkPlayImg();
        }
    }

    private void setTrackVisible() {
        mBinding.control.text.setVisibility(mPlayers.haveTrack(C.TRACK_TYPE_TEXT) || mPlayers.isVod() ? View.VISIBLE : View.GONE);
        mBinding.control.audio.setVisibility(mPlayers.haveTrack(C.TRACK_TYPE_AUDIO) ? View.VISIBLE : View.GONE);
        mBinding.control.video.setVisibility(mPlayers.haveTrack(C.TRACK_TYPE_VIDEO) ? View.VISIBLE : View.GONE);
    }

    private void setMetadata() {
        String title = mHistory.getVodName();
        String episode = getEpisode().getName();
        String artist = title.equals(episode) ? "" : getString(R.string.play_now, episode);
        mPlayers.setMetadata(title, artist, mHistory.getVodPic(), mBinding.exo.getDefaultArtwork());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onErrorEvent(ErrorEvent event) {
        if (!event.getTag().equals(tag)) return;
        if (mPlayers.retried()) onError(event);
        else onRefresh();
    }

    private void onError(ErrorEvent event) {
        Track.delete(mPlayers.getUrl());
        showError(event.getMsg());
        mClock.setCallback(null);
        mPlayers.resetTrack();
        mPlayers.reset();
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
        int position = isGone(mBinding.flag) ? -1 : getFlagPosition();
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
        mExecutor = Executors.newFixedThreadPool(10);
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

    private void setSearch(Result result) {
        List<Vod> items = result.getList();
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
        String keyword = Objects.toString(mBinding.part.getTag(), "");
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

    private void onPaused() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (isFullscreen()) showInfo();
        else hideInfo();
        mPlayers.pause();
        checkPlayImg();
    }

    private void onPlay() {
        if (mHistory != null && mPlayers.isEnded()) mPlayers.seekTo(mHistory.getOpening());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (!mPlayers.isEmpty() && mPlayers.isIdle()) mPlayers.prepare();
        mPlayers.play();
        checkPlayImg();
        hideCenter();
    }

    private boolean isFullscreen() {
        return fullscreen;
    }

    private void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
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

    public boolean isRedirect() {
        return redirect;
    }

    public void setRedirect(boolean redirect) {
        this.redirect = redirect;
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
    public void onSeeking(int time) {
        mBinding.widget.center.setVisibility(View.VISIBLE);
        mBinding.widget.exoDuration.setText(mPlayers.getDurationTime());
        mBinding.widget.exoPosition.setText(mPlayers.getPositionTime(time));
        mBinding.widget.action.setImageResource(time > 0 ? R.drawable.ic_widget_forward : R.drawable.ic_widget_rewind);
        hideProgress();
    }

    @Override
    public void onSeekTo(int time) {
        mPlayers.seekTo(time);
        mKeyDown.resetTime();
        showProgress();
        onPlay();
    }

    @Override
    public void onSpeedUp() {
        if (!mPlayers.isPlaying()) return;
        mBinding.control.speed.setText(mPlayers.setSpeed(Setting.getSpeed()));
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
        showControl(current < half ? mBinding.control.opening : mBinding.control.ending);
    }

    @Override
    public void onKeyDown() {
        showControl(getFocus2());
    }

    @Override
    public void onKeyCenter() {
        if (mPlayers.isPlaying()) onPaused();
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
        if (resultCode == RESULT_OK && requestCode == 1001) mPlayers.checkData(data);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mClock.stop().start();
        onPlay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRedirect()) onPlay();
        setRedirect(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRedirect()) onPaused();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Setting.isBackgroundOff()) onPaused();
        if (Setting.isBackgroundOff()) mClock.stop();
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
        RefreshEvent.history();
        PlaybackService.stop();
        App.removeCallbacks(mR1, mR2, mR3, mR4);
        mViewModel.result.removeObserver(mObserveDetail);
        mViewModel.player.removeObserver(mObservePlayer);
        mViewModel.search.removeObserver(mObserveSearch);
    }
}
