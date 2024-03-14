package com.fongmi.android.tv.ui.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Button;
import com.fongmi.android.tv.databinding.AdapterButtonsBinding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ButtonsAdapter extends RecyclerView.Adapter<ButtonsAdapter.ViewHolder> {

    private List<Button> mItems;
    private int upFocus;
    private int downFocus;

    public ButtonsAdapter() {
        this.mItems = Button.sortedAll();
        this.upFocus = -1;
        this.downFocus = -1;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterButtonsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Button item = mItems.get(position);
        holder.binding.text.setText(item.getName());
        holder.binding.check.setChecked(getChecked(item));
        holder.binding.select.setOnLongClickListener(v -> onItemLongClick(item));
        holder.binding.select.setOnClickListener(v -> onItemClick(item, position));
        holder.binding.text.setGravity(Gravity.START);
        holder.binding.down.setOnClickListener(v -> onDownClick(item, position));
        holder.binding.up.setOnClickListener(v -> onUpClick(item, position));
        if (upFocus == position) holder.binding.up.requestFocus();
        else if (downFocus == position) holder.binding.down.requestFocus();
    }

    private boolean getChecked(Button item) {
        Map<Integer, Button> map = Button.getButtonsMap();
        if (map.containsKey(item.getId())) return true;
        return false;
    }

    private void onItemClick(Button item, int position) {
        boolean checked = getChecked(item);
        Map<Integer, Button> map = Button.getButtonsMap();
        if (checked) map.remove(item.getId());
        else map.put(item.getId(), item);
        save(mItems, map);
        notifyItemRangeChanged(0, getItemCount());
    }

    private boolean onItemLongClick(Button item) {
        boolean checked = getChecked(item);
        Map<Integer, Button> map = new LinkedHashMap<>();
        if (!checked) map = Button.getMap(mItems);
        Button.save(map);
        notifyItemRangeChanged(0, getItemCount());
        return true;
    }

    private void onDownClick(Button item, int position) {
        if (position == getItemCount() - 1) return;
        List<Button> buttonList = Button.sortedAll();
        Button button = buttonList.get(position);
        buttonList.remove(position);
        buttonList.add(position + 1, button);
        Map<Integer, Button> map = Button.getMap(buttonList);
        Button.saveSorted(map);
        mItems = Button.sortedAll();
        save(mItems, Button.getButtonsMap());
        downFocus = position + 1;
        upFocus = -1;
        notifyItemRangeChanged(0, getItemCount());
    }

    private void onUpClick(Button item, int position) {
        if (position == 0) return;
        List<Button> buttonList = Button.sortedAll();
        Button button = buttonList.get(position);
        buttonList.remove(position);
        buttonList.add(position - 1, button);
        Map<Integer, Button> map = Button.getMap(buttonList);
        Button.saveSorted(map);
        mItems = Button.sortedAll();
        save(mItems, Button.getButtonsMap());
        upFocus = position - 1;
        downFocus = -1;
        notifyItemRangeChanged(0, getItemCount());
    }

    private void save(List<Button> sortedItems, Map<Integer, Button> btnsMap) {
        List<Button> btns = new ArrayList<>();
        for(int i=0; i<sortedItems.size(); i++) {
            if (btnsMap.containsKey(sortedItems.get(i).getId())) btns.add(sortedItems.get(i));
        }
        Button.save(Button.getMap(btns));
    }


    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterButtonsBinding binding;

        ViewHolder(@NonNull AdapterButtonsBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

}
