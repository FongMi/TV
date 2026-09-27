package com.fongmi.android.tv.ui.adapter;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.PlaybackContent;
import com.fongmi.android.tv.databinding.AdapterPlaybackContentBinding;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

public class PlaybackContentAdapter extends RecyclerView.Adapter<PlaybackContentAdapter.ViewHolder> {

    private final OnClickListener listener;
    private final List<PlaybackContent> mItems;
    private String timeWidthSample = "00:00";
    private String keyword = "";
    private boolean danmakuOnly;
    private long position;
    private int active = RecyclerView.NO_POSITION;

    public PlaybackContentAdapter(OnClickListener listener) {
        this.listener = listener;
        this.mItems = new ArrayList<>();
        setHasStableIds(true);
    }

    public interface OnClickListener {

        void onItemClick(PlaybackContent item);
    }

    public void setItems(List<PlaybackContent> items) {
        setItems(items, "");
    }

    public void setItems(List<PlaybackContent> items, String keyword) {
        String query = keyword.trim();
        if (mItems.equals(items) && this.keyword.equals(query)) return;
        this.keyword = query;
        mItems.clear();
        mItems.addAll(items);
        timeWidthSample = "00:00";
        long maximum = 0;
        boolean milliseconds = false;
        danmakuOnly = true;
        active = RecyclerView.NO_POSITION;
        for (int i = 0; i < items.size(); i++) {
            PlaybackContent item = items.get(i);
            long itemPosition = item.getPosition();
            danmakuOnly &= item.danmaku();
            if (!item.danmaku() && item.isActive(position)) active = i;
            maximum = Math.max(maximum, itemPosition);
            milliseconds |= !item.danmaku() && itemPosition % 1000 != 0;
        }
        if (maximum >= 3_600_000) timeWidthSample = maximum / 3_600_000 + ":00:00";
        if (milliseconds) timeWidthSample += ".000";
        notifyDataSetChanged();
    }

    public PlaybackContent getItem(int position) {
        return position < 0 || position >= mItems.size() ? null : mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mItems.get(position).getId();
    }

    public void setPosition(long position) {
        if (this.position == position) return;
        long previous = this.position;
        this.position = position;
        active = RecyclerView.NO_POSITION;
        if (danmakuOnly) {
            if (previous / 1000 != position / 1000) {
                notifyDanmakuSecond(previous / 1000);
                notifyDanmakuSecond(position / 1000);
            }
            return;
        }
        for (int i = 0; i < mItems.size(); i++) {
            PlaybackContent item = mItems.get(i);
            boolean activeNow = isActive(item, position);
            if (!item.danmaku() && activeNow) active = i;
            if (isActive(item, previous) != activeNow || item.isPrimaryActive(previous) != item.isPrimaryActive(position) || item.isSecondaryActive(previous) != item.isSecondaryActive(position)) notifyItemChanged(i, Boolean.TRUE);
        }
    }

    private void notifyDanmakuSecond(long second) {
        int low = 0;
        int high = mItems.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (mItems.get(middle).startTimeSeconds() < second) low = middle + 1;
            else high = middle;
        }
        int end = low;
        while (end < mItems.size() && mItems.get(end).startTimeSeconds() == second) end++;
        if (end > low) notifyItemRangeChanged(low, end - low, Boolean.TRUE);
    }

    public int getSelected() {
        if (active != RecyclerView.NO_POSITION) return active;
        int low = 0;
        int high = mItems.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (mItems.get(middle).getPosition() <= position) low = middle + 1;
            else high = middle;
        }
        return Math.max(0, low - 1);
    }

    private boolean isActive(PlaybackContent item, long position) {
        return item.danmaku() ? position / 1000 == item.startTimeSeconds() : item.isActive(position);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterPlaybackContentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaybackContent item = mItems.get(position);
        String primary = item.primary();
        String secondary = item.secondary();
        holder.binding.time.setText(item.getTime());
        ViewGroup.LayoutParams params = holder.binding.time.getLayoutParams();
        int width = Math.max(ResUtil.dp2px(40), (int) Math.ceil(holder.binding.time.getPaint().measureText(timeWidthSample)));
        if (params.width != width) {
            params.width = width;
            holder.binding.time.setLayoutParams(params);
        }
        holder.binding.primary.setText(highlight(primary, holder.itemView));
        holder.binding.secondary.setText(highlight(secondary, holder.itemView));
        holder.binding.primaryRow.setVisibility(primary.isEmpty() ? View.GONE : View.VISIBLE);
        holder.binding.secondaryRow.setVisibility(secondary.isEmpty() ? View.GONE : View.VISIBLE);
        holder.binding.primaryRole.setVisibility(item.danmaku() ? View.GONE : View.VISIBLE);
        holder.binding.getRoot().setGravity(item.danmaku() ? Gravity.CENTER_VERTICAL : Gravity.TOP);
        ViewGroup.MarginLayoutParams secondaryParams = (ViewGroup.MarginLayoutParams) holder.binding.secondaryRow.getLayoutParams();
        int margin = primary.isEmpty() ? 0 : ResUtil.dp2px(4);
        if (secondaryParams.topMargin != margin) {
            secondaryParams.topMargin = margin;
            holder.binding.secondaryRow.setLayoutParams(secondaryParams);
        }
        holder.primaryDescription = primary.isEmpty() ? "" : (item.danmaku() ? item.getTimeRange() : ResUtil.getString(R.string.subtitle_primary_role) + ": " + item.getPrimaryTimeRange()) + ", " + primary;
        holder.secondaryDescription = secondary.isEmpty() ? "" : ResUtil.getString(R.string.subtitle_secondary_role) + ": " + item.getSecondaryTimeRange() + ", " + secondary;
        bindState(holder, item);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.stream().allMatch(Boolean.TRUE::equals)) bindState(holder, mItems.get(position));
        else onBindViewHolder(holder, position);
    }

    private void bindState(ViewHolder holder, PlaybackContent item) {
        boolean selected = isActive(item, position);
        boolean primaryActive = item.danmaku() ? selected : item.isPrimaryActive(position);
        boolean secondaryActive = item.isSecondaryActive(position);
        holder.itemView.setSelected(selected);
        holder.binding.primaryRow.setSelected(primaryActive);
        holder.binding.secondaryRow.setSelected(secondaryActive);
        holder.binding.playing.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        String current = ResUtil.getString(R.string.content_current) + ", ";
        String description = (primaryActive ? current : "") + holder.primaryDescription;
        if (!holder.secondaryDescription.isEmpty()) description += (description.isEmpty() ? "" : ", ") + (secondaryActive ? current : "") + holder.secondaryDescription;
        holder.itemView.setContentDescription(description);
    }

    private CharSequence highlight(String text, View view) {
        if (keyword.isEmpty()) return text;
        int start = PlaybackContent.indexOf(text, keyword, 0);
        if (start < 0) return text;
        SpannableString result = new SpannableString(text);
        int background = MaterialColors.getColor(view, com.google.android.material.R.attr.colorSecondaryContainer);
        int foreground = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnSecondaryContainer);
        do {
            int end = start + keyword.length();
            result.setSpan(new BackgroundColorSpan(background), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            result.setSpan(new ForegroundColorSpan(foreground), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = PlaybackContent.indexOf(text, keyword, end);
        } while (start >= 0);
        return result;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final AdapterPlaybackContentBinding binding;
        private String primaryDescription;
        private String secondaryDescription;

        public ViewHolder(@NonNull AdapterPlaybackContentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) listener.onItemClick(mItems.get(position));
        }
    }
}
