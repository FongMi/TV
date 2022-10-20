package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.databinding.ActivityLiveBinding;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.ui.adapter.ChannelAdapter;
import com.fongmi.android.tv.ui.adapter.GroupAdapter;
import com.fongmi.android.tv.utils.Prefers;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.StyledPlayerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class LiveActivity extends BaseActivity implements GroupAdapter.OnItemClickListener, ChannelAdapter.OnItemClickListener {

    private ActivityLiveBinding mBinding;
    private ChannelAdapter mChannelAdapter;
    private GroupAdapter mGroupAdapter;
    private Handler mHandler;
    private Players mPlayers;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, LiveActivity.class));
    }

    private StyledPlayerView getPlayerView() {
        return Prefers.getRender() == 0 ? mBinding.surface : mBinding.texture;
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityLiveBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mHandler = new Handler(Looper.getMainLooper());
        mPlayers = new Players().init();
        setRecyclerView();
        setVideoView();
    }

    @Override
    protected void initEvent() {
        EventBus.getDefault().register(this);
    }

    private void setRecyclerView() {
        mBinding.group.setAdapter(mGroupAdapter = new GroupAdapter(this));
        mBinding.channel.setAdapter(mChannelAdapter = new ChannelAdapter(this));
        mGroupAdapter.addAll(LiveConfig.get().getLives().get(0).getGroups());
    }

    private void setVideoView() {
        getPlayerView().setPlayer(mPlayers.exo());
        getPlayerView().setVisibility(View.VISIBLE);
        getPlayerView().setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
    }

    @Override
    public void onItemClick(Group item) {
        mChannelAdapter.addAll(item);
    }

    @Override
    public void onItemClick(Channel item) {
        mPlayers.start(item, 0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerEvent(PlayerEvent event) {
        switch (event.getState()) {
            case 0:
                break;
            case Player.STATE_IDLE:
                break;
            case Player.STATE_BUFFERING:
                //mBinding.widget.progress.getRoot().setVisibility(View.VISIBLE);
                break;
            case Player.STATE_READY:
                mPlayers.setRetry(0);
                //mBinding.widget.progress.getRoot().setVisibility(View.GONE);
                break;
            case Player.STATE_ENDED:
                break;
            default:
                if (!event.isRetry() || mPlayers.addRetry() > 3) onError(event.getMsg());
                else onRetry();
                break;
        }
    }

    private void onError(String msg) {

    }

    private void onRetry() {
        mPlayers.start(mChannelAdapter.getCurrent(), 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayers.release();
        EventBus.getDefault().unregister(this);
    }
}
