package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.AdapterGroupBinding;
import com.fongmi.android.tv.utils.ResUtil;

public class GroupPresenter extends Presenter {

    private final OnClickListener mListener;
    private final String backward;
    private final String forward;
    private final String reverse;

    public GroupPresenter(OnClickListener listener) {
        this.mListener = listener;
        this.backward = ResUtil.getString(R.string.play_backward);
        this.forward = ResUtil.getString(R.string.play_forward);
        this.reverse = ResUtil.getString(R.string.play_reverse);
    }

    public interface OnClickListener {

        void onRevSort();

        void onRevPlay(TextView view);
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterGroupBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        ViewHolder holder = (ViewHolder) viewHolder;
        String text = object.toString();
        holder.binding.text.setText(text);
        if (text.equals(reverse)) setOnClickListener(holder, view -> mListener.onRevSort());
        if (text.equals(backward) || text.equals(forward)) setOnClickListener(holder, view -> mListener.onRevPlay(holder.binding.text));
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterGroupBinding binding;

        public ViewHolder(@NonNull AdapterGroupBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}