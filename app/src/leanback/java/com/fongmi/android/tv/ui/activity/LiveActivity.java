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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.databinding.ActivityLiveBinding;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.impl.PassCallback;
import com.fongmi.android.tv.model.LiveViewModel;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.player.source.Force;
import com.fongmi.android.tv.ui.custom.CustomKeyDownLive;
import com.fongmi.android.tv.ui.custom.CustomLiveListView;
import com.fongmi.android.tv.ui.custom.dialog.PassDialog;
import com.fongmi.android.tv.ui.presenter.ChannelPresenter;
import com.fongmi.android.tv.ui.presenter.GroupPresenter;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class LiveActivity extends BaseActivity implements GroupPresenter.OnClickListener, ChannelPresenter.OnClickListener, CustomKeyDownLive.Listener, CustomLiveListView.Callback, PassCallback {

    private ActivityLiveBinding mBinding;
    private ArrayObjectAdapter mChannelAdapter;
    private ArrayObjectAdapter mGroupAdapter;
    private CustomKeyDownLive mKeyDown;
    private LiveViewModel mViewModel;
    private List<Group> mHides;
    private Handler mHandler;
    private Players mPlayers;
    private Channel mChannel;
    private View mOldView;
    private Group mGroup;
    private Runnable mR0;
    private Runnable mR1;
    private Runnable mR2;
    private int count;

    public static void start(Activity activity) {
        if (LiveConfig.get().isEmpty()) return;
        activity.startActivity(new Intent(activity, LiveActivity.class));
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
        return mBinding = ActivityLiveBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mR0 = this::hideUI;
        mR1 = this::hideInfo;
        mR2 = this::setChannelActivated;
        mPlayers = new Players().init();
        mHandler = new Handler(Looper.getMainLooper());
        mKeyDown = CustomKeyDownLive.create(this);
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
        getPlayerView().setOnClickListener(view -> toggle());
        mBinding.group.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                onChildSelected(child, mGroup = (Group) mGroupAdapter.get(position));
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
        mViewModel.result.observe(this, result -> mPlayers.start(result));
    }

    private void setVideoView() {
        getPlayerView().setPlayer(mPlayers.exo());
        getPlayerView().setVisibility(View.VISIBLE);
        getPlayerView().setResizeMode(Prefers.getScale());
    }

    private void getLive() {
        List<Group> items = new ArrayList<>();
        items.add(Group.create(ResUtil.getString(R.string.keep)));
        for (Group group : LiveConfig.get().getHome().getGroups()) (group.isHidden() ? mHides : items).add(group);
        mGroupAdapter.setItems(items, null);
        LiveConfig.get().setKeep(items);
        setPosition(LiveConfig.get().getKeep(items));
    }

    private void setPosition(int[] position) {
        if (position[0] == -1) return;
        mGroup = (Group) mGroupAdapter.get(position[0]);
        mBinding.group.setSelectedPosition(position[0]);
        mGroup.setPosition(position[1]);
        onItemClick(mGroup);
        onItemClick(mGroup.current());
    }

    private void setPosition() {
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
        if (!mBinding.channel.isComputingLayout()) mChannelAdapter.notifyArrayItemRangeChanged(0, mChannelAdapter.size());
        getUrl();
    }

    private void toggle() {
        if (isVisible(mBinding.recycler)) hideUI();
        else showUI();
        hideInfo();
    }

    private void hideUI() {
        mHandler.removeCallbacks(mR0);
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
        mHandler.postDelayed(mR1, 5000);
        mBinding.info.name.setText(mChannel.getName());
        mBinding.info.line.setText(mChannel.getLineText());
        mBinding.info.number.setText(mChannel.getNumber());
        mBinding.info.getRoot().setVisibility(View.VISIBLE);
    }

    private void resetPass() {
        this.count = 0;
    }

    @Override
    public void onItemClick(Group item) {
        mChannelAdapter.setItems(mGroup.getChannel(), null);
        mBinding.channel.setSelectedPosition(mGroup.getPosition());
        if (!item.isKeep() || ++count < 5) return;
        mHandler.removeCallbacks(mR0);
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
        mHandler.removeCallbacks(mR2);
        mHandler.postDelayed(mR2, 100);
        LiveConfig.get().setKeep(mGroup, mChannel = item);
        showInfo();
    }

    private void getUrl() {
        mBinding.progress.getRoot().setVisibility(View.VISIBLE);
        mViewModel.getUrl(mChannel);
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
        if (isGone(mBinding.recycler) && mKeyDown.hasEvent(event)) return mKeyDown.onKeyDown(event);
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void setUITimer() {
        mHandler.removeCallbacks(mR0);
        mHandler.postDelayed(mR0, 5000);
    }

    @Override
    public void onShow(String number) {
        mBinding.digital.setText(number);
        mBinding.digital.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFind(String number) {
        mBinding.digital.setVisibility(View.GONE);
        setPosition(LiveConfig.get().find(number, mGroupAdapter.unmodifiableList()));
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
    public void onKeyLeft() {
        if (mChannel.getUrls().size() == 1) return;
        mChannel.prevLine();
        showInfo();
        getUrl();
    }

    @Override
    public void onKeyRight() {
        if (mChannel.getUrls().size() == 1) return;
        mChannel.nextLine();
        showInfo();
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
        Clock.start(mBinding.info.time);
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
        EventBus.getDefault().unregister(this);
    }
}
