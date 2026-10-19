package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.databinding.AdapterLiveBinding;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.List;

public class LiveAdapter extends RecyclerView.Adapter<LiveAdapter.ViewHolder> {

    private final OnClickListener listener;
    private final List<Live> mItems;
    private boolean action;

    public LiveAdapter(OnClickListener listener) {
        this.listener = listener;
        this.mItems = LiveConfig.get().getLives();
    }

    public interface OnClickListener {

        void onItemClick(Live item);

        void onBootClick(int position, Live item);

        void onPassClick(int position, Live item);

        boolean onBootLongClick(Live item);

        boolean onPassLongClick(Live item);
    }

    public void setAction(boolean action) {
        this.action = action;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterLiveBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Live item = mItems.get(position);
        holder.binding.text.setText(item.getName());
        holder.binding.text.setSelected(item.isSelected());
        holder.binding.boot.setImageResource(item.getBootIcon());
        holder.binding.pass.setImageResource(item.getPassIcon());
        holder.binding.boot.setContentDescription(getActionDescription(R.string.dialog_action_live_boot, item.isBoot()));
        holder.binding.pass.setContentDescription(getActionDescription(R.string.dialog_action_live_password, !item.isPass()));
        holder.binding.boot.setVisibility(action ? View.VISIBLE : View.GONE);
        holder.binding.pass.setVisibility(action ? View.VISIBLE : View.GONE);
        holder.binding.text.setOnClickListener(v -> listener.onItemClick(item));
        holder.binding.boot.setOnClickListener(v -> listener.onBootClick(position, item));
        holder.binding.pass.setOnClickListener(v -> listener.onPassClick(position, item));
        holder.binding.boot.setOnLongClickListener(v -> listener.onBootLongClick(item));
        holder.binding.pass.setOnLongClickListener(v -> listener.onPassLongClick(item));
    }

    private static String getActionDescription(@StringRes int action, boolean enabled) {
        return ResUtil.getString(R.string.dialog_action_toggle_hint, ResUtil.getString(action), ResUtil.getString(enabled ? R.string.setting_on : R.string.setting_off));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterLiveBinding binding;

        ViewHolder(@NonNull AdapterLiveBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
