package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.databinding.AdapterPartBinding;

public class PartPresenter extends Presenter {

    private final OnClickListener mListener;
    private int nextFocus;

    public PartPresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {
        void onItemClick(String item);
    }

    public void setNextFocusUp(int nextFocus) {
        this.nextFocus = nextFocus;
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterPartBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        String text = object.toString();
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(text);
        holder.binding.text.setMaxEms(Product.getEms());
        holder.binding.text.setNextFocusUpId(nextFocus);
        setOnClickListener(holder, view -> mListener.onItemClick(text));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterPartBinding binding;

        public ViewHolder(@NonNull AdapterPartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}