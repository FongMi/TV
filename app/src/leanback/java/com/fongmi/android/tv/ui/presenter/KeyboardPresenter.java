package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.View;
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

        void onTextClick(String text);

        void onIconClick(int resId);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterKeyboardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        ViewHolder holder = (ViewHolder) viewHolder;
        if (object instanceof String) {
            String text = (String) object;
            holder.binding.text.setText(text);
            holder.binding.icon.setVisibility(View.GONE);
            holder.binding.text.setVisibility(View.VISIBLE);
            setOnClickListener(holder, view -> mListener.onTextClick(text));
        } else {
            int resId = (int) object;
            holder.binding.icon.setImageResource(resId);
            holder.binding.text.setVisibility(View.GONE);
            holder.binding.icon.setVisibility(View.VISIBLE);
            setOnClickListener(holder, view -> mListener.onIconClick(resId));
        }
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