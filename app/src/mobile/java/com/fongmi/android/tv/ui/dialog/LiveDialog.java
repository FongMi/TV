package com.fongmi.android.tv.ui.dialog;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.databinding.DialogLiveBinding;
import com.fongmi.android.tv.impl.LiveListener;
import com.fongmi.android.tv.ui.adapter.LiveAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class LiveDialog extends BaseAlertDialog implements LiveAdapter.OnClickListener {

    private DialogLiveBinding binding;
    private LiveListener listener;
    private LiveAdapter adapter;

    public static void show(FragmentActivity activity) {
        new LiveDialog().show(activity.getSupportFragmentManager(), null);
    }

    public static void show(Fragment fragment) {
        new LiveDialog().show(fragment.getChildFragmentManager(), null);
    }

    private boolean isFull() {
        return getParentFragment() == null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        listener = isFull() ? (LiveListener) context : (LiveListener) getParentFragment();
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogLiveBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setView(getBinding().getRoot());
    }

    @Override
    protected void initView() {
        adapter = new LiveAdapter(this);
        adapter.setAction(!isFull());
        binding.recycler.setAdapter(adapter);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        if (isFull()) binding.recycler.setMaxHeight(ResUtil.dp2px(264));
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 8));
        binding.recycler.post(() -> binding.recycler.scrollToPosition(LiveConfig.getHomeIndex()));
    }

    @Override
    public void onItemClick(Live item) {
        listener.setLive(item);
        dismiss();
    }

    @Override
    public void onBootClick(int position, Live item) {
        item.boot(!item.isBoot()).saveSettings();
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onPassClick(int position, Live item) {
        item.pass(!item.isPass()).saveSettings();
        adapter.notifyItemChanged(position);
    }

    @Override
    public boolean onBootLongClick(Live item) {
        boolean result = !item.isBoot();
        List<Live> lives = LiveConfig.get().getLives();
        lives.forEach(live -> live.boot(result));
        Live.saveSettings(lives);
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        Notify.show(getString(R.string.dialog_action_bulk_result, getString(R.string.dialog_action_live_boot), getString(result ? R.string.setting_on : R.string.setting_off)));
        return true;
    }

    @Override
    public boolean onPassLongClick(Live item) {
        boolean result = !item.isPass();
        List<Live> lives = LiveConfig.get().getLives();
        lives.forEach(live -> live.pass(result));
        Live.saveSettings(lives);
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        Notify.show(getString(R.string.dialog_action_bulk_result, getString(R.string.dialog_action_live_password), getString(result ? R.string.setting_off : R.string.setting_on)));
        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter.getItemCount() == 0) dismiss();
        else if (ResUtil.isLand(requireContext())) setWidth(0.5f);
    }
}
