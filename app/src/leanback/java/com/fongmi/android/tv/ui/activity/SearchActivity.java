package com.fongmi.android.tv.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Hot;
import com.fongmi.android.tv.bean.Suggest;
import com.fongmi.android.tv.databinding.ActivitySearchBinding;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.ui.custom.CustomKeyboard;
import com.fongmi.android.tv.ui.custom.CustomListener;
import com.fongmi.android.tv.ui.presenter.WordPresenter;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Response;

public class SearchActivity extends BaseActivity implements WordPresenter.OnClickListener, CustomKeyboard.Callback {

    private final ActivityResultLauncher<String> launcherString = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> onVoice());

    private ActivitySearchBinding mBinding;
    private ArrayObjectAdapter mWordAdapter;
    private SpeechRecognizer mRecognizer;
    private Animation mFlicker;
    private Handler mHandler;

    private boolean hasVoice() {
        return SpeechRecognizer.isRecognitionAvailable(this);
    }

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SearchActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySearchBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mFlicker = ResUtil.getAnim(R.anim.flicker);
        mHandler = new Handler(Looper.getMainLooper());
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mBinding.voice.setVisibility(hasVoice() ? View.VISIBLE : View.GONE);
        CustomKeyboard.init(this, mBinding);
        mBinding.keyword.requestFocus();
        setRecyclerView();
        getHot();
    }

    @Override
    protected void initEvent() {
        mBinding.voice.setOnClickListener(view -> onVoice());
        mBinding.keyword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) onSearch();
            return true;
        });
        mBinding.keyword.addTextChangedListener(new CustomListener() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) getHot();
                else getSuggest(s.toString());
            }
        });
        mRecognizer.setRecognitionListener(new CustomListener() {
            @Override
            public void onResults(String result) {
                mBinding.voice.clearAnimation();
                mBinding.keyword.setText(result);
                mBinding.keyword.setSelection(mBinding.keyword.length());
            }
        });
    }

    @SuppressLint("RestrictedApi")
    private void setRecyclerView() {
        mBinding.word.setHorizontalSpacing(ResUtil.dp2px(16));
        mBinding.word.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.word.setFocusScrollStrategy(HorizontalGridView.FOCUS_SCROLL_ITEM);
        mBinding.word.setAdapter(new ItemBridgeAdapter(mWordAdapter = new ArrayObjectAdapter(new WordPresenter(this))));
    }

    private void onVoice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            launcherString.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            mBinding.voice.startAnimation(mFlicker);
            mRecognizer.startListening(intent);
        }
    }

    @Override
    public void onSearch() {
        String keyword = mBinding.keyword.getText().toString().trim();
        mBinding.keyword.setSelection(mBinding.keyword.length());
        Utils.hideKeyboard(mBinding.keyword);
        if (TextUtils.isEmpty(keyword)) return;
        CollectActivity.start(this, keyword);
    }

    @Override
    public void onRemote() {
        PushActivity.start(this);
    }

    @Override
    public void onItemClick(String text) {
        mBinding.keyword.setText(text);
        onSearch();
    }

    private void getHot() {
        OKHttp.newCall("https://node.video.qq.com/x/api/hot_mobilesearch?channdlId=0").enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<String> items = Hot.get(response.body().string());
                mHandler.post(() -> mWordAdapter.setItems(items, null));
            }
        });
    }

    private void getSuggest(String text) {
        OKHttp.newCall("https://suggest.video.iqiyi.com/?if=mobile&key=" + text).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<String> items = Suggest.get(response.body().string());
                mHandler.post(() -> mWordAdapter.setItems(items, null));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecognizer.destroy();
    }
}
