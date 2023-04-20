package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.cast.ScanEvent;
import com.fongmi.android.tv.databinding.ActivityScanBinding;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.Utils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import java.util.List;

public class ScanActivity extends BaseActivity implements BarcodeCallback {

    private ActivityScanBinding mBinding;
    private CaptureManager mCapture;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, ScanActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityScanBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.hideSystemUI(this);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mCapture = new CaptureManager(this, mBinding.scanner);
        mBinding.scanner.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(List.of(BarcodeFormat.QR_CODE)));
    }

    @Override
    public void possibleResultPoints(List<ResultPoint> resultPoints) {
    }

    @Override
    public void barcodeResult(BarcodeResult result) {
        if (!result.getText().startsWith("http")) return;
        ScanEvent.post(result.getText());
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mCapture.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Utils.hideSystemUI(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Utils.hideSystemUI(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCapture.onResume();
        mBinding.scanner.decodeSingle(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCapture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCapture.onDestroy();
    }
}
