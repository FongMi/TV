package com.fongmi.android.tv.ui.activity;

import android.Manifest;
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
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Hot;
import com.fongmi.android.tv.bean.Suggest;
import com.fongmi.android.tv.databinding.ActivitySearchBinding;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.ui.adapter.HistoryAdapter;
import com.fongmi.android.tv.ui.adapter.WordAdapter;
import com.fongmi.android.tv.ui.custom.CustomKeyboard;
import com.fongmi.android.tv.ui.custom.CustomListener;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Response;

public class SearchActivity extends BaseActivity implements WordAdapter.OnClickListener, HistoryAdapter.OnClickListener, CustomKeyboard.Callback {

    private final ActivityResultLauncher<String> launcher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> startListening());

    private ActivitySearchBinding mBinding;
    private SpeechRecognizer mRecognizer;
    private HistoryAdapter mHistoryAdapter;
    private WordAdapter mWordAdapter;
    private Animation mFlicker;
    private Handler mHandler;

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
        CustomKeyboard.init(this, mBinding);
        setRecyclerView();
        getHot();
    }

    @Override
    protected void initEvent() {
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
                stopListening();
                mBinding.keyword.setText(result);
                mBinding.keyword.setSelection(mBinding.keyword.length());
            }
        });
    }

    private void setRecyclerView() {
        mBinding.word.setHasFixedSize(true);
        mBinding.word.addItemDecoration(new SpaceItemDecoration(1, 16));
        mBinding.word.setAdapter(mWordAdapter = new WordAdapter(this));
        mBinding.history.setHasFixedSize(true);
        mBinding.history.addItemDecoration(new SpaceItemDecoration(1, 16));
        mBinding.history.setAdapter(mHistoryAdapter = new HistoryAdapter(this));
    }

    private void getHot() {
        mBinding.hint.setText(R.string.search_hot);
        OKHttp.newCall("https://node.video.qq.com/x/api/hot_mobilesearch?channdlId=0").enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<String> items = Hot.get(response.body().string());
                mHandler.post(() -> mWordAdapter.addAll(items));
            }
        });
    }

    private void getSuggest(String text) {
        mBinding.hint.setText(R.string.search_suggest);
        OKHttp.newCall("https://suggest.video.iqiyi.com/?if=mobile&key=" + text).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<String> items = Suggest.get(response.body().string());
                mHandler.post(() -> mWordAdapter.addAll(items));
            }
        });
    }

    @Override
    public void onItemClick(String text) {
        mBinding.keyword.setText(text);
        onSearch();
    }

    @Override
    public void onDataChanged(int size) {
        mBinding.historyLayout.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onSearch() {
        String keyword = mBinding.keyword.getText().toString().trim();
        mBinding.keyword.setSelection(mBinding.keyword.length());
        Utils.hideKeyboard(mBinding.keyword);
        if (TextUtils.isEmpty(keyword)) return;
        CollectActivity.start(this, keyword);
        mHandler.postDelayed(() -> mHistoryAdapter.add(keyword), 250);
    }

    @Override
    public void onRemote() {
        PushActivity.start(this);
    }

    @Override
    public void onVoice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            startListening();
        }
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mBinding.keyboard.setVisibility(View.INVISIBLE);
        mBinding.voice.setVisibility(View.VISIBLE);
        mBinding.voice.startAnimation(mFlicker);
        mRecognizer.startListening(intent);
    }

    private void stopListening() {
        mBinding.keyboard.setVisibility(View.VISIBLE);
        mBinding.voice.setVisibility(View.GONE);
        mBinding.voice.clearAnimation();
        mRecognizer.stopListening();
    }

    private void destroyRecognizer() {
        try {
            mRecognizer.destroy();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBinding.keyword.requestFocus();
    }

    @Override
    public void onBackPressed() {
        if (mBinding.voice.getVisibility() == View.VISIBLE) {
            stopListening();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyRecognizer();
    }
}
