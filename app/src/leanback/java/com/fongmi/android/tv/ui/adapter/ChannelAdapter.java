package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.databinding.AdapterChannelBinding;
import com.fongmi.android.tv.ui.adapter.holder.ChannelHolder;

import java.util.ArrayList;
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelHolder> {

    private OnItemClickListener mListener;
    private final List<Channel> mItems;
    private Channel current;
    private boolean focus;
    private int position;
    private Group group;

    public ChannelAdapter(OnItemClickListener listener) {
        this.mItems = new ArrayList<>();
        this.mListener = listener;
    }

    public interface OnItemClickListener {

        void onItemClick(Channel item);
    }

    private Channel getItem() {
        return mItems.get(position);
    }

    public Channel getCurrent() {
        return current;
    }

    public void setCurrent(Channel current) {
        this.current = current;
    }

    public boolean isFocus() {
        return focus;
    }

    public void setFocus(boolean focus) {
        this.focus = focus;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public void addAll(Group group) {
        setGroup(group);
        mItems.clear();
        mItems.addAll(group.getChannel());
        notifyDataSetChanged();
    }

    public void setSelected() {
        for (int i = 0; i < mItems.size(); i++) mItems.get(i).setSelect(i == position);
        notifyDataSetChanged();
        setFocus(true);
    }

    public void setChannel() {
        if (position < 0 || position > mItems.size() - 1) return;
        //if (!getGroup().isHidden()) getItem().putKeep();
        mListener.onItemClick(getItem());
        getGroup().setPosition(position);
        getItem().setGroup(getGroup());
        setCurrent(getItem());
        setSelected();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ChannelHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ChannelHolder(this, AdapterChannelBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelHolder holder, int position) {
        holder.setView(mItems.get(position));
    }
}