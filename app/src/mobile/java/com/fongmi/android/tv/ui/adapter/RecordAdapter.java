package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.databinding.AdapterCollectRecordBinding;
import com.fongmi.android.tv.utils.Prefers;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<String> mItems;
    private final Gson mGson;

    public RecordAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mGson = new Gson();
        this.mItems = getItems();
        this.mListener.onDataChanged(mItems.size());
    }

    public interface OnClickListener {

        void onItemClick(String text);

        void onDataChanged(int size);
    }

    private List<String> getItems() {
        if (Prefers.getKeyword().isEmpty()) return new ArrayList<>();
        return mGson.fromJson(Prefers.getKeyword(), new TypeToken<List<String>>() {}.getType());
    }

    private void checkToAdd(String item) {
        int index = mItems.indexOf(item);
        if (index == -1) {
            mItems.add(0, item);
            notifyItemInserted(0);
        } else {
            mItems.remove(index);
            mItems.add(0, item);
            notifyItemRangeChanged(0, mItems.size());
        }
        if (mItems.size() > 8) {
            mItems.remove(8);
            notifyItemRemoved(8);
        }
    }

    public void add(String item) {
        checkToAdd(item);
        mListener.onDataChanged(getItemCount());
        Prefers.putKeyword(mGson.toJson(mItems));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterCollectRecordBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String text = mItems.get(position);
        holder.binding.text.setText(text);
        holder.binding.text.setOnClickListener(v -> mListener.onItemClick(text));
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

        private final AdapterCollectRecordBinding binding;

        ViewHolder(@NonNull AdapterCollectRecordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
            mItems.remove(getLayoutPosition());
            notifyItemRemoved(getLayoutPosition());
            mListener.onDataChanged(getItemCount());
            Prefers.putKeyword(mGson.toJson(mItems));
            return true;
        }
    }
}
