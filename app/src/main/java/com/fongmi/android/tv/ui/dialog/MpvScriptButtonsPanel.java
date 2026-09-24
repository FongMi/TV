package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.activity.ComponentDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.view.OneShotPreDrawListener;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogMpvScriptButtonBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.mpv.MpvScriptSession;
import com.fongmi.android.tv.player.mpv.MpvScripts;
import com.fongmi.android.tv.ui.activity.PlaybackActivity;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.Task;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

final class MpvScriptButtonsPanel {

    private static final int LIST = 0, EDIT = 1, RENAME = 2, ADD = 3, COMMAND = 4, STATUS = 5;
    private final DialogFragment owner;
    private final ActivityResultLauncher<Intent> picker;
    private DialogMpvScriptButtonBinding binding;
    private SpaceItemDecoration itemDecoration;
    private ItemAdapter adapter;
    private Operations operations;
    private List<MpvScripts.Item> items = new ArrayList<>();
    private MpvScripts.Item selected;
    private boolean automatic;
    private String replaceId;
    private int page;
    private int itemSpacing = -1;
    private int listPosition;
    private int editPosition;
    private final List<Integer> commands = new ArrayList<>();
    private List<String> scriptActions = new ArrayList<>();
    private final OnBackPressedCallback backCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            back();
        }
    };
    private final Runnable refreshStatus = new Runnable() {
        @Override
        public void run() {
            if (binding == null) return;
            renderStatus();
            binding.status.postDelayed(this, 500);
        }
    };

    MpvScriptButtonsPanel(DialogFragment owner) {
        this.owner = owner;
        picker = owner.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) return;
            boolean mode = automatic;
            String target = replaceId;
            work(() -> MpvScripts.importFrom(result.getData().getData(), mode, target).id, true, mode, mode ? null : target);
        });
    }

    void bind(DialogMpvScriptButtonBinding binding, Bundle savedInstanceState) {
        this.binding = binding;
        operations = new ViewModelProvider(owner).get(Operations.class);
        adapter = new ItemAdapter();
        binding.items.setAdapter(adapter);
        binding.items.setItemAnimator(null);
        itemDecoration = null;
        itemSpacing = -1;
        binding.title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MpvScripts.MAX_TITLE_LENGTH)});
        binding.command.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MpvScripts.MAX_COMMAND_LENGTH)});
        Bundle state = savedInstanceState == null ? owner.requireArguments() : savedInstanceState;
        page = state.getInt("page", LIST);
        automatic = state.getBoolean("automatic");
        replaceId = state.getString("replace");
        listPosition = state.getInt("listPosition");
        editPosition = state.getInt("editPosition");
        try {
            String id = state.getString("selected");
            for (MpvScripts.Item item : MpvScripts.read()) if (item.id.equals(id)) selected = item;
            if (selected != null && savedInstanceState == null) {
                automatic = selected.automatic;
                page = EDIT;
            }
        } catch (JSONException e) {
            Notify.show(Notify.getError(R.string.mpv_script_error, e));
        }
        if (selected == null && (page == EDIT || page == RENAME || page == STATUS)) page = LIST;
        binding.title.setText(state.getString("draft", selected == null ? "" : selected.title));
        binding.command.setText(state.getString("command", ""));
        operations.result.observe(owner.getViewLifecycleOwner(), result -> {
            if (result == null) return;
            operations.result.setValue(null);
            if (result.error != null) Notify.show(Notify.getError(R.string.mpv_script_error, result.error));
            else {
                page = LIST;
                if (result.id != null) {
                    try {
                        for (MpvScripts.Item item : MpvScripts.read()) if (item.id.equals(result.id)) selected = item;
                    } catch (JSONException e) {
                        Notify.show(Notify.getError(R.string.mpv_script_error, e));
                    }
                }
                if (result.applyError != null) Notify.show(Notify.getError(R.string.mpv_script_error, result.applyError));
            }
            render(true);
        });
        bindTabs();
        binding.primary.setOnClickListener(this::onPrimary);
        binding.title.setOnEditorActionListener(this::onEditorAction);
        binding.command.setOnEditorActionListener(this::onEditorAction);
        if (owner.requireDialog() instanceof ComponentDialog dialog) dialog.getOnBackPressedDispatcher().addCallback(owner.getViewLifecycleOwner(), backCallback);
        render(!isRoot());
        if (Util.isLeanback() && isRoot()) PlaybackDialogFocus.selectFirstTab(binding.getRoot(), binding.tabGroup, binding.tabButtons);
    }

    void start() {
        binding.status.post(refreshStatus);
    }

    void stop() {
        if (binding != null) binding.status.removeCallbacks(refreshStatus);
    }

    private void onPrimary(View view) {
        if (operations.running) return;
        if (page == RENAME) {
            String title = binding.title.getText().toString();
            MpvScripts.Item item = selected;
            work(() -> { MpvScripts.rename(item.id, title); return item.id; }, false, false);
        } else if (page == COMMAND) {
            String title = binding.title.getText().toString();
            String command = binding.command.getText().toString();
            MpvScripts.Item item = selected;
            if (item == null) work(() -> MpvScripts.addCommand(title, command).id, false, false);
            else work(() -> { MpvScripts.updateCommand(item.id, title, command); return item.id; }, true, false);
        } else if (automatic) {
            pick(null);
        } else {
            selected = null;
            page = ADD;
            render(true);
        }
    }

    private boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        if (actionId != EditorInfo.IME_ACTION_DONE || (page != RENAME && page != COMMAND)) return false;
        onPrimary(view);
        return true;
    }

    private boolean isRoot() {
        return page == LIST;
    }

    private MaterialButton selectedTab() {
        return automatic ? binding.tabAutomatic : binding.tabButtons;
    }

    private void bindTabs() {
        MaterialButton[] tabs = {binding.tabButtons, binding.tabAutomatic};
        for (MaterialButton tab : tabs) {
            if (Util.isLeanback()) tab.setOnFocusChangeListener((view, focused) -> {
                if (focused) binding.tabGroup.check(tab.getId());
            });
        }
        binding.tabGroup.addOnButtonCheckedListener((group, id, checked) -> {
            if (!checked || operations.running || selectedTab().getId() == id) return;
            page = LIST;
            automatic = id == binding.tabAutomatic.getId();
            selected = null;
            listPosition = 0;
            render(false);
        });
    }

    private void back() {
        if (operations.running || isRoot()) return;
        page = switch (page) {
            case RENAME, STATUS -> EDIT;
            case COMMAND -> selected == null ? ADD : EDIT;
            default -> LIST;
        };
        render(true);
    }

    private PlayerManager player() {
        return owner.getActivity() instanceof PlaybackActivity activity ? activity.getPlaybackPlayer() : null;
    }

    private String getString(int id) {
        return owner.getString(id);
    }

    private void render(boolean focusContent) {
        if (binding == null || !owner.isAdded()) return;
        binding.heading.setText(switch (page) {
            case EDIT, RENAME, STATUS -> selected.title;
            case COMMAND -> selected == null ? getString(R.string.mpv_script_add_command) : selected.title;
            case ADD -> getString(R.string.mpv_script_add_button);
            default -> getString(R.string.mpv_script_buttons);
        });
        binding.tabGroup.setVisibility(isRoot() ? View.VISIBLE : View.GONE);
        if (isRoot()) binding.tabGroup.check(selectedTab().getId());
        boolean showPrimary = page == LIST || page == RENAME || page == COMMAND;
        binding.primary.setVisibility(showPrimary ? View.VISIBLE : View.INVISIBLE);
        binding.primary.setNextFocusDownId(isRoot() ? selectedTab().getId() : editingPage() ? R.id.title : View.NO_ID);
        binding.primary.setText(switch (page) {
            case RENAME, COMMAND -> R.string.dialog_positive;
            case LIST -> automatic ? R.string.mpv_script_import : R.string.mpv_script_add;
            default -> R.string.mpv_script_add;
        });
        updateBusy();
        renderContent(focusContent);
    }

    private void renderContent(boolean focusContent) {
        updateItemSpacing();
        int topPadding = page == EDIT && !Util.isLeanback() ? 0 : owner.getResources().getDimensionPixelSize(R.dimen.playback_dialog_spacing);
        binding.items.setPaddingRelative(binding.items.getPaddingStart(), topPadding, binding.items.getPaddingEnd(), binding.items.getPaddingBottom());
        boolean editing = editingPage();
        if (!editing && binding.form.getVisibility() == View.VISIBLE) Util.hideKeyboard(binding.title);
        binding.form.setVisibility(editing ? View.VISIBLE : View.GONE);
        binding.commandInput.setVisibility(page == COMMAND ? View.VISIBLE : View.GONE);
        binding.statusScroll.setVisibility(page == STATUS ? View.VISIBLE : View.GONE);
        binding.items.setVisibility(editing || page == STATUS ? View.GONE : View.VISIBLE);
        List<Row> rows = new ArrayList<>();
        commands.clear();
        if (page == LIST) {
            try {
                items = new ArrayList<>();
                for (MpvScripts.Item item : MpvScripts.read()) if (item.automatic == automatic) items.add(item);
                PlayerManager player = player();
                for (int i = 0; i < items.size(); i++) {
                    MpvScripts.Item item = items.get(i);
                    if (selected != null && item.id.equals(selected.id)) listPosition = i;
                    String suffix = getString(item.enabled ? R.string.mpv_script_enabled : R.string.mpv_script_disabled);
                    if (!automatic && item.hidden) suffix += " · " + getString(R.string.mpv_script_hidden);
                    if (item.isCommand()) suffix += " · " + getString(R.string.mpv_script_command);
                    MpvScriptSession.Status status = automatic && player != null ? player.getMpvScriptStatus(item.id) : null;
                    if (status != null && status.state() == MpvScriptSession.State.ERROR) suffix += " · " + getString(R.string.mpv_script_failed);
                    rows.add(new Row(item.title, suffix));
                }
            } catch (JSONException e) {
                Notify.show(Notify.getError(R.string.mpv_script_error, e));
            }
        } else if (page == EDIT) {
            commands.add(R.string.mpv_script_status);
            if (selected.isCommand()) commands.add(R.string.mpv_script_edit_command);
            else {
                commands.add(R.string.mpv_script_rename);
                commands.add(R.string.mpv_script_replace);
            }
            if (!automatic) addButtonCommands();
            commands.add(selected.enabled ? R.string.mpv_script_disable : R.string.mpv_script_enable);
            commands.add(R.string.mpv_script_delete);
            for (int command : commands) rows.add(new Row(getString(command), ""));
        } else if (page == ADD) {
            rows.add(new Row(getString(R.string.mpv_script_import), getString(R.string.mpv_script_import_detail)));
            rows.add(new Row(getString(R.string.mpv_script_command), getString(R.string.mpv_script_command_detail)));
            try {
                PlayerManager player = player();
                scriptActions = player == null ? new ArrayList<>() : player.getMpvScriptBindings();
                for (String action : scriptActions) rows.add(new Row(action, getString(R.string.mpv_script_action)));
            } catch (JSONException e) {
                Notify.show(Notify.getError(R.string.mpv_script_error, e));
            }
        }
        binding.empty.setVisibility(isRoot() && rows.isEmpty() ? View.VISIBLE : View.GONE);
        binding.empty.setText(automatic ? R.string.mpv_script_auto_empty : R.string.mpv_script_empty);
        adapter.replaceRows(rows);
        if (editing) {
            int imeAction = page == COMMAND ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE;
            binding.title.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN | imeAction);
            binding.title.requestFocus();
            binding.title.getContext().getSystemService(InputMethodManager.class).restartInput(binding.title);
        } else if (focusContent && Util.isLeanback()) {
            if (page == STATUS) binding.statusScroll.requestFocus();
            else if (rows.isEmpty()) binding.primary.requestFocus();
            else {
                int position = Math.min(rows.size() - 1, page == LIST ? listPosition : page == EDIT ? editPosition : 0);
                binding.items.getLayoutManager().scrollToPosition(position);
                OneShotPreDrawListener.add(binding.items, () -> {
                    if (this.binding != null) {
                        View view = binding.items.getLayoutManager().findViewByPosition(position);
                        if (view != null) view.requestFocus();
                    }
                });
            }
        }
        renderStatus();
    }

    private void updateItemSpacing() {
        int spacing = owner.getResources().getInteger(page == EDIT ? R.integer.mpv_script_action_spacing : R.integer.mpv_script_item_spacing);
        if (itemSpacing == spacing) return;
        if (itemDecoration != null) binding.items.removeItemDecoration(itemDecoration);
        itemSpacing = spacing;
        itemDecoration = spacing == 0 ? null : new SpaceItemDecoration(1, spacing);
        if (itemDecoration != null) binding.items.addItemDecoration(itemDecoration);
    }

    private boolean editingPage() {
        return page == RENAME || page == COMMAND;
    }

    private void addButtonCommands() {
        commands.add(selected.hidden ? R.string.mpv_script_show : R.string.mpv_script_hide);
        try {
            List<MpvScripts.Item> buttons = new ArrayList<>();
            for (MpvScripts.Item item : MpvScripts.read()) if (!item.automatic) buttons.add(item);
            for (int i = 0; i < buttons.size(); i++) {
                if (!buttons.get(i).id.equals(selected.id)) continue;
                if (i > 0) commands.add(R.string.mpv_script_move_earlier);
                if (i + 1 < buttons.size()) commands.add(R.string.mpv_script_move_later);
                break;
            }
        } catch (JSONException e) {
            Notify.show(Notify.getError(R.string.mpv_script_error, e));
        }
    }

    private void renderStatus() {
        if (page != STATUS) return;
        PlayerManager player = player();
        MpvScriptSession.Status status = selected == null || player == null ? null : player.getMpvScriptStatus(selected.id);
        String state = status == null ? getString(R.string.mpv_script_no_status) : getString(switch (status.state()) {
            case RUNNING -> R.string.mpv_script_running;
            case DONE -> R.string.mpv_script_done;
            case ERROR -> R.string.mpv_script_failed;
            case TIMEOUT -> R.string.mpv_script_timeout;
            case LOADING -> R.string.mpv_script_loading;
            case LOADED -> R.string.mpv_script_loaded;
        });
        String error = status == null ? "" : status.error();
        binding.status.setText(error.isEmpty() ? state : state + "\n\n" + error);
    }

    private void editCommand(String title, String command) {
        binding.title.setText(title);
        binding.command.setText(command);
        page = COMMAND;
        render(true);
    }

    private void select(int position) {
        if (operations.running || position == RecyclerView.NO_POSITION) return;
        if (page == LIST) {
            listPosition = position;
            editPosition = 0;
            selected = items.get(position);
            page = EDIT;
            render(true);
        } else if (page == EDIT) {
            editPosition = position;
            MpvScripts.Item item = selected;
            int command = commands.get(position);
            if (command == R.string.mpv_script_edit_command) {
                editCommand(item.title, item.command);
            } else if (command == R.string.mpv_script_rename) {
                page = RENAME;
                binding.title.setText(item.title);
                render(true);
            } else if (command == R.string.mpv_script_replace) pick(item.id);
            else if (command == R.string.mpv_script_delete) work(() -> { MpvScripts.delete(item.id); return null; }, true, item.automatic);
            else if (command == R.string.mpv_script_status) { page = STATUS; render(true); }
            else if (command == R.string.mpv_script_hide || command == R.string.mpv_script_show) work(() -> { MpvScripts.setHidden(item.id, !item.hidden); return item.id; }, false, false);
            else if (command == R.string.mpv_script_move_earlier || command == R.string.mpv_script_move_later) work(() -> { MpvScripts.move(item.id, command == R.string.mpv_script_move_earlier); return item.id; }, false, false);
            else work(() -> { MpvScripts.setEnabled(item.id, !item.enabled); return item.id; }, true, item.automatic);
        } else if (page == ADD) {
            if (position == 0) pick(null);
            else if (position == 1) editCommand("", "");
            else {
                String action = scriptActions.get(position - 2);
                editCommand(action.substring(action.indexOf('/') + 1), "script-binding " + action);
            }
        }
    }

    private void pick(String id) {
        PermissionUtil.requestFile(owner, granted -> {
            if (!granted) return;
            replaceId = id;
            FileChooser.from(picker).show();
        });
    }

    private record Row(String title, String detail) {
    }

    private final class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.Holder> {
        private List<Row> rows = new ArrayList<>();

        private void replaceRows(List<Row> rows) {
            int previousCount = this.rows.size();
            if (previousCount > 0) {
                this.rows = new ArrayList<>();
                notifyItemRangeRemoved(0, previousCount);
            }
            this.rows = rows;
            if (!rows.isEmpty()) notifyItemRangeInserted(0, rows.size());
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false));
        }

        @Override
        public int getItemViewType(int position) {
            return page == EDIT ? R.layout.adapter_mpv_script_action : R.layout.adapter_mpv_script;
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Row row = rows.get(position);
            holder.text.setText(row.title());
            if (holder.detail != null) {
                holder.detail.setText(row.detail());
                holder.detail.setVisibility(row.detail().isEmpty() ? View.GONE : View.VISIBLE);
            }
            holder.itemView.setEnabled(!operations.running);
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        final class Holder extends RecyclerView.ViewHolder {
            private final TextView text;
            private final TextView detail;

            Holder(View itemView) {
                super(itemView);
                text = itemView.findViewById(R.id.text);
                detail = itemView.findViewById(R.id.detail);
                itemView.setOnClickListener(view -> select(getBindingAdapterPosition()));
            }
        }
    }

    private void work(Operation operation, boolean syncScripts, boolean reloadStartupScripts) {
        work(operation, syncScripts, reloadStartupScripts, null);
    }

    private void work(Operation operation, boolean syncScripts, boolean reloadStartupScripts, String reloadButtonId) {
        PlayerManager player = syncScripts ? player() : null;
        operations.run(operation, player == null ? null : () -> player.reloadMpvScripts(reloadStartupScripts, reloadButtonId));
        updateBusy();
    }

    private void updateBusy() {
        if (binding == null) return;
        backCallback.setEnabled(operations.running || !isRoot());
        binding.primary.setEnabled(!operations.running);
        for (int i = 0; i < binding.tabGroup.getChildCount(); i++) binding.tabGroup.getChildAt(i).setEnabled(!operations.running);
        binding.items.setEnabled(!operations.running);
        for (int i = 0; i < binding.items.getChildCount(); i++) binding.items.getChildAt(i).setEnabled(!operations.running);
        binding.title.setEnabled(!operations.running);
        binding.command.setEnabled(!operations.running);
    }

    void save(Bundle state) {
        state.putInt("page", page);
        state.putBoolean("automatic", automatic);
        state.putString("replace", replaceId);
        state.putString("selected", selected == null ? null : selected.id);
        state.putInt("listPosition", listPosition);
        state.putInt("editPosition", editPosition);
        if (binding != null) {
            state.putString("draft", binding.title.getText().toString());
            state.putString("command", binding.command.getText().toString());
        }
    }

    void release() {
        stop();
        backCallback.setEnabled(false);
        binding.tabGroup.clearOnButtonCheckedListeners();
        binding.items.setAdapter(null);
        binding = null;
        itemDecoration = null;
        itemSpacing = -1;
        adapter = null;
    }

    public static final class Operations extends ViewModel {
        private final MutableLiveData<Result> result = new MutableLiveData<>();
        private boolean running;

        private void run(Operation operation, Runnable onSuccess) {
            if (running) return;
            running = true;
            Task.execute(() -> {
                Exception error = null;
                String id = null;
                try {
                    id = operation.run();
                } catch (Exception e) {
                    error = e;
                }
                Result completed = new Result(error, null, id);
                App.post(() -> {
                    running = false;
                    RuntimeException applyError = null;
                    try {
                        if (completed.error == null && onSuccess != null) onSuccess.run();
                    } catch (RuntimeException e) {
                        applyError = e;
                    }
                    result.setValue(new Result(completed.error, applyError, completed.id));
                });
            });
        }
    }

    private record Result(Exception error, RuntimeException applyError, String id) {
    }

    private interface Operation {
        String run() throws Exception;
    }
}
