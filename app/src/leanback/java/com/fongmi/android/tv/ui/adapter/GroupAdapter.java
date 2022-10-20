package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.databinding.AdapterLiveGroupBinding;
import com.fongmi.android.tv.ui.adapter.holder.GroupHolder;

import java.util.ArrayList;
import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupHolder> {

    private OnItemClickListener mListener;
    private final List<Group> mItems;
    private final List<Group> mHides;
    private boolean focus;
    private int position;

    public GroupAdapter(OnItemClickListener listener) {
        this.mItems = new ArrayList<>();
        this.mHides = new ArrayList<>();
        this.mListener = listener;
    }

    public interface OnItemClickListener {

        void onItemClick(Group item);
    }

    private Group getItem() {
        return mItems.get(position);
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

    public void addAll(List<Group> items) {
        mItems.clear();
        addGroup(items);
        notifyDataSetChanged();
    }

    private void addGroup(List<Group> items) {
        for (Group item : items) if (item.isHidden()) mHides.add(item); else mItems.add(item);
    }

    public void setSelected() {
        for (int i = 0; i < mItems.size(); i++) mItems.get(i).setSelect(i == position);
        notifyDataSetChanged();
        setFocus(true);
    }

    public void setType() {
        mListener.onItemClick(getItem());
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public GroupHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GroupHolder(this, AdapterLiveGroupBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull GroupHolder holder, int position) {
        holder.setView(mItems.get(position));
    }
}
