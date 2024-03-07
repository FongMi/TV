package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.AdapterArrayBinding;
import com.fongmi.android.tv.utils.ResUtil;

public class ArrayPresenter extends Presenter {

    private final OnClickListener mListener;
    private final String backward;
    private final String forward;
    private final String reverse;
    private int nextFocusDown;
    private int nextFocusUp;

    public ArrayPresenter(OnClickListener listener) {
        this.mListener = listener;
        this.backward = ResUtil.getString(R.string.play_backward);
        this.forward = ResUtil.getString(R.string.play_forward);
        this.reverse = ResUtil.getString(R.string.play_reverse);
    }

    public interface OnClickListener {

        void onRevSort();

        void onRevPlay(TextView view);

        boolean onArrayItemTouch();
    }

    public void setNextFocusDown(int nextFocusDown) {
        this.nextFocusDown = nextFocusDown;
    }

    public void setNextFocusUp(int nextFocusUp) {
        this.nextFocusUp = nextFocusUp;
    }

    @Override
    public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {
        return new ViewHolder(AdapterArrayBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object object) {
        ViewHolder holder = (ViewHolder) viewHolder;
        String text = object.toString();
        holder.binding.text.setText(text);
        holder.binding.text.setNextFocusUpId(nextFocusUp);
        holder.binding.text.setNextFocusDownId(nextFocusDown);
        holder.binding.text.setOnTouchListener((view, event) -> mListener.onArrayItemTouch());
        if (text.equals(reverse)) setOnClickListener(holder, view -> mListener.onRevSort());
        else if (text.equals(backward) || text.equals(forward)) setOnClickListener(holder, view -> mListener.onRevPlay(holder.binding.text));
        else setOnClickListener(holder, null);
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterArrayBinding binding;

        public ViewHolder(@NonNull AdapterArrayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}