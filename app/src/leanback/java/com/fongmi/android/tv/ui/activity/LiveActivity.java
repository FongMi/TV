package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;

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
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.databinding.ActivityLiveBinding;
import com.fongmi.android.tv.event.ErrorEvent;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.impl.PassCallback;
import com.fongmi.android.tv.model.LiveViewModel;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.player.source.Force;
import com.fongmi.android.tv.player.source.TVBus;
import com.fongmi.android.tv.player.source.ZLive;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomKeyDownLive;
import com.fongmi.android.tv.ui.custom.CustomLiveListView;
import com.fongmi.android.tv.ui.custom.dialog.LiveDialog;
import com.fongmi.android.tv.ui.custom.dialog.PassDialog;
import com.fongmi.android.tv.ui.custom.dialog.TrackDialog;
import com.fongmi.android.tv.ui.presenter.ChannelPresenter;
import com.fongmi.android.tv.ui.presenter.GroupPresenter;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Traffic;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Response;
import tv.danmaku.ijk.media.player.ui.IjkVideoView;

public class LiveActivity extends BaseActivity implements GroupPresenter.OnClickListener, ChannelPresenter.OnClickListener, CustomKeyDownLive.Listener, CustomLiveListView.Callback, PassCallback, LiveCallback {

    private ActivityLiveBinding mBinding;
    private ArrayObjectAdapter mChannelAdapter;
    private ArrayObjectAdapter mGroupAdapter;
    private SimpleDateFormat mFormatDate;
    private SimpleDateFormat mFormatTime;
    private CustomKeyDownLive mKeyDown;
    private LiveViewModel mViewModel;
    private List<Group> mHides;
    private Players mPlayers;
    private Channel mChannel;
    private View mOldView;
    private Group mGroup;
    private Runnable mR0;
    private Runnable mR1;
    private Runnable mR2;
    private Runnable mR3;
    private Runnable mR4;
    private int count;

    public static void start(Activity activity) {
        if (!LiveConfig.isEmpty()) activity.startActivity(new Intent(activity, LiveActivity.class));
    }

    private PlayerView getExo() {
        return Prefers.getRender() == 0 ? mBinding.surface : mBinding.texture;
    }

    private IjkVideoView getIjk() {
        return mBinding.ijk;
    }

    private Group getKeep() {
        return (Group) mGroupAdapter.get(0);
    }

    private Live getHome() {
        return LiveConfig.get().getHome();
    }

    private int getPlayerType() {
        return getHome().getPlayerType() != -1 ? getHome().getPlayerType() : Prefers.getLivePlayer();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityLiveBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mR0 = this::hideUI;
        mR1 = this::hideInfo;
        mR2 = this::hideControl;
        mR3 = this::setChannelActivated;
        mR4 = this::setTraffic;
        mPlayers = new Players().init();
        mKeyDown = CustomKeyDownLive.create(this);
        mFormatDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        mFormatTime = new SimpleDateFormat("yyyy-MM-ddHH:mm", Locale.getDefault());
        mHides = new ArrayList<>();
        setRecyclerView();
        setVideoView();
        setViewModel();
        getLive();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void initEvent() {
        mBinding.group.setListener(this);
        mBinding.channel.setListener(this);
        mBinding.control.seek.setListener(mPlayers);
        mBinding.control.text.setOnClickListener(this::onTrack);
        mBinding.control.audio.setOnClickListener(this::onTrack);
        mBinding.control.video.setOnClickListener(this::onTrack);
        mBinding.control.home.setOnClickListener(view -> onHome());
        mBinding.control.scale.setOnClickListener(view -> onScale());
        mBinding.control.speed.setOnClickListener(view -> onSpeed());
        mBinding.control.invert.setOnClickListener(view -> onInvert());
        mBinding.control.across.setOnClickListener(view -> onAcross());
        mBinding.control.player.setOnClickListener(view -> onPlayer());
        mBinding.control.decode.setOnClickListener(view -> onDecode());
        mBinding.control.line.setOnClickListener(view -> nextLine(false));
        mBinding.control.speed.setOnLongClickListener(view -> onSpeedLong());
        mBinding.video.setOnTouchListener((view, event) -> mKeyDown.onTouchEvent(event));
        mBinding.group.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (mGroupAdapter.size() > 0) onChildSelected(child, mGroup = (Group) mGroupAdapter.get(position));
            }
        });
    }

    private void setRecyclerView() {
        mBinding.group.setItemAnimator(null);
        mBinding.channel.setItemAnimator(null);
        mBinding.group.setAdapter(new ItemBridgeAdapter(mGroupAdapter = new ArrayObjectAdapter(new GroupPresenter(this))));
        mBinding.channel.setAdapter(new ItemBridgeAdapter(mChannelAdapter = new ArrayObjectAdapter(new ChannelPresenter(this))));
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
        setScale(Prefers.getLiveScale());
        getIjk().setRender(Prefers.getRender());
        mBinding.control.speed.setText(mPlayers.getSpeedText());
        mBinding.control.home.setVisibility(LiveConfig.isOnly() ? View.GONE : View.VISIBLE);
        mBinding.control.invert.setActivated(Prefers.isInvert());
        mBinding.control.across.setActivated(Prefers.isAcross());
    }

    private void setScale(int scale) {
        getExo().setResizeMode(scale);
        getIjk().setResizeMode(scale);
        mBinding.control.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(LiveViewModel.class);
        mViewModel.channel.observe(this, result -> mPlayers.start(result));
        mViewModel.live.observe(this, this::setGroup);
    }

    private void getLive() {
        mPlayers.setPlayer(getPlayerType());
        mViewModel.getLive(getHome());
        setPlayerView();
        setDecodeView();
        showProgress();
    }

    private void setGroup(Live home) {
        List<Group> items = new ArrayList<>();
        items.add(Group.create(R.string.keep));
        for (Group group : home.getGroups()) (group.isHidden() ? mHides : items).add(group);
        mGroupAdapter.setItems(items, null);
        setPosition(LiveConfig.get().find(items));
        mBinding.control.home.setText(home.getName());
        hideProgress();
    }

    private void setPosition(int[] position) {
        if (position[0] == -1) return;
        if (mGroupAdapter.size() == 1) return;
        mGroup = (Group) mGroupAdapter.get(position[0]);
        mBinding.group.setSelectedPosition(position[0]);
        if (mGroup.getChannel().isEmpty()) return;
        mGroup.setPosition(position[1]);
        onItemClick(mGroup);
        onItemClick(mGroup.current());
    }

    private void setPosition() {
        if (mChannel == null) return;
        Group group = mChannel.getGroup();
        int position = mGroupAdapter.indexOf(group);
        boolean change = mBinding.group.getSelectedPosition() != position;
        if (change) mBinding.group.setSelectedPosition(position);
        if (change) mChannelAdapter.setItems(group.getChannel(), null);
        mBinding.channel.setSelectedPosition(group.getPosition());
    }

    private void onChildSelected(@Nullable RecyclerView.ViewHolder child, Group group) {
        if (mOldView != null) mOldView.setSelected(false);
        if (child == null) return;
        mOldView = child.itemView;
        mOldView.setSelected(true);
        onItemClick(group);
        resetPass();
    }

    private void setChannelActivated() {
        for (int i = 0; i < mChannelAdapter.size(); i++) ((Channel) mChannelAdapter.get(i)).setSelected(mChannel);
        notifyItemChanged(mBinding.channel, mChannelAdapter);
        getUrl();
    }

    private void setTraffic() {
        Traffic.setSpeed(mBinding.widget.traffic);
        App.post(mR4, Constant.INTERVAL_TRAFFIC);
    }

    private void onToggle() {
        if (isVisible(mBinding.control.getRoot())) hideControl();
        else if (isVisible(mBinding.recycler)) hideUI();
        else showUI();
        hideInfo();
    }

    private void onHome() {
        LiveDialog.create(this).show();
        hideControl();
    }

    private void onScale() {
        int index = Prefers.getLiveScale();
        String[] array = ResUtil.getStringArray(R.array.select_scale);
        Prefers.putLiveScale(index = index == array.length - 1 ? 0 : ++index);
        setScale(index);
    }

    private void onSpeed() {
        mBinding.control.speed.setText(mPlayers.addSpeed());
    }

    private boolean onSpeedLong() {
        mBinding.control.speed.setText(mPlayers.toggleSpeed());
        return true;
    }

    private void onInvert() {
        Prefers.putInvert(!Prefers.isInvert());
        mBinding.control.invert.setActivated(Prefers.isInvert());
    }

    private void onAcross() {
        Prefers.putAcross(!Prefers.isAcross());
        mBinding.control.across.setActivated(Prefers.isAcross());
    }

    private void onPlayer() {
        mPlayers.togglePlayer();
        Prefers.putLivePlayer(mPlayers.getPlayer());
        setPlayerView();
        getUrl();
    }

    private void onDecode() {
        mPlayers.toggleDecode();
        mPlayers.set(getExo(), getIjk());
        setDecodeView();
        getUrl();
    }

    private void onTrack(View view) {
        int type = Integer.parseInt(view.getTag().toString());
        TrackDialog.create().player(mPlayers).type(type).show(getSupportFragmentManager(), null);
        hideControl();
    }

    private void hideUI() {
        App.removeCallbacks(mR0);
        if (isGone(mBinding.recycler)) return;
        mBinding.recycler.setVisibility(View.GONE);
        setPosition();
    }

    private void showUI() {
        if (isVisible(mBinding.recycler)) return;
        mBinding.recycler.setVisibility(View.VISIBLE);
        mBinding.channel.requestFocus();
        setPosition();
    }

    private void showProgress() {
        mBinding.widget.progress.setVisibility(View.VISIBLE);
        App.post(mR4, 0);
    }

    private void hideProgress() {
        mBinding.widget.progress.setVisibility(View.GONE);
        App.removeCallbacks(mR4);
        Traffic.reset();
    }

    private void showControl(View view) {
        mBinding.control.getRoot().setVisibility(View.VISIBLE);
        view.requestFocus();
        setR2Callback();
    }

    private void hideControl() {
        mBinding.control.getRoot().setVisibility(View.GONE);
        App.removeCallbacks(mR2);
    }

    private void showInfo() {
        mBinding.widget.info.setVisibility(View.VISIBLE);
        setR1Callback();
        setInfo();
    }

    private void hideInfo() {
        mBinding.widget.info.setVisibility(View.GONE);
        App.removeCallbacks(mR1);
    }

    private void showEpg() {
        mBinding.control.play.setText(mChannel.getData().getEpg());
        mBinding.widget.play.setText(mChannel.getData().getEpg());
    }

    private void hideCenter() {
        mBinding.widget.action.setImageResource(R.drawable.ic_widget_play);
        mBinding.widget.center.setVisibility(View.GONE);
    }

    private void setR1Callback() {
        App.post(mR1, Constant.INTERVAL_HIDE);
    }

    private void setR2Callback() {
        App.post(mR2, Constant.INTERVAL_HIDE);
    }

    private void resetPass() {
        this.count = 0;
    }

    @Override
    public void onItemClick(Group item) {
        mChannelAdapter.setItems(mGroup.getChannel(), null);
        mBinding.channel.setSelectedPosition(mGroup.getPosition());
        if (!item.isKeep() || ++count < 5 || mHides.isEmpty()) return;
        App.removeCallbacks(mR0);
        PassDialog.show(this);
        resetPass();
    }

    @Override
    public void onItemClick(Channel item) {
        mGroup.setPosition(mBinding.channel.getSelectedPosition());
        setChannel(item.group(mGroup));
        hideUI();
    }

    @Override
    public boolean onLongClick(Channel item) {
        if (mGroup.isHidden()) return false;
        boolean exist = Keep.exist(item.getName());
        Notify.show(exist ? R.string.keep_del : R.string.keep_add);
        if (exist) delKeep(item);
        else addKeep(item);
        return true;
    }

    private void addKeep(Channel item) {
        getKeep().add(item);
        Keep keep = new Keep();
        keep.setKey(item.getName());
        keep.setType(1);
        keep.save();
    }

    private void delKeep(Channel item) {
        if (mGroup.isKeep()) mChannelAdapter.remove(item);
        if (mChannelAdapter.size() == 0) mBinding.group.requestFocus();
        getKeep().getChannel().remove(item);
        Keep.delete(item.getName());
    }

    private void setChannel(Channel item) {
        LiveConfig.get().setKeep(mGroup, mChannel = item);
        App.post(mR3, 100);
        showInfo();
    }

    private void setInfo() {
        mChannel.loadLogo(mBinding.widget.logo);
        mBinding.control.name.setText(mChannel.getName());
        mBinding.control.line.setText(mChannel.getLineText());
        mBinding.control.number.setText(mChannel.getNumber());
        mBinding.widget.name.setText(mChannel.getName());
        mBinding.widget.line.setText(mChannel.getLineText());
        mBinding.widget.number.setText(mChannel.getNumber());
        mBinding.control.line.setVisibility(mChannel.getLineVisible());
        mBinding.widget.line.setVisibility(mChannel.getLineVisible());
        checkEpg();
    }

    private void checkEpg() {
        if (mChannel.getEpg().isEmpty()) return;
        String date = mFormatDate.format(new Date());
        String epg = mChannel.getEpg().replace("{date}", date);
        if (mChannel.getData().equal(date)) showEpg();
        else getEpg(epg, mChannel);
    }

    private void getEpg(String epg, Channel channel) {
        OkHttp.newCall(epg).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                channel.setData(Epg.objectFrom(response.body().string(), mFormatTime));
                if (mChannel.equals(channel)) App.post(() -> showEpg());
            }
        });
    }

    private void getUrl() {
        mViewModel.getUrl(mChannel);
        showProgress();
    }

    private void prevLine() {
        mChannel.prevLine();
        showInfo();
        getUrl();
    }

    private void nextLine(boolean show) {
        mChannel.nextLine();
        if (show) showInfo();
        else setInfo();
        getUrl();
    }

    private void notifyItemChanged(RecyclerView view, ArrayObjectAdapter adapter) {
        if (!view.isComputingLayout()) adapter.notifyArrayItemRangeChanged(0, adapter.size());
    }

    @Override
    public boolean nextGroup(boolean skip) {
        int position = mBinding.group.getSelectedPosition() + 1;
        if (position > mGroupAdapter.size() - 1) position = 0;
        mGroup = (Group) mGroupAdapter.get(position);
        mBinding.group.setSelectedPosition(position);
        if (skip && mGroup.skip()) return nextGroup(true);
        mChannelAdapter.setItems(mGroup.getChannel(), null);
        mGroup.setPosition(0);
        return true;
    }

    @Override
    public boolean prevGroup(boolean skip) {
        int position = mBinding.group.getSelectedPosition() - 1;
        if (position < 0) position = mGroupAdapter.size() - 1;
        mGroup = (Group) mGroupAdapter.get(position);
        mBinding.group.setSelectedPosition(position);
        if (skip && mGroup.skip()) return prevGroup(true);
        mChannelAdapter.setItems(mGroup.getChannel(), null);
        mGroup.setPosition(mGroup.getChannel().size() - 1);
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isVisible(mBinding.control.getRoot())) setR2Callback();
        if (mKeyDown.hasEvent(event)) mKeyDown.onKeyDown(event);
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void setUITimer() {
        App.post(mR0, Constant.INTERVAL_HIDE);
    }

    @Override
    public void onShow(String number) {
        mBinding.widget.digital.setText(number);
        mBinding.widget.digital.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFind(String number) {
        mBinding.widget.digital.setVisibility(View.GONE);
        setPosition(LiveConfig.get().find(number, mGroupAdapter.unmodifiableList()));
    }

    @Override
    public void onSeeking(int time) {
        if (!mPlayers.isVod() || !mChannel.isOnly()) return;
        mBinding.widget.exoDuration.setText(mPlayers.getDurationTime());
        mBinding.widget.exoPosition.setText(mPlayers.getPositionTime(time));
        mBinding.widget.action.setImageResource(time > 0 ? R.drawable.ic_widget_forward : R.drawable.ic_widget_rewind);
        mBinding.widget.center.setVisibility(View.VISIBLE);
        hideProgress();
    }

    @Override
    public boolean dispatch(boolean check) {
        boolean condition1 = mGroup != null && mChannel != null;
        boolean condition2 = isGone(mBinding.recycler) && isGone(mBinding.control.getRoot());
        return check ? condition1 && condition2 : condition1;
    }

    @Override
    public void onKeyUp() {
        int position = mGroup.getPosition() - 1;
        boolean limit = position < 0;
        if (Prefers.isAcross() & limit) prevGroup(true);
        else mGroup.setPosition(limit ? mChannelAdapter.size() - 1 : position);
        setChannel(mGroup.current());
    }

    @Override
    public void onKeyDown() {
        int position = mGroup.getPosition() + 1;
        boolean limit = position > mChannelAdapter.size() - 1;
        if (Prefers.isAcross() && limit) nextGroup(true);
        else mGroup.setPosition(limit ? 0 : position);
        setChannel(mGroup.current());
    }

    @Override
    public void onKeyLeft(int time) {
        if (mChannel.isOnly() && mPlayers.isVod()) App.post(() -> seekTo(time), 250);
        else if (!mChannel.isOnly()) prevLine();
        mKeyDown.resetTime();
    }

    @Override
    public void onKeyRight(int time) {
        if (mChannel.isOnly() && mPlayers.isVod()) App.post(() -> seekTo(time), 250);
        else if (!mChannel.isOnly()) nextLine(true);
        mKeyDown.resetTime();
    }

    private void seekTo(int time) {
        mPlayers.seekTo(time);
        showProgress();
        hideCenter();
    }

    @Override
    public void onKeyCenter() {
        hideInfo();
        showUI();
    }

    @Override
    public void onMenu() {
        if (isVisible(mBinding.control.home)) showControl(mBinding.control.home);
        else if (isVisible(mBinding.control.line)) showControl(mBinding.control.line);
        else showControl(mBinding.control.player);
        hideInfo();
        hideUI();
    }

    @Override
    public void onSingleTap() {
        onToggle();
    }

    @Override
    public void onDoubleTap() {
        if (isVisible(mBinding.control.getRoot())) hideControl();
        else onMenu();
    }

    @Override
    public void setPass(String pass) {
        boolean first = true;
        int position = mGroupAdapter.size();
        Iterator<Group> iterator = mHides.iterator();
        while (iterator.hasNext()) {
            Group item = iterator.next();
            if (!item.getPass().equals(pass)) continue;
            mGroupAdapter.add(mGroupAdapter.size(), item);
            if (first) mBinding.group.setSelectedPosition(position);
            if (first) onItemClick(mGroup = item);
            iterator.remove();
            first = false;
        }
    }

    @Override
    public void setLive(Live item) {
        LiveConfig.get().setHome(item);
        mHides.clear();
        hideControl();
        getLive();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerEvent(PlayerEvent event) {
        switch (event.getState()) {
            case 0:
                setTrackVisible(false);
                break;
            case Player.STATE_IDLE:
                break;
            case Player.STATE_BUFFERING:
                showProgress();
                break;
            case Player.STATE_READY:
                hideProgress();
                mPlayers.reset();
                setSpeedVisible();
                setTrackVisible(true);
                break;
            case Player.STATE_ENDED:
                onKeyDown();
                break;
        }
    }

    private void setSpeedVisible() {
        mBinding.control.speed.setVisibility(mPlayers.isVod() ? View.VISIBLE : View.GONE);
    }

    private void setTrackVisible(boolean visible) {
        mBinding.control.text.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_TEXT) ? View.VISIBLE : View.GONE);
        mBinding.control.audio.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_AUDIO) ? View.VISIBLE : View.GONE);
        mBinding.control.video.setVisibility(visible && mPlayers.haveTrack(C.TRACK_TYPE_VIDEO) ? View.VISIBLE : View.GONE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onErrorEvent(ErrorEvent event) {
        if (!event.isRetry() || mPlayers.addRetry() > 3) onError();
        else getUrl();
    }

    private void onError() {
        mPlayers.stop();
        checkNext();
    }

    private void checkNext() {
        if (mChannel.isLast()) {
            if (isGone(mBinding.recycler)) onKeyDown();
        } else {
            nextLine(true);
            getUrl();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Clock.start(mBinding.widget.time);
        mPlayers.play();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Clock.get().release();
        mPlayers.pause();
    }

    @Override
    public void onBackPressed() {
        if (isVisible(mBinding.control.getRoot())) {
            hideControl();
        } else if (isVisible(mBinding.widget.info)) {
            hideInfo();
        } else if (isVisible(mBinding.recycler)) {
            hideUI();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayers.release();
        Force.get().stop();
        ZLive.get().stop();
        TVBus.get().quit();
        App.removeCallbacks(mR1, mR2, mR3, mR4);
    }
}
