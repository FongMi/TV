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
    private Handler mHandler;
    private Players mPlayers;
    private Channel mChannel;
    private View mOldView;
    private Group mGroup;
    private Runnable mR1;
    private Runnable mR2;
    private Runnable mR3;

    public static void start(Activity activity) {
        if (LiveConfig.get().isEmpty()) return;
        activity.startActivity(new Intent(activity, LiveActivity.class));
    }

    private StyledPlayerView getPlayerView() {
        return Prefers.getRender() == 0 ? mBinding.surface : mBinding.texture;
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
        mR1 = this::hideInfo;
        mR2 = this::setGroupActivated;
        mR3 = this::setChannelActivated;
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
                onChildSelected(child, mGroup = (Group) mGroupAdapter.get(position));
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
        getPlayerView().setResizeMode(Prefers.getScale());
    }

    private void getLive() {
        mGroupAdapter.setItems(LiveConfig.get().getHome().getGroups(), null);
        mBinding.group.setVisibility(mGroupAdapter.size() == 1 ? View.GONE : View.VISIBLE);
        setPosition(LiveConfig.get().getKeep());
    }

    private void setPosition(int[] position) {
        mGroup = (Group) mGroupAdapter.get(position[0]);
        mBinding.group.setSelectedPosition(position[0]);
        mGroup.setPosition(position[1]);
        onItemClick(mGroup);
        onItemClick(mGroup.current());
    }

    private void setPosition() {
        mBinding.group.setSelectedPosition(mGroupAdapter.indexOf(mChannel.getGroup()));
        mBinding.channel.setSelectedPosition(mChannel.getGroup().getPosition());
    }

    private void onChildSelected(@Nullable RecyclerView.ViewHolder child, Group group) {
        if (mOldView != null) mOldView.setSelected(false);
        if (child == null) return;
        mOldView = child.itemView;
        mOldView.setSelected(true);
        onItemClick(group);
    }

    private void notifyItemChanged(VerticalGridView view, ArrayObjectAdapter adapter) {
        if (!view.isComputingLayout()) adapter.notifyArrayItemRangeChanged(0, adapter.size());
    }

    private void setGroupActivated() {
        for (int i = 0; i < mGroupAdapter.size(); i++) ((Group) mGroupAdapter.get(i)).setSelected(mGroup);
        mChannelAdapter.setItems(mGroup.getChannel(), null);
        notifyItemChanged(mBinding.group, mGroupAdapter);
    }

    private void setChannelActivated() {
        for (int i = 0; i < mChannelAdapter.size(); i++) ((Channel) mChannelAdapter.get(i)).setSelected(mChannel);
        notifyItemChanged(mBinding.channel, mChannelAdapter);
        LiveConfig.get().setKeep(mGroup, mChannel);
        getUrl();
    }

    private void toggle() {
        if (isVisible(mBinding.recycler)) hideUI();
        else showUI();
        hideInfo();
    }

    private void hideUI() {
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
        mBinding.info.getRoot().setVisibility(View.GONE);
    }

    private void showInfo() {
        mHandler.removeCallbacks(mR1);
        mBinding.info.name.setSelected(true);
        mBinding.info.name.setText(mChannel.getName());
        mBinding.info.line.setText(mChannel.getLineText());
        mBinding.info.number.setText(mChannel.getNumber());
        mBinding.info.getRoot().setVisibility(View.VISIBLE);
        mHandler.postDelayed(mR1, 5000);
    }

    @Override
    public void onItemClick(Group item) {
        if (item.isSelected()) return;
        mHandler.removeCallbacks(mR2);
        mHandler.postDelayed(mR2, 100);
        mBinding.channel.setSelectedPosition(mGroup.getPosition());
    }

    @Override
    public void onItemClick(Channel item) {
        mGroup.setPosition(mBinding.channel.getSelectedPosition());
        setChannel(item.group(mGroup));
        hideUI();
    }

    private void setChannel(Channel item) {
        mChannel = item;
        mHandler.removeCallbacks(mR3);
        mHandler.postDelayed(mR3, 100);
        showInfo();
    }

    private void nextGroup() {
        int position = mBinding.group.getSelectedPosition() + 1;
        if (position > mGroupAdapter.size() - 1) position = 0;
        mGroup = (Group) mGroupAdapter.get(position);
        mBinding.group.setSelectedPosition(position);
        mChannelAdapter.setItems(mGroup.getChannel(), null);
        mGroup.setPosition(0);
    }

    private void prevGroup() {
        int position = mBinding.group.getSelectedPosition() - 1;
        if (position < 0) position = mGroupAdapter.size() - 1;
        mGroup = (Group) mGroupAdapter.get(position);
        mBinding.group.setSelectedPosition(position);
        mChannelAdapter.setItems(mGroup.getChannel(), null);
        mGroup.setPosition(mGroup.getChannel().size() - 1);
    }

    private void getUrl() {
        mBinding.progress.getRoot().setVisibility(View.VISIBLE);
        mViewModel.getUrl(mChannel);
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
        int position = mGroup.getPosition() - 1;
        if (position < 0) prevGroup();
        else mGroup.setPosition(position);
        setChannel(mGroup.current());
    }

    @Override
    public void onKeyDown() {
        int position = mGroup.getPosition() + 1;
        if (position > mGroup.getChannel().size() - 1) nextGroup();
        else mGroup.setPosition(position);
        setChannel(mGroup.current());
    }

    @Override
    public void onKeyLeft() {
        if (mChannel.getUrls().size() == 1) return;
        mBinding.info.getRoot().setVisibility(View.VISIBLE);
        mBinding.info.line.setText(mChannel.prevLine().getLineText());
        getUrl();
    }

    @Override
    public void onKeyRight() {
        if (mChannel.getUrls().size() == 1) return;
        mBinding.info.getRoot().setVisibility(View.VISIBLE);
        mBinding.info.line.setText(mChannel.nextLine().getLineText());
        getUrl();
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
                else getUrl();
                break;
        }
    }

    private void onError() {
        mPlayers.setRetry(0);
        if (isGone(mBinding.recycler) && mChannel.isLastLine()) {
            onKeyDown();
        } else {
            mChannel.nextLine();
            getUrl();
        }
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
        Force.get().stop();
        mGroup.setSelected(false);
        mChannel.setSelected(false);
        EventBus.getDefault().unregister(this);
    }
}
