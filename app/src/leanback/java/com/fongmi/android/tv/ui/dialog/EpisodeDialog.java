package com.fongmi.android.tv.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.databinding.DialogEpisodeBinding;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.fongmi.android.tv.ui.presenter.ArrayPresenter;
import com.fongmi.android.tv.ui.presenter.EpisodePresenter;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class EpisodeDialog extends BaseDialog implements ArrayPresenter.OnClickListener {

    private DialogEpisodeBinding binding;
    private VideoActivity activity;
    private View mFocus1;
    private List<Episode> episodes;
    private EpisodePresenter mEpisodePresenter;
    private ArrayObjectAdapter mEpisodeAdapter;
    private ArrayObjectAdapter mArrayAdapter;
    private ArrayPresenter mArrayPresenter;
    private int groupSize;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (activity.isFullscreen() && KeyUtil.isMenuKey(event) && Setting.getFullscreenMenuKey() == 1) dismiss();
                return false;
            }
        });
        return dialog;
    }

    public static EpisodeDialog create() {
        return new EpisodeDialog();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogEpisodeBinding.inflate(inflater, container, false);
    }

    public EpisodeDialog() {

    }

    public EpisodeDialog episodes(List<Episode> episodes) {
        this.episodes = episodes;
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
        this.activity = (VideoActivity) activity;
    }

    @Override
    protected void initView() {
        this.binding.array.setHorizontalSpacing(ResUtil.dp2px(8));
        this.binding.array.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        this.binding.array.setAdapter(new ItemBridgeAdapter(mArrayAdapter = new ArrayObjectAdapter(mArrayPresenter = new ArrayPresenter(this))));
        this.binding.episodeVert.setVerticalSpacing(ResUtil.dp2px(8));
        this.binding.episodeVert.setHorizontalSpacing(ResUtil.dp2px(8));
        this.binding.episodeVert.setAdapter(new ItemBridgeAdapter(mEpisodeAdapter = new ArrayObjectAdapter(mEpisodePresenter = new EpisodePresenter(this::setEpisodeActivated))));
        setEpisodeAdapter(episodes);
    }

    @Override
    protected void initEvent() {
        this.binding.array.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (mEpisodeAdapter.size() > getGroupSize()) setEpisodeSelectedPosition(position * getGroupSize());
            }
        });
        this.binding.episodeVert.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                super.onChildViewHolderSelected(parent, child, position, subposition);
                if (child != null ) mFocus1 = child.itemView;
                setEpisodeChildKeyListener(child, position);
            }
        });
    }

    private void setEpisodeChildKeyListener(RecyclerView.ViewHolder child, int position) {
        int itemCount = binding.episodeVert.getAdapter().getItemCount();
        if (itemCount <= 0) return;
        int columns = mEpisodePresenter.getNumColumns();
        if ((position + columns >= itemCount) && ((position % columns) + 1 > (itemCount % columns))) {
            child.itemView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.getAction() == KeyEvent.ACTION_DOWN) {
                        View lastItem =  binding.episodeVert.getLayoutManager().findViewByPosition(itemCount - 1);
                        if (lastItem != null) lastItem.requestFocus();
                    }
                    return false;
                }
            });
        }
    }

    private void setEpisodeSelectedPosition(int position) {
        this.binding.episodeVert.setSelectedPosition(position);
    }

    private int getEpisodePosition() {
        for (int i = 0; i < mEpisodeAdapter.size(); i++) if (((Episode) mEpisodeAdapter.get(i)).isActivated()) return i;
        return 0;
    }

    public int getGroupSize() {
        return groupSize;
    }

    public void setGroupSize(int size) {
        groupSize = size;
    }

    private void setEpisodeAdapter(List<Episode> items) {
        this.binding.episodeVert.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        mEpisodePresenter.setNextFocusUp(this.binding.array.getId());
        mArrayPresenter.setNextFocusDown(this.binding.episodeVert.getId());
        setEpisodeView(items);
        setArrayAdapter(items.size());
        mEpisodeAdapter.setItems(items, null);
        this.binding.episodeVert.postDelayed(() -> {
            int position = getEpisodePosition();
            setEpisodeSelectedPosition(position);
            binding.episodeVert.postDelayed(() -> {
                View selectedItem = binding.episodeVert.getLayoutManager().findViewByPosition(position);
                if (selectedItem != null) selectedItem.requestFocus();
            }, 300);
        }, 1000);
    }

    private void setArrayAdapter(int size) {
        if (size > 200) setGroupSize(100);
        else if (size > 100) setGroupSize(40);
        else setGroupSize(20);
        List<String> items = new ArrayList<>();
        this.binding.array.setVisibility(size > 1 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < size; i += getGroupSize()) items.add((i + 1) + "-" + Math.min(i + getGroupSize(), size));
        mArrayAdapter.setItems(items, null);
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
        int width =  ResUtil.getScreenWidth() - ResUtil.dp2px(48);
        this.binding.episodeVert.setNumColumns(numColumns);
        this.binding.episodeVert.setColumnWidth((width - ((numColumns - 1) * ResUtil.dp2px(8))) / numColumns);
        this.binding.episodeVert.setWindowAlignmentOffsetPercent(10f);
        ViewGroup.LayoutParams params = this.binding.getRoot().getLayoutParams();
        int height = ResUtil.getScreenHeight();
        if (rowNum > 6) height = ResUtil.getScreenHeight() * 3 / 4;
        else if (rowNum > 2) height = ResUtil.getScreenHeight() * 1 / 2;
        else if (rowNum > 0) height = ResUtil.getScreenHeight() * 1 / 3;
        params.height = height;
        this.binding.getRoot().setLayoutParams(params);
        mEpisodePresenter.setNumColumns(numColumns);
        mEpisodePresenter.setNumRows(rowNum);
    }

    private void setEpisodeActivated(Episode item) {
        if (!this.activity.isFullscreen() || !item.isActivated()) this.activity.setEpisodeActivated(item);
        this.dismiss();
    }

    @Override
    public boolean onArrayItemTouch() {
        return false;
    }

    @Override
    public void onRevSort() {

    }

    @Override
    public void onRevPlay(TextView view) {

    }

}
