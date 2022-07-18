package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.ApiConfig;
import com.fongmi.bear.R;
import com.fongmi.bear.bean.Vod;
import com.fongmi.bear.databinding.ActivityDetailBinding;
import com.fongmi.bear.event.PlayerEvent;
import com.fongmi.bear.model.SiteViewModel;
import com.fongmi.bear.player.Players;
import com.fongmi.bear.ui.presenter.EpisodePresenter;
import com.fongmi.bear.ui.presenter.FlagPresenter;
import com.fongmi.bear.ui.presenter.GroupPresenter;
import com.fongmi.bear.utils.ResUtil;
import com.google.android.exoplayer2.Player;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends BaseActivity {

    private ActivityDetailBinding mBinding;
    private ArrayObjectAdapter mFlagAdapter;
    private ArrayObjectAdapter mGroupAdapter;
    private ArrayObjectAdapter mEpisodeAdapter;
    private EpisodePresenter mEpisodePresenter;
    private SiteViewModel mSiteViewModel;
    private View mOldView;

    private String getId() {
        return getIntent().getStringExtra("id");
    }

    private Vod.Flag getVodFlag() {
        return (Vod.Flag) mFlagAdapter.get(mBinding.flag.getSelectedPosition());
    }

    public static void start(Activity activity, String id) {
        Intent intent = new Intent(activity, DetailActivity.class);
        intent.putExtra("id", id);
        activity.startActivity(intent);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityDetailBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mBinding.progressLayout.showProgress();
        mBinding.video.setPlayer(Players.get().callback(this).exo());
        setRecyclerView();
        setViewModel();
        getDetail();
    }

    @Override
    protected void initEvent() {
        EventBus.getDefault().register(this);
        mBinding.flag.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                setFlagActivated(child, position);
            }
        });
        mBinding.group.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (mEpisodeAdapter.size() > 20) mBinding.episode.setSelectedPosition(position * 20);
            }
        });
        mEpisodePresenter.setOnClickListener(item -> {
            setEpisodeActivated(item);
            getPlayer(item.getUrl());
        });
        mBinding.frame.setOnClickListener(view -> {
            mBinding.video.setPlayer(null);
            PlayActivity.newInstance(getActivity(), getVodFlag());
        });
    }

    private void setRecyclerView() {
        mBinding.flag.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.flag.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.flag.setAdapter(new ItemBridgeAdapter(mFlagAdapter = new ArrayObjectAdapter(new FlagPresenter())));
        mBinding.episode.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.episode.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.episode.setAdapter(new ItemBridgeAdapter(mEpisodeAdapter = new ArrayObjectAdapter(mEpisodePresenter = new EpisodePresenter())));
        mBinding.group.setHorizontalSpacing(ResUtil.dp2px(8));
        mBinding.group.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.group.setAdapter(new ItemBridgeAdapter(mGroupAdapter = new ArrayObjectAdapter(new GroupPresenter())));
    }

    private void getDetail() {
        mSiteViewModel.detailContent(getId());
    }

    private void getPlayer(String id) {
        mBinding.progress.getRoot().setVisibility(View.VISIBLE);
        mSiteViewModel.playerContent(getVodFlag().getFlag(), id);
    }

    private void setViewModel() {
        mSiteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mSiteViewModel.player.observe(this, object -> Players.get().setMediaSource(object));
        mSiteViewModel.result.observe(this, result -> {
            if (result.getList().isEmpty()) mBinding.progressLayout.showErrorText();
            else setDetail(result.getList().get(0));
        });
    }

    private void setDetail(Vod item) {
        mBinding.progressLayout.showContent();
        mBinding.name.setText(item.getVodName());
        setText(mBinding.year, R.string.detail_year, item.getVodYear());
        setText(mBinding.area, R.string.detail_area, item.getVodArea());
        setText(mBinding.type, R.string.detail_type, item.getTypeName());
        setText(mBinding.actor, R.string.detail_actor, item.getVodActor());
        setText(mBinding.content, R.string.detail_content, item.getVodContent());
        setText(mBinding.director, R.string.detail_director, item.getVodDirector());
        setText(mBinding.site, R.string.detail_site, ApiConfig.get().getHome().getName());
        mFlagAdapter.addAll(0, item.getVodFlags());
    }

    private void setText(TextView view, int resId, String text) {
        if (text.isEmpty()) view.setVisibility(View.GONE);
        else view.setText(ResUtil.getString(resId, text));
    }

    private void setFlagActivated(RecyclerView.ViewHolder child, int position) {
        if (mOldView != null) mOldView.setActivated(false);
        if (child == null) return;
        mOldView = child.itemView;
        mOldView.setActivated(true);
        setEpisode((Vod.Flag) mFlagAdapter.get(position));
    }

    private void setEpisodeActivated(Vod.Flag.Episode item) {
        for (int i = 0; i < mFlagAdapter.size(); i++) {
            Vod.Flag flag = (Vod.Flag) mFlagAdapter.get(i);
            if (mBinding.flag.getSelectedPosition() == i) flag.setActivated(item);
            else flag.deactivated();
        }
        mEpisodeAdapter.notifyArrayItemRangeChanged(0, mEpisodeAdapter.size());
    }

    private void setEpisode(Vod.Flag item) {
        mEpisodeAdapter.clear();
        mEpisodeAdapter.addAll(0, item.getEpisodes());
        setGroup(item.getEpisodes().size());
    }

    private void setGroup(int size) {
        List<String> items = new ArrayList<>();
        int itemSize = (int) Math.ceil(size / 20.0f);
        for (int i = 0; i < itemSize; i++) items.add(String.valueOf(i * 20 + 1));
        mBinding.group.setVisibility(itemSize > 1 ? View.VISIBLE : View.GONE);
        mGroupAdapter.clear();
        mGroupAdapter.addAll(0, items);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlaybackStateChanged(PlayerEvent event) {
        mBinding.progress.getRoot().setVisibility(event.getState() == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mBinding.video.setPlayer(Players.get().exo());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Players.get().stop();
        EventBus.getDefault().unregister(this);
    }
}
