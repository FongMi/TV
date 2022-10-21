package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.databinding.ActivityLiveBinding;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.ui.presenter.ChannelPresenter;
import com.fongmi.android.tv.ui.presenter.GroupPresenter;
import com.fongmi.android.tv.utils.Prefers;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.StyledPlayerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class LiveActivity extends BaseActivity implements GroupPresenter.OnClickListener, ChannelPresenter.OnClickListener {

    private ActivityLiveBinding mBinding;
    private ArrayObjectAdapter mChannelAdapter;
    private ArrayObjectAdapter mGroupAdapter;
    private Handler mHandler;
    private Players mPlayers;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, LiveActivity.class));
    }

    private StyledPlayerView getPlayerView() {
        return Prefers.getRender() == 0 ? mBinding.surface : mBinding.texture;
    }

    private Group getGroup() {
        return (Group) mGroupAdapter.get(mBinding.group.getSelectedPosition());
    }

    private Channel getChannel() {
        return (Channel) mChannelAdapter.get(mBinding.channel.getSelectedPosition());
    }

    private boolean isRecyclerVisible() {
        return mBinding.recycler.getVisibility() == View.VISIBLE;
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
        getLive();
    }

    @Override
    protected void initEvent() {
        EventBus.getDefault().register(this);
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

    private void getLive() {
        mGroupAdapter.setItems(LiveConfig.get().getHome().getGroups(), null);
        mBinding.channel.requestFocus();
    }

    private void setVideoView() {
        getPlayerView().setPlayer(mPlayers.exo());
        getPlayerView().setVisibility(View.VISIBLE);
        getPlayerView().setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
    }

    @Override
    public void onItemClick(Group item) {
        mChannelAdapter.setItems(item.getChannel(), null);
        mBinding.channel.setSelectedPosition(item.getPosition());
    }

    @Override
    public void onItemClick(Channel item) {
        getGroup().setPosition(mBinding.channel.getSelectedPosition());
        mPlayers.start(item);
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
                else mPlayers.start(getChannel());
                break;
        }
    }

    private void onError() {
        int index = getChannel().getIndex() + 1;
        int size = getChannel().getUrls().size();
        mPlayers.setRetry(0);
        if (index == size) {
            //TODO Auto Next
            mBinding.progress.getRoot().setVisibility(View.GONE);
        } else {
            getChannel().setIndex(index);
            mPlayers.start(getChannel());
        }
    }

    @Override
    public void onBackPressed() {
        if (isRecyclerVisible()) {
            mBinding.recycler.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayers.release();
        EventBus.getDefault().unregister(this);
    }
}
