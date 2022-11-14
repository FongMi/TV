package com.fongmi.android.tv.ui.activity;

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
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Epg;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.databinding.ActivityLiveBinding;
import com.fongmi.android.tv.databinding.ViewControllerLiveBinding;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.impl.PassCallback;
import com.fongmi.android.tv.model.LiveViewModel;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.player.source.Force;
import com.fongmi.android.tv.ui.custom.CustomKeyDownLive;
import com.fongmi.android.tv.ui.custom.CustomLiveListView;
import com.fongmi.android.tv.ui.custom.TrackSelectionDialog;
import com.fongmi.android.tv.ui.custom.dialog.LiveDialog;
import com.fongmi.android.tv.ui.custom.dialog.PassDialog;
import com.fongmi.android.tv.ui.presenter.ChannelPresenter;
import com.fongmi.android.tv.ui.presenter.GroupPresenter;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Response;

public class LiveActivity extends BaseActivity implements GroupPresenter.OnClickListener, ChannelPresenter.OnClickListener, CustomKeyDownLive.Listener, CustomLiveListView.Callback, PassCallback, LiveCallback {

    private ActivityLiveBinding mBinding;
    private ViewControllerLiveBinding mControl;
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

    private StyledPlayerView getPlayerView() {
        return Prefers.getRender() == 0 ? mBinding.surface : mBinding.texture;
    }

    private Group getKeep() {
        return (Group) mGroupAdapter.get(0);
    }

    private boolean isVisible(View view) {
        return view.getVisibility() == View.VISIBLE;
    }

    private boolean isGone(View view) {
        return view.getVisibility() == View.GONE;
    }

    @Override
    protected ViewBinding getBinding() {
        mBinding = ActivityLiveBinding.inflate(getLayoutInflater());
        mControl = ViewControllerLiveBinding.bind(getPlayerView().findViewById(com.google.android.exoplayer2.ui.R.id.exo_controller));
        return mBinding;
    }

    @Override
    protected void initView() {
        mR0 = this::hideUI;
        mR1 = this::hideInfo;
        mR2 = this::hideCenter;
        mR3 = this::setChannelActivated;
        mR4 = this::onError;
        mPlayers = new Players().init();
        mKeyDown = CustomKeyDownLive.create(this);
        mFormatDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        mFormatTime = new SimpleDateFormat("yyyy-MM-ddHH:mm", Locale.getDefault());
        mHides = new ArrayList<>();
        setRecyclerView();
        setViewModel();
        setVideoView();
        getLive();
    }

    @Override
    protected void initEvent() {
        mBinding.group.setListener(this);
        mBinding.channel.setListener(this);
        mControl.home.setOnClickListener(view -> onHome());
        mControl.scale.setOnClickListener(view -> onScale());
        mControl.speed.setOnClickListener(view -> onSpeed());
        mControl.tracks.setOnClickListener(view -> onTracks());
        mControl.line.setOnClickListener(view -> nextLine(false));
        mControl.speed.setOnLongClickListener(view -> onSpeedLong());
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

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(LiveViewModel.class);
        mViewModel.result.observe(this, result -> {
            if (result instanceof Live) setGroup((Live) result);
            else if (result instanceof Channel) mPlayers.start((Channel) result);
        });
    }

    private void getLive() {
        mViewModel.getLive(LiveConfig.get().getHome());
    }

    private void setVideoView() {
        getPlayerView().setPlayer(mPlayers.exo());
        getPlayerView().setVisibility(View.VISIBLE);
        getPlayerView().setResizeMode(Prefers.getLiveScale());
        getPlayerView().setOnClickListener(view -> onToggle());
        getPlayerView().setOnLongClickListener(view -> onLongPress());
        mControl.home.setVisibility(LiveConfig.isOnly() ? View.GONE : View.VISIBLE);
        mControl.scale.setText(ResUtil.getStringArray(R.array.select_scale)[Prefers.getLiveScale()]);
        mControl.speed.setText(mPlayers.getSpeed());
    }

    private void setGroup(Live home) {
        List<Group> items = new ArrayList<>();
        items.add(Group.create(ResUtil.getString(R.string.keep)));
        for (Group group : home.getGroups()) (group.isHidden() ? mHides : items).add(group);
        mGroupAdapter.setItems(items, null);
        setPosition(LiveConfig.get().find(items));
        mControl.home.setText(home.getName());
        Notify.dismiss();
    }

    private void setPosition(int[] position) {
        if (position[0] == -1) return;
        if (mGroupAdapter.size() == 1) return;
        mGroup = (Group) mGroupAdapter.get(position[0]);
        mBinding.group.setSelectedPosition(position[0]);
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

    private void onToggle() {
        getPlayerView().hideController();
        if (isVisible(mBinding.recycler)) hideUI();
        else showUI();
        hideInfo();
    }

    private void onHome() {
        App.post(() -> getPlayerView().hideController(), 150);
        LiveDialog.create(this).show();
    }

    private void onScale() {
        int scale = getPlayerView().getResizeMode();
        getPlayerView().setResizeMode(scale = scale == 4 ? 0 : scale + 1);
        mControl.scale.setText(ResUtil.getStringArray(R.array.select_scale)[scale]);
        Prefers.putLiveScale(scale);
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

    private void onTracks() {
        App.post(() -> getPlayerView().hideController(), 150);
        TrackSelectionDialog.createForPlayer(mPlayers.exo(), dialog -> {
        }).show(getSupportFragmentManager(), "tracks");
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

    private void hideInfo() {
        mBinding.widget.info.setVisibility(View.GONE);
    }

    private void showInfo() {
        mBinding.widget.info.setVisibility(View.VISIBLE);
        App.post(mR1, 5000);
        setInfo();
    }

    private void showEpg() {
        mControl.play.setText(mChannel.getData().getEpg());
        mBinding.widget.play.setText(mChannel.getData().getEpg());
    }

    private void hideCenter() {
        mBinding.widget.action.setImageResource(R.drawable.ic_play);
        mBinding.widget.center.setVisibility(View.GONE);
    }

    private void resetPass() {
        this.count = 0;
    }

    @Override
    public void onItemClick(Group item) {
        mChannelAdapter.setItems(mGroup.getChannel(), null);
        mBinding.channel.setSelectedPosition(mGroup.getPosition());
        if (!item.isKeep() || ++count < 5) return;
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
        mControl.name.setText(mChannel.getName());
        mControl.line.setText(mChannel.getLineText());
        mControl.number.setText(mChannel.getNumber());
        mBinding.widget.name.setText(mChannel.getName());
        mBinding.widget.line.setText(mChannel.getLineText());
        mBinding.widget.number.setText(mChannel.getNumber());
        mControl.line.setVisibility(mChannel.getLineVisible());
        mBinding.widget.logo.setVisibility(mChannel.getLogoVisible());
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
        OKHttp.newCall(epg).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                channel.setData(Epg.objectFrom(response.body().string(), mFormatTime));
                if (mChannel.equals(channel)) App.post(() -> showEpg());
            }
        });
    }

    private void getUrl() {
        mBinding.widget.progress.getRoot().setVisibility(View.VISIBLE);
        mViewModel.getUrl(mChannel);

    }

    private void prevLine(boolean show) {
        mChannel.prevLine();
        if (show) showInfo();
        else setInfo();
        getUrl();
    }

    private void nextLine(boolean show) {
        mChannel.nextLine();
        if (show) showInfo();
        else setInfo();
        getUrl();
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
        if (mGroup == null) return false;
        if (Utils.isMenuKey(event)) onLongPress();
        else if (isGone(mBinding.recycler) && !getPlayerView().isControllerFullyVisible() && mKeyDown.hasEvent(event)) return mKeyDown.onKeyDown(event);
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void setUITimer() {
        App.post(mR0, 5000);
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
        mBinding.widget.exoDuration.setText(mControl.exoDuration.getText());
        mBinding.widget.exoPosition.setText(mPlayers.getTime(time));
        mBinding.widget.action.setImageResource(time > 0 ? R.drawable.ic_forward : R.drawable.ic_rewind);
        mBinding.widget.center.setVisibility(View.VISIBLE);
    }

    @Override
    public void onKeyUp() {
        int position = mGroup.getPosition() - 1;
        if (position < 0) prevGroup(true);
        else mGroup.setPosition(position);
        setChannel(mGroup.current());
    }

    @Override
    public void onKeyDown() {
        int position = mGroup.getPosition() + 1;
        if (position > mGroup.getChannel().size() - 1) nextGroup(true);
        else mGroup.setPosition(position);
        setChannel(mGroup.current());
    }

    @Override
    public void onKeyLeft(int time) {
        if (isVisible(mBinding.widget.center)) App.post(mR2, 500);
        if (mChannel.isOnly() && mPlayers.isVod()) mPlayers.seekTo(time);
        else if (!mChannel.isOnly()) prevLine(true);
        mKeyDown.resetTime();
    }

    @Override
    public void onKeyRight(int time) {
        if (isVisible(mBinding.widget.center)) App.post(mR2, 500);
        if (mChannel.isOnly() && mPlayers.isVod()) mPlayers.seekTo(time);
        else if (!mChannel.isOnly()) nextLine(true);
        mKeyDown.resetTime();
    }

    @Override
    public void onKeyCenter() {
        hideInfo();
        showUI();
    }

    @Override
    public boolean onLongPress() {
        if (isVisible(mControl.home)) mControl.home.requestFocus();
        else if (isVisible(mControl.line)) mControl.line.requestFocus();
        else mControl.speed.requestFocus();
        getPlayerView().showController();
        hideInfo();
        hideUI();
        return true;
    }

    @Override
    public void setPass(String pass) {
        int position = mGroupAdapter.size() - 1;
        for (Group item : mHides) {
            if (!item.getPass().equals(pass)) continue;
            mGroupAdapter.add(position, item);
            mBinding.group.setSelectedPosition(position);
            mHides.remove(item);
            onItemClick(item);
            break;
        }
    }

    @Override
    public void setLive(Live item) {
        getPlayerView().hideController();
        LiveConfig.get().setHome(item);
        Notify.progress(this);
        mHides.clear();
        getLive();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerEvent(PlayerEvent event) {
        switch (event.getState()) {
            case 0:
                App.post(mR4, 10000);
                break;
            case Player.STATE_IDLE:
                break;
            case Player.STATE_BUFFERING:
                mBinding.widget.progress.getRoot().setVisibility(View.VISIBLE);
                break;
            case Player.STATE_READY:
                mPlayers.reset();
                App.removeCallbacks(mR4);
                mBinding.widget.progress.getRoot().setVisibility(View.GONE);
                TrackSelectionDialog.setVisible(mPlayers.exo(), mControl.tracks);
                break;
            case Player.STATE_ENDED:
                onKeyDown();
                break;
            default:
                App.removeCallbacks(mR4);
                if (!event.isRetry() || mPlayers.addRetry() > 2) onError();
                else getUrl();
                break;
        }
    }

    private void onError() {
        mPlayers.reset();
        if (mChannel.isLastLine()) {
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
        if (getPlayerView().isControllerFullyVisible()) {
            getPlayerView().hideController();
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
    }
}
