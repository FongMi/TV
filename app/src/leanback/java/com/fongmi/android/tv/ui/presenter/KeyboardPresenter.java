package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.databinding.AdapterKeyboardBinding;

public class KeyboardPresenter extends Presenter {

    private final OnClickListener mListener;

    public KeyboardPresenter(OnClickListener listener) {
        this.mListener = listener;
    }

    public interface OnClickListener {
        void onItemClick(String item);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterKeyboardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        String item = (String) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        setOnClickListener(holder, view -> mListener.onItemClick(item));
        holder.binding.text.setText(item);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterKeyboardBinding binding;

        public ViewHolder(@NonNull AdapterKeyboardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}