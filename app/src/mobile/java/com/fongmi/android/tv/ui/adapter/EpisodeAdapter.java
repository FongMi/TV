package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.databinding.AdapterEpisodeGridBinding;
import com.fongmi.android.tv.databinding.AdapterEpisodeHoriBinding;
import com.fongmi.android.tv.databinding.AdapterEpisodeVertBinding;
import com.fongmi.android.tv.ui.base.BaseEpisodeHolder;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.holder.EpisodeGridHolder;
import com.fongmi.android.tv.ui.holder.EpisodeHoriHolder;
import com.fongmi.android.tv.ui.holder.EpisodeVertHolder;

import java.util.ArrayList;
import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<BaseEpisodeHolder> {

    private final OnClickListener mListener;
    private final List<Episode> mItems;
    private final int viewType;

    public EpisodeAdapter(OnClickListener listener, int viewType) {
        this(listener, viewType, new ArrayList<>());
    }

    public EpisodeAdapter(OnClickListener listener, int viewType, ArrayList<Episode> items) {
        this.mListener = listener;
        this.viewType = viewType;
        this.mItems = items;
    }

    public interface OnClickListener {

        void onItemClick(Episode item);
    }

    public void addAll(List<Episode> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public int getPosition() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).isActivated()) return i;
        return 0;
    }

    public int getPosition(Episode item) {
        return mItems.indexOf(item);
    }

    public Episode getActivated() {
        return mItems.get(getPosition());
    }

    public Episode getNext() {
        int current = getPosition();
        int max = getItemCount() - 1;
        current = ++current > max ? max : current;
        return mItems.get(current);
    }

    public Episode getPrev() {
        int current = getPosition();
        current = --current < 0 ? 0 : current;
        return mItems.get(current);
    }

    public List<Episode> getItems() {
        return mItems;
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @Override
    public void onBindViewHolder(@NonNull BaseEpisodeHolder holder, int position) {
        holder.initView(mItems.get(position));
    }

    @NonNull
    @Override
    public BaseEpisodeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case ViewType.HORI:
                return new EpisodeHoriHolder(AdapterEpisodeHoriBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
            case ViewType.VERT:
                return new EpisodeVertHolder(AdapterEpisodeVertBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
            default:
                return new EpisodeGridHolder(AdapterEpisodeGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), mListener);
        }
    }
}