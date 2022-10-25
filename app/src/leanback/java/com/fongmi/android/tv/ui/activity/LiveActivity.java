package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.leanback.widget.VerticalGridView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.databinding.ActivityLiveBinding;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.model.LiveViewModel;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.player.source.Force;
import com.fongmi.android.tv.ui.custom.CustomKeyDownLive;
import com.fongmi.android.tv.ui.presenter.ChannelPresenter;
import com.fongmi.android.tv.ui.presenter.GroupPresenter;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.Prefers;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.StyledPlayerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class LiveActivity extends BaseActivity implements GroupPresenter.OnClickListener, ChannelPresenter.OnClickListener, CustomKeyDownLive.Listener {

    private ActivityLiveBinding mBinding;
    private ArrayObjectAdapter mChannelAdapter;
    private ArrayObjectAdapter mGroupAdapter;
    private CustomKeyDownLive mKeyDown;
    private LiveViewModel mViewModel;
    private Runnable mRunnable;
    private Handler mHandler;
    private Players mPlayers;
    private Channel mCurrent;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, LiveActivity.class));
    }

    private StyledPlayerView getPlayerView() {
        return Prefers.getRender() == 0 ? mBinding.surface : mBinding.texture;
    }

    private Group getGroup() {
        return (Group) mGroupAdapter.get(mBinding.group.getSelectedPosition());
    }

    private boolean isVisible(View view) {
        return view.getVisibility() == View.VISIBLE;
    }

    private boolean isGone(View view) {
        return view.getVisibility() == View.GONE;
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityLiveBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mRunnable = this::hideInfo;
        mPlayers = new Players().init();
        mHandler = new Handler(Looper.getMainLooper());
        mKeyDown = CustomKeyDownLive.create(this);
        setRecyclerView();
        setViewModel();
        setVideoView();
        getLive();
    }

    @Override
    protected void initEvent() {
        EventBus.getDefault().register(this);
        getPlayerView().setOnClickListener(view -> toggle());
        mBinding.group.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                onItemClick((Group) mGroupAdapter.get(position));
            }
        });
    }

    private void setRecyclerView() {
        mBinding.group.setAdapter(new ItemBridgeAdapter(mGroupAdapter = new ArrayObjectAdapter(new GroupPresenter(this))));
        mBinding.channel.setAdapter(new ItemBridgeAdapter(mChannelAdapter = new ArrayObjectAdapter(new ChannelPresenter(this))));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(LiveViewModel.class);
        mViewModel.result.observe(this, result -> mPlayers.start(result));
    }

    private void setVideoView() {
        getPlayerView().setPlayer(mPlayers.exo());
        getPlayerView().setVisibility(View.VISIBLE);
        getPlayerView().setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
    }

    private void getLive() {
        int[] position = LiveConfig.get().getKeep();
        mGroupAdapter.setItems(LiveConfig.get().getHome().getGroups(), null);
        setPosition(position);
    }

    private void setPosition(int[] position) {
        Group group = (Group) mGroupAdapter.get(position[0]);
        mBinding.group.setSelectedPosition(position[0]);
        group.setPosition(position[1]);
        onItemClick(group);
        onItemClick(group.current());
    }

    private void setPosition() {
        mBinding.group.setSelectedPosition(mGroupAdapter.indexOf(mCurrent.getGroup()));
        mBinding.channel.setSelectedPosition(mCurrent.getGroup().getPosition());
    }

    private void notifyItemChanged(VerticalGridView view, ArrayObjectAdapter adapter) {
        if (!view.isComputingLayout()) adapter.notifyArrayItemRangeChanged(0, adapter.size());
    }

    private void setGroupActivated(Group item) {
        for (int i = 0; i < mGroupAdapter.size(); i++) {
            Group group = (Group) mGroupAdapter.get(i);
            group.setSelected(item.equals(group));
        }
        notifyItemChanged(mBinding.group, mGroupAdapter);
    }

    private void setChannelActivated(Channel item) {
        for (int i = 0; i < mChannelAdapter.size(); i++) {
            Channel channel = (Channel) mChannelAdapter.get(i);
            channel.setSelected(item.equals(channel));
        }
        notifyItemChanged(mBinding.channel, mChannelAdapter);
    }

    private void toggle() {
        if (isVisible(mBinding.recycler)) hideUI();
        else showUI();
        hideInfo();
    }

    private void hideUI() {
        mBinding.recycler.setVisibility(View.GONE);
        setPosition();
    }

    private void showUI() {
        mBinding.recycler.setVisibility(View.VISIBLE);
        mBinding.channel.requestFocus();
        setPosition();
    }

    private void hideInfo() {
        mBinding.info.getRoot().setVisibility(View.GONE);
    }

    private void showInfo(Channel item) {
        mHandler.removeCallbacks(mRunnable);
        mBinding.info.name.setSelected(true);
        mBinding.info.name.setText(item.getName());
        mBinding.info.line.setText(item.getLineText());
        mBinding.info.number.setText(item.getNumber());
        mBinding.info.getRoot().setVisibility(View.VISIBLE);
        mHandler.postDelayed(mRunnable, 5000);
    }

    @Override
    public void onItemClick(Group item) {
        mChannelAdapter.setItems(item.getChannel(), null);
        mBinding.channel.setSelectedPosition(item.getPosition());
        setGroupActivated(item);
    }

    @Override
    public void onItemClick(Channel item) {
        getGroup().setPosition(mBinding.channel.getSelectedPosition());
        setChannel(mCurrent = item.group(getGroup()));
        hideUI();
    }

    private void setChannel(Channel item) {
        LiveConfig.get().setKeep(getGroup(), item);
        setChannelActivated(item);
        showInfo(item);
        getUrl(item);
    }

    private void getUrl(Channel item) {
        mBinding.progress.getRoot().setVisibility(View.VISIBLE);
        mViewModel.getUrl(item);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (isGone(mBinding.recycler) && mKeyDown.hasEvent(event)) return mKeyDown.onKeyDown(event);
        else return super.dispatchKeyEvent(event);
    }

    @Override
    public void onShow(String number) {
        mBinding.digital.setText(number);
        mBinding.digital.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFind(String number) {
        mBinding.digital.setVisibility(View.GONE);
        int[] position = LiveConfig.get().find(number);
        if (position[0] != -1) setPosition(position);
    }

    @Override
    public void onKeyUp() {
        setChannel(mCurrent = getGroup().prev());
    }

    @Override
    public void onKeyDown() {
        setChannel(mCurrent = getGroup().next());
    }

    @Override
    public void onKeyLeft() {
        Channel item = getGroup().current().prevLine();
        if (item.getUrls().size() == 1) return;
        mBinding.info.getRoot().setVisibility(View.VISIBLE);
        mBinding.info.line.setText(item.getLineText());
        getUrl(item);
    }

    @Override
    public void onKeyRight() {
        Channel item = getGroup().current().nextLine();
        if (item.getUrls().size() == 1) return;
        mBinding.info.getRoot().setVisibility(View.VISIBLE);
        mBinding.info.line.setText(item.getLineText());
        getUrl(item);
    }

    @Override
    public void onKeyCenter() {
        hideInfo();
        showUI();
    }

    @Override
    public void onLongPress() {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerEvent(PlayerEvent event) {
        switch (event.getState()) {
            case 0:
                break;
            case Player.STATE_IDLE:
                break;
            case Player.STATE_BUFFERING:
                mBinding.progress.getRoot().setVisibility(View.VISIBLE);
                break;
            case Player.STATE_READY:
                mPlayers.setRetry(0);
                mBinding.progress.getRoot().setVisibility(View.GONE);
                break;
            case Player.STATE_ENDED:
                break;
            default:
                if (!event.isRetry() || mPlayers.addRetry() > 1) onError();
                else getUrl(getGroup().current());
                break;
        }
    }

    private void onError() {
        mPlayers.setRetry(0);
        if (isGone(mBinding.recycler) && getGroup().current().isLastLine()) onKeyDown();
        else getUrl(getGroup().current().nextLine());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Clock.start(mBinding.info.time, "HH:mm:ss");
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
        if (isVisible(mBinding.info.getRoot())) {
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
        Force.get().destroy();
        EventBus.getDefault().unregister(this);
    }
}
