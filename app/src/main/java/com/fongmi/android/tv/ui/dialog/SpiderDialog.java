package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.QRCode;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.Runtime;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class SpiderDialog implements LifecycleEventObserver {

    private static final int[] BUTTONS = {DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE, DialogInterface.BUTTON_NEUTRAL};

    private final CompletableFuture<String> result = new CompletableFuture<>();
    private final Map<String, Input> inputs = new LinkedHashMap<>();
    private final Map<Integer, ButtonOptions> buttons = new LinkedHashMap<>();
    private final Function<String, CompletableFuture<String>> onAction;
    private CompletableFuture<String> action;
    private FragmentActivity activity;
    private AlertDialog dialog;
    private AlertDialog picker;
    private TextView message;
    private Button copy;
    private ImageView qrcode;
    private ImageView image;
    private RequestManager images;

    public static CompletableFuture<String> show(String options) {
        return show(options, null);
    }

    public static CompletableFuture<String> show(String options, Function<String, CompletableFuture<String>> onAction) {
        SpiderDialog request = new SpiderDialog(onAction);
        CompletableFuture<String> completion = request.result.whenCompleteAsync((value, error) -> request.close(), App::post);
        completion.whenComplete((value, error) -> {
            if (completion.isCancelled()) request.cancel();
        });
        App.post(() -> request.showDialog(options));
        return completion;
    }

    private SpiderDialog(Function<String, CompletableFuture<String>> onAction) {
        this.onAction = onAction;
    }

    private void showDialog(String options) {
        if (result.isDone()) return;
        try {
            Activity current = Runtime.activity();
            if (!(current instanceof FragmentActivity owner)) throw new IllegalStateException("No Activity available for dialog");
            activity = owner;
            JSONObject json = new JSONObject(options);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity).setTitle(json.optString("title"));
            bindContent(builder, json);
            bindButtons(builder, json.has("buttons") ? json.getJSONArray("buttons") : null);
            dialog = builder.create();
            dialog.setCancelable(flag(json, "cancelable", true));
            dialog.setOnDismissListener(this::onDismiss);
            activity.getLifecycle().addObserver(this);
            if (!result.isDone()) showDialog();
        } catch (JSONException | RuntimeException error) {
            result.completeExceptionally(error);
        }
    }

    private void showDialog() {
        dialog.show();
        for (Map.Entry<Integer, ButtonOptions> button : buttons.entrySet()) dialog.getButton(button.getKey()).setOnClickListener(view -> onButton(button.getKey(), button.getValue()));
        setEnabled(true);
        dialog.getWindow().getDecorView().post(this::focusInput);
    }

    private void bindContent(MaterialAlertDialogBuilder builder, JSONObject options) throws JSONException {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(ResUtil.dp2px(24), ResUtil.dp2px(8), ResUtil.dp2px(24), ResUtil.dp2px(8));
        bindMessage(content, flag(options, "copyable", false));
        setMessage(options.optString("message"));
        qrcode = addImage(content);
        image = addImage(content);
        image.setContentDescription(activity.getString(R.string.dialog_image));
        if (options.has("qrcode")) setQRCode(options.get("qrcode"));
        if (options.has("image")) setImage(options.get("image"));
        bindInputs(content, options.has("inputs") ? options.getJSONArray("inputs") : null);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(content);
        builder.setView(scroll);
    }

    private void bindMessage(LinearLayout content, boolean copyable) {
        message = new MaterialTextView(activity);
        message.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        message.setTextIsSelectable(copyable);
        message.setOnKeyListener(this::onFocusKey);
        message.setPadding(0, 0, 0, ResUtil.dp2px(8));
        content.addView(message, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (!copyable) return;
        copy = new MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        copy.setText(android.R.string.copy);
        copy.setOnClickListener(this::copyMessage);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.END;
        content.addView(copy, params);
    }

    private boolean onFocusKey(View view, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN || !event.hasNoModifiers() || keyCode != KeyEvent.KEYCODE_DPAD_UP && keyCode != KeyEvent.KEYCODE_DPAD_DOWN) return false;
        if (view instanceof MaterialAutoCompleteTextView select && select.isPopupShowing() || canMoveCursor(view, keyCode)) return false;
        View next = view.focusSearch(keyCode == KeyEvent.KEYCODE_DPAD_UP ? View.FOCUS_UP : View.FOCUS_DOWN);
        return next != null && next.requestFocus();
    }

    private boolean canMoveCursor(View view, int keyCode) {
        if (!(view instanceof EditText input) || input.getKeyListener() == null || input.getLayout() == null || input.getSelectionEnd() < 0) return false;
        int line = input.getLayout().getLineForOffset(input.getSelectionEnd());
        return keyCode == KeyEvent.KEYCODE_DPAD_UP ? line > 0 : line < input.getLayout().getLineCount() - 1;
    }

    private void setMessage(String text) {
        message.setText(text);
        message.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
        if (copy != null) copy.setVisibility(message.getVisibility());
    }

    private void copyMessage(View view) {
        Util.copy(message.getText().toString());
    }

    private ImageView addImage(LinearLayout content) {
        ImageView view = new ImageView(activity);
        view.setVisibility(View.GONE);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ResUtil.dp2px(220));
        params.topMargin = params.bottomMargin = ResUtil.dp2px(8);
        content.addView(view, params);
        return view;
    }

    private void setQRCode(Object code) {
        if (!(code instanceof String value) || value.isBlank()) throw new IllegalArgumentException("Dialog qrcode must be a nonempty string");
        Bitmap bitmap = QRCode.getBitmap(value, 220, 4);
        if (bitmap == null) throw new IllegalArgumentException("Dialog qrcode cannot be encoded");
        qrcode.setImageBitmap(bitmap);
        qrcode.setContentDescription(value);
        qrcode.setVisibility(View.VISIBLE);
    }

    private void setImage(Object source) {
        if (!(source instanceof String value)) throw new IllegalArgumentException("Dialog image must be a string");
        if (value.isEmpty()) {
            if (images != null) images.clear(image);
            image.setImageDrawable(null);
            image.setVisibility(View.GONE);
            return;
        }
        if (!value.startsWith("data:image/") || !value.contains(";base64,")) throw new IllegalArgumentException("Dialog image requires a base64 image data URI");
        if (images == null) images = Glide.with(activity);
        image.setVisibility(View.VISIBLE);
        images.asBitmap().load(value).override(ResUtil.dp2px(220)).fitCenter().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).listener(imageListener()).into(image);
    }

    private RequestListener<Bitmap> imageListener() {
        return new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException error, Object model, @NonNull Target<Bitmap> target, boolean first) {
                result.completeExceptionally(new IllegalArgumentException("Dialog image could not be decoded", error));
                return true;
            }

            @Override
            public boolean onResourceReady(Bitmap bitmap, Object model, Target<Bitmap> target, DataSource source, boolean first) {
                return result.isDone();
            }
        };
    }

    private void bindInputs(LinearLayout content, JSONArray fields) throws JSONException {
        if (fields == null) return;
        for (int i = 0; i < fields.length(); i++) addInput(content, fields.getJSONObject(i));
    }

    private void addInput(LinearLayout content, JSONObject field) throws JSONException {
        String name = field.getString("name");
        if (name.isBlank() || inputs.containsKey(name)) throw new IllegalArgumentException("Dialog input names must be nonempty and unique");
        TextInputLayout layout = new TextInputLayout(activity);
        layout.setHint(field.optString("label", name));
        layout.setHelperText(field.optString("helperText"));
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        String type = field.optString("type", "text");
        boolean multiple = type.equals("multiselect");
        EditText input = createInput(layout, type);
        input.setOnKeyListener(this::onFocusKey);
        layout.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = ResUtil.dp2px(8);
        content.addView(layout, params);
        int min = inputLimit(field, multiple ? "minItems" : "minLength", 0);
        int max = inputLimit(field, multiple ? "maxItems" : "maxLength", Integer.MAX_VALUE);
        if (max < min) throw new IllegalArgumentException("Invalid dialog input limits");
        List<Choice> choices = multiple || input instanceof MaterialAutoCompleteTextView ? readChoices(field.getJSONArray("options")) : List.of();
        Input control = new Input(layout, input, flag(field, "required", false), min, max, choices, multiple, flag(field, "enabled", true));
        if (input instanceof MaterialAutoCompleteTextView) bindSelect(control);
        if (multiple) bindMultiSelect(control);
        Object value = multiple ? new JSONArray() : "";
        if (field.has("value")) value = field.get("value");
        control.checkValue(value);
        control.setValue(value);
        inputs.put(name, control);
    }

    private EditText createInput(TextInputLayout layout, String type) {
        if (type.equals("select")) {
            layout.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
            MaterialAutoCompleteTextView input = new MaterialAutoCompleteTextView(layout.getContext());
            input.setInputType(InputType.TYPE_NULL);
            input.setKeyListener(null);
            return input;
        }
        TextInputEditText input = new TextInputEditText(layout.getContext());
        if (type.equals("multiselect")) {
            input.setInputType(InputType.TYPE_NULL);
            input.setKeyListener(null);
            input.setCursorVisible(false);
            input.setFocusable(true);
            input.setFocusableInTouchMode(false);
            return input;
        }
        input.setInputType(inputType(type));
        input.setSingleLine(!type.equals("textarea"));
        if (type.equals("textarea")) {
            input.setMinLines(3);
            input.setGravity(Gravity.TOP | Gravity.START);
        }
        return input;
    }

    private void bindSelect(Input control) {
        MaterialAutoCompleteTextView input = (MaterialAutoCompleteTextView) control.view;
        ArrayAdapter<Choice> adapter = new ArrayAdapter<>(input.getContext(), android.R.layout.simple_list_item_1, control.choices);
        input.setAdapter(adapter);
        input.setOnItemClickListener((parent, view, position, id) -> control.selected = adapter.getItem(position).value);
    }

    private void bindMultiSelect(Input input) {
        input.layout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        input.layout.setEndIconDrawable(android.R.drawable.arrow_down_float);
        input.layout.setEndIconContentDescription(input.layout.getHint());
        input.layout.setEndIconOnClickListener(view -> showChoices(input));
        input.view.setOnClickListener(view -> showChoices(input));
    }

    private void showChoices(Input input) {
        if (result.isDone() || action != null || !input.enabled || picker != null) return;
        try {
            CharSequence[] labels = input.choices.stream().map(choice -> choice.label).toArray(CharSequence[]::new);
            boolean[] checked = new boolean[labels.length];
            for (int i = 0; i < checked.length; i++) checked[i] = input.checked.contains(input.choices.get(i).value);
            AlertDialog choices = new MaterialAlertDialogBuilder(activity).setTitle(input.layout.getHint())
                    .setMultiChoiceItems(labels, checked, (dialog, which, selected) -> checked[which] = selected)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> applyChoices(input, checked))
                    .setNegativeButton(android.R.string.cancel, null).create();
            picker = choices;
            choices.setOnDismissListener(dialog -> {
                if (picker == choices) picker = null;
                if (!result.isDone() && action == null && input.enabled) input.view.requestFocus();
            });
            choices.show();
        } catch (RuntimeException error) {
            result.completeExceptionally(error);
        }
    }

    private void applyChoices(Input input, boolean[] checked) {
        if (result.isDone() || action != null || !input.enabled) return;
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < checked.length; i++) if (checked[i]) selected.add(input.choices.get(i).value);
        input.setChecked(selected);
    }

    private void closePicker() {
        AlertDialog current = picker;
        picker = null;
        if (current != null) current.dismiss();
    }

    private List<Choice> readChoices(JSONArray items) throws JSONException {
        if (items.length() == 0) throw new IllegalArgumentException("Dialog choices require at least one option");
        List<Choice> choices = new ArrayList<>();
        Set<String> values = new HashSet<>();
        for (int i = 0; i < items.length(); i++) choices.add(readChoice(items.getJSONObject(i), values));
        return choices;
    }

    private Choice readChoice(JSONObject option, Set<String> values) throws JSONException {
        Object value = option.get("value");
        if (!(value instanceof String id) || id.isBlank() || !values.add(id)) throw new IllegalArgumentException("Dialog option values must be nonempty unique strings");
        Object caption = option.has("label") ? option.get("label") : id;
        if (!(caption instanceof String label) || label.isBlank()) throw new IllegalArgumentException("Dialog option labels must be nonempty strings");
        return new Choice(id, label);
    }

    private int inputLimit(JSONObject field, String key, int fallback) throws JSONException {
        if (!field.has(key)) return fallback;
        Object value = field.get(key);
        if (!(value instanceof Number limit) || limit.doubleValue() != limit.intValue() || limit.intValue() < 0) throw new IllegalArgumentException("Invalid dialog input limit: " + key);
        return limit.intValue();
    }

    private boolean flag(JSONObject json, String name, boolean fallback) throws JSONException {
        if (!json.has(name)) return fallback;
        if (!(json.get(name) instanceof Boolean value)) throw new IllegalArgumentException("Dialog " + name + " must be a boolean");
        return value;
    }

    private int inputType(String type) {
        return switch (type) {
            case "text" -> InputType.TYPE_CLASS_TEXT;
            case "textarea" -> InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
            case "phone" -> InputType.TYPE_CLASS_PHONE;
            case "number" -> InputType.TYPE_CLASS_NUMBER;
            case "password" -> InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
            default -> throw new IllegalArgumentException("Unsupported dialog input type: " + type);
        };
    }

    private void bindButtons(MaterialAlertDialogBuilder builder, JSONArray buttons) throws JSONException {
        if (buttons == null) {
            addButton(builder, new ButtonOptions("ok", activity.getString(android.R.string.ok), true, inputs.keySet(), true), DialogInterface.BUTTON_POSITIVE);
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> result.complete(null));
            return;
        }
        if (buttons.length() < 1 || buttons.length() > BUTTONS.length) throw new IllegalArgumentException("Dialog requires 1 to 3 buttons");
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < buttons.length(); i++) addButton(builder, readButton(buttons.getJSONObject(i), ids), BUTTONS[i]);
    }

    private ButtonOptions readButton(JSONObject button, Set<String> ids) throws JSONException {
        String id = button.getString("id");
        String label = button.getString("label");
        if (id.isBlank() || label.isBlank() || !ids.add(id)) throw new IllegalArgumentException("Dialog buttons require unique ids and nonempty labels");
        boolean close = flag(button, "close", true);
        if (!close && onAction == null) throw new IllegalArgumentException("Nonclosing dialog buttons require onAction");
        return new ButtonOptions(id, label, close, validateNames(button.has("validate") ? button.getJSONArray("validate") : null), flag(button, "enabled", true));
    }

    private void addButton(MaterialAlertDialogBuilder builder, ButtonOptions button, int which) {
        buttons.put(which, button);
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE -> builder.setPositiveButton(button.label, null);
            case DialogInterface.BUTTON_NEGATIVE -> builder.setNegativeButton(button.label, null);
            case DialogInterface.BUTTON_NEUTRAL -> builder.setNeutralButton(button.label, null);
        }
    }

    private Set<String> validateNames(JSONArray names) throws JSONException {
        if (names == null) return inputs.keySet();
        Set<String> selected = new HashSet<>();
        for (int i = 0; i < names.length(); i++) {
            String name = names.getString(i);
            if (!inputs.containsKey(name)) throw new IllegalArgumentException("Unknown dialog input: " + name);
            selected.add(name);
        }
        return selected;
    }

    private boolean validateInputs(ButtonOptions button) {
        Input first = null;
        for (Map.Entry<String, Input> input : inputs.entrySet()) {
            Input field = input.getValue();
            field.layout.setError(null);
            if (field.enabled && button.fields.contains(input.getKey()) && !field.validate() && first == null) first = field;
        }
        if (first != null) first.view.requestFocus();
        return first == null;
    }

    private void onButton(int which, ButtonOptions button) {
        if (result.isDone() || action != null || !button.enabled) return;
        try {
            if (!validateInputs(button)) return;
            String event = inputEvent(button.id);
            if (onAction == null) {
                result.complete(event);
            } else {
                startAction(which, button, event);
            }
        } catch (JSONException | RuntimeException error) {
            result.completeExceptionally(error);
        }
    }

    private String inputEvent(String which) throws JSONException {
        JSONObject values = new JSONObject();
        for (Map.Entry<String, Input> input : inputs.entrySet()) values.put(input.getKey(), input.getValue().value());
        return new JSONObject().put("button", which).put("values", values).toString();
    }

    private void startAction(int which, ButtonOptions button, String event) {
        setBusy(true, which);
        action = Objects.requireNonNull(onAction.apply(event));
        CompletableFuture<String> current = action;
        current.whenComplete((update, error) -> App.post(() -> completeAction(current, which, button, update, error)));
    }

    private void setBusy(boolean busy, int selected) {
        setEnabled(!busy);
        Button button = dialog.getButton(selected);
        button.setText(busy ? activity.getString(R.string.dialog_processing) : buttons.get(selected).label);
        if (!busy) focusButton(selected);
    }

    private void setEnabled(boolean enabled) {
        if (!enabled) closePicker();
        for (int which : BUTTONS) {
            Button button = dialog.getButton(which);
            ButtonOptions options = buttons.get(which);
            if (button != null) button.setEnabled(enabled && (options == null || options.enabled));
        }
        for (Input input : inputs.values()) input.setEnabled(enabled && input.enabled);
        if (copy != null) copy.setEnabled(enabled);
    }

    private void focusInput() {
        for (Input input : inputs.values()) {
            if (input.enabled && input.view.requestFocus()) return;
        }
    }

    private void focusButton(int selected) {
        if (dialog.getButton(selected).isEnabled() && dialog.getButton(selected).requestFocus()) return;
        for (int which : BUTTONS) {
            Button button = dialog.getButton(which);
            if (button != null && button.isEnabled() && button.requestFocus()) return;
        }
        focusInput();
    }

    private void completeAction(CompletableFuture<String> current, int which, ButtonOptions button, String update, Throwable error) {
        if (result.isDone() || action != current) return;
        action = null;
        if (error != null) {
            result.completeExceptionally(error);
            return;
        }
        try {
            applyUpdate(which, button, update == null ? new JSONObject() : new JSONObject(update));
        } catch (JSONException | RuntimeException failure) {
            result.completeExceptionally(failure);
        }
    }

    private void applyUpdate(int which, ButtonOptions button, JSONObject patch) throws JSONException {
        if (patch.has("qrcode")) setQRCode(patch.get("qrcode"));
        if (patch.has("message")) setMessage(patch.getString("message"));
        if (patch.has("image")) setImage(patch.get("image"));
        applyValues(patch.has("values") ? patch.getJSONObject("values") : null);
        applyEnabled(patch);
        setBusy(false, which);
        boolean invalid = applyErrors(patch.has("errors") ? patch.getJSONObject("errors") : null);
        boolean close = flag(patch, "close", button.close);
        if (close && !invalid) result.complete(inputEvent(button.id));
    }

    private void applyValues(JSONObject values) throws JSONException {
        if (values == null) return;
        Map<Input, Object> updates = new LinkedHashMap<>();
        for (Iterator<String> names = values.keys(); names.hasNext(); ) {
            String name = names.next();
            Input input = inputs.get(name);
            if (input == null) throw new IllegalArgumentException("Unknown dialog input: " + name);
            Object value = values.get(name);
            input.checkValue(value);
            updates.put(input, value);
        }
        for (Map.Entry<Input, Object> update : updates.entrySet()) update.getKey().setValue(update.getValue());
    }

    private void applyEnabled(JSONObject patch) throws JSONException {
        Map<Input, Boolean> fields = readInputStates(patch.has("inputs") ? patch.getJSONArray("inputs") : new JSONArray());
        Map<ButtonOptions, Boolean> controls = readButtonStates(patch.has("buttons") ? patch.getJSONArray("buttons") : new JSONArray());
        for (Map.Entry<Input, Boolean> field : fields.entrySet()) field.getKey().enabled = field.getValue();
        for (Map.Entry<ButtonOptions, Boolean> control : controls.entrySet()) control.getKey().enabled = control.getValue();
    }

    private Map<Input, Boolean> readInputStates(JSONArray entries) throws JSONException {
        Map<Input, Boolean> fields = new LinkedHashMap<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject field = entries.getJSONObject(i);
            String name = field.getString("name");
            Input input = inputs.get(name);
            if (input == null) throw new IllegalArgumentException("Unknown dialog input: " + name);
            if (fields.put(input, flag(field, "enabled", input.enabled)) != null) throw new IllegalArgumentException("Duplicate dialog input update");
        }
        return fields;
    }

    private Map<ButtonOptions, Boolean> readButtonStates(JSONArray entries) throws JSONException {
        Map<ButtonOptions, Boolean> controls = new LinkedHashMap<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject field = entries.getJSONObject(i);
            String id = field.getString("id");
            ButtonOptions button = buttons.values().stream().filter(value -> value.id.equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown dialog button: " + id));
            if (controls.put(button, flag(field, "enabled", button.enabled)) != null) throw new IllegalArgumentException("Duplicate dialog button update");
        }
        return controls;
    }

    private boolean applyErrors(JSONObject errors) throws JSONException {
        if (errors == null) return false;
        Input first = null;
        boolean invalid = false;
        for (Iterator<String> names = errors.keys(); names.hasNext(); ) {
            String name = names.next();
            Input input = inputs.get(name);
            if (input == null) throw new IllegalArgumentException("Unknown dialog input: " + name);
            String error = errors.isNull(name) ? null : errors.getString(name);
            input.layout.setError(error);
            if (error != null && !error.isEmpty()) {
                invalid = true;
                if (input.enabled && first == null) first = input;
            }
        }
        if (first != null) first.view.requestFocus();
        return invalid;
    }

    private void onDismiss(DialogInterface dialog) {
        result.complete(null);
    }

    @Override
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_DESTROY) result.complete(null);
    }

    private void cancel() {
        result.cancel(true);
        App.post(this::close);
    }

    private void close() {
        if (action != null) action.cancel(true);
        closePicker();
        if (images != null) images.clear(image);
        if (activity != null) activity.getLifecycle().removeObserver(this);
        if (dialog != null) dialog.dismiss();
        inputs.clear();
        buttons.clear();
        action = null;
        activity = null;
        dialog = null;
        message = null;
        copy = null;
        qrcode = null;
        image = null;
        images = null;
    }

    private static final class ButtonOptions {

        private final String id;
        private final String label;
        private final boolean close;
        private final Set<String> fields;
        private boolean enabled;

        private ButtonOptions(String id, String label, boolean close, Set<String> fields, boolean enabled) {
            this.id = id;
            this.label = label;
            this.close = close;
            this.fields = fields;
            this.enabled = enabled;
        }
    }

    private record Choice(String value, String label) {

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class Input {

        private final TextInputLayout layout;
        private final EditText view;
        private final boolean required;
        private final int min;
        private final int max;
        private final List<Choice> choices;
        private final boolean multiple;
        private boolean enabled;
        private List<String> checked = List.of();
        private String selected = "";

        private Input(TextInputLayout layout, EditText view, boolean required, int min, int max, List<Choice> choices, boolean multiple, boolean enabled) {
            this.layout = layout;
            this.view = view;
            this.required = required;
            this.min = min;
            this.max = max;
            this.choices = choices;
            this.multiple = multiple;
            this.enabled = enabled;
        }

        private Object value() {
            if (multiple) return new JSONArray(checked);
            return view instanceof MaterialAutoCompleteTextView ? selected : view.getText().toString();
        }

        private Choice getChoice(String value) {
            if (value.isEmpty()) return null;
            return choices.stream().filter(choice -> choice.value.equals(value)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown dialog select value: " + value));
        }

        private List<String> readChecked(Object value) throws JSONException {
            if (!(value instanceof JSONArray array)) throw new IllegalArgumentException("Dialog multiselect values must be string arrays");
            Set<String> values = new HashSet<>();
            for (int i = 0; i < array.length(); i++) {
                if (!(array.get(i) instanceof String id) || id.isEmpty() || !values.add(id)) throw new IllegalArgumentException("Dialog multiselect values must be unique option strings");
                getChoice(id);
            }
            return choices.stream().filter(choice -> values.contains(choice.value)).map(choice -> choice.value).toList();
        }

        private void checkValue(Object value) throws JSONException {
            if (multiple) {
                readChecked(value);
            } else {
                if (!(value instanceof String text)) throw new IllegalArgumentException("Dialog input values must be strings");
                if (!choices.isEmpty()) getChoice(text);
            }
        }

        private void setValue(Object value) throws JSONException {
            if (multiple) {
                setChecked(readChecked(value));
            } else if (view instanceof MaterialAutoCompleteTextView select) {
                Choice choice = getChoice((String) value);
                select.setText(choice == null ? "" : choice.label, false);
                selected = (String) value;
            } else {
                view.setText((String) value);
            }
            layout.setError(null);
        }

        private void setChecked(List<String> values) {
            checked = values;
            view.setText(TextUtils.join(", ", choices.stream().filter(choice -> checked.contains(choice.value)).map(choice -> choice.label).toList()));
            layout.setError(null);
        }

        private void setEnabled(boolean enabled) {
            if (!enabled && view instanceof MaterialAutoCompleteTextView select) select.dismissDropDown();
            layout.setEnabled(enabled);
        }

        private boolean validate() {
            String value = multiple ? "" : (String) value();
            int length = multiple ? checked.size() : value.codePointCount(0, value.length());
            if (required && (multiple ? checked.isEmpty() : value.isBlank())) layout.setError(view.getContext().getString(R.string.dialog_required));
            else if (length > 0 && length < min) layout.setError(view.getContext().getString(multiple ? R.string.dialog_min_choices : R.string.dialog_min_length, min));
            else if (length > max) layout.setError(view.getContext().getString(multiple ? R.string.dialog_max_choices : R.string.dialog_max_length, max));
            return layout.getError() == null;
        }
    }
}
