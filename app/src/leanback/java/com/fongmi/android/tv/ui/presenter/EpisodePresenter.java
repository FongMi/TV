package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.databinding.AdapterEpisodeBinding;

public class EpisodePresenter extends Presenter {

    private final OnClickListener mListener;
    private int numColumns;
    private int numRows;
    private int nextFocusUp;
    private int nextFocusDown;

    public EpisodePresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {
        void onItemClick(Episode item);
    }

    public void setNextFocusUp(int nextFocus) {
        this.nextFocusUp = nextFocus;
    }

    public void setNextFocusDown(int nextFocus) {
        this.nextFocusDown = nextFocus;
    }

    public void setNumColumns(int numColumns) {
        this.numColumns = numColumns;
    }

    public void setNumRows(int numRows) {
        this.numRows = numRows;
    }
    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterEpisodeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        Episode item = (Episode) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        if (item.getIndex() < numColumns) holder.binding.text.setNextFocusUpId(nextFocusUp);
        else holder.binding.text.setNextFocusUpId(0);
        if (item.getIndex() >= (numRows - 1) * numColumns) holder.binding.text.setNextFocusDownId(nextFocusDown);
        else holder.binding.text.setNextFocusDownId(0);
        holder.binding.text.setMaxEms(Product.getEms());
        holder.binding.text.setActivated(item.isActivated());
        holder.binding.text.setText(item.getDesc().concat(item.getName()));
        setOnClickListener(holder, view -> mListener.onItemClick(item));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterEpisodeBinding binding;

        public ViewHolder(@NonNull AdapterEpisodeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}