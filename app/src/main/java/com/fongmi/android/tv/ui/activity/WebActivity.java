package com.fongmi.android.tv.ui.activity;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.PixelCopy;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Web;
import com.fongmi.android.tv.databinding.ActivityWebBinding;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.BrowserWebView;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Task;
import com.github.catvod.utils.UriUtil;
import com.google.android.material.color.MaterialColors;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Set;

public final class WebActivity extends BaseActivity implements BrowserWebView.BrowserListener {

    private static final String EXTRA_WEB = "web";

    private ActivityWebBinding binding;
    private BrowserWebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private BrowserWebView.Download download;
    private Web options;
    private boolean fullscreen;
    private boolean standalone;
    private final Runnable loginCheck = this::checkLogin;

    private final ActivityResultLauncher<Intent> fileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            this::onFileChooserResult);

    private final ActivityResultLauncher<Intent> downloadLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            this::onDownloadResult);

    public static boolean openWeb(String json) {
        return openWeb(App.activity(), Web.objectFrom(json));
    }

    private static boolean openWeb(Activity activity, Web options) {
        if (activity == null || options == null || !options.isValid()) return false;
        App.post(() -> start(activity, options));
        return true;
    }

    public static void handleAction(Activity activity, Result result) {
        if (result == null) return;
        Web options = result.takeWeb();
        if (options != null && !openWeb(activity, options)) {
            Notify.show(R.string.error_file_open);
            return;
        }
        Notify.show(result.getMsg());
    }

    public static String getUserAgent() {
        String userAgent = Setting.getUa();
        return userAgent.isEmpty() ? WebSettings.getDefaultUserAgent(App.get()) : userAgent;
    }

    private static void start(Activity activity, Web options) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        Intent intent = new Intent(activity, WebActivity.class).putExtra(EXTRA_WEB, App.gson().toJson(options));
        activity.startActivity(intent);
        if (activity instanceof VideoActivity) activity.finish();
    }

    @Override
    protected ActivityWebBinding getBinding() {
        return binding = ActivityWebBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        if (!initOptions(Web.objectFrom(getIntent().getStringExtra(EXTRA_WEB)))) return;
        setSystemBars(MaterialColors.getColor(this, android.R.attr.colorBackground, "WebActivity"));
        webView = BrowserWebView.createBrowser(this, options.getUrl(), this);
        binding.container.addView(webView, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        webView.requestFocus();
    }

    private void setSystemBars(int color) {
        boolean light = MaterialColors.isColorLight(color);
        binding.container.setBackgroundColor(color);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(light);
        controller.setAppearanceLightNavigationBars(light);
    }

    @Override
    public void onPageReady(BrowserWebView view) {
        if (Util.isLeanback() || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        String url = view.getUrl();
        view.postOnAnimation(() -> view.post(() -> copyPageColor(view, url)));
    }

    private boolean isCurrentPage(BrowserWebView view, String url) {
        return webView == view && view.isShown() && !isFinishing() && url != null && url.equals(view.getUrl());
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void copyPageColor(BrowserWebView view, String url) {
        if (!isCurrentPage(view, url) || view.getWidth() == 0 || view.getHeight() == 0) return;
        int[] location = new int[2];
        view.getLocationInWindow(location);
        int x = location[0] + view.getWidth() / 2;
        Rect source = new Rect(x, location[1], x + 1, location[1] + 1);
        Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        try {
            PixelCopy.request(getWindow(), source, bitmap, result -> onPageColorCopied(view, url, bitmap, result), view.getHandler());
        } catch (IllegalArgumentException e) {
            bitmap.recycle();
            SpiderDebug.log("WebActivity", "system bars copy rejected: %s", e.getMessage());
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void onPageColorCopied(BrowserWebView view, String url, Bitmap bitmap, int result) {
        try {
            if (result == PixelCopy.SUCCESS && isCurrentPage(view, url)) setSystemBars(bitmap.getPixel(0, 0));
            if (result != PixelCopy.SUCCESS) SpiderDebug.log("WebActivity", "system bars copy failed: %d", result);
        } finally {
            bitmap.recycle();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Web next = Web.objectFrom(intent.getStringExtra(EXTRA_WEB));
        if (webView != null && next != null && !next.getUrl().equals(webView.getUrl())) webView.hideCustomView();
        if (!initOptions(next) || webView == null || options.getUrl().equals(webView.getUrl())) return;
        webView.loadUrl(options.getUrl());
    }

    private boolean initOptions(Web next) {
        options = next;
        if (options == null) {
            finish();
            return false;
        }
        options.snapshot(url -> CookieManager.getInstance().getCookie(url));
        if (webView != null) {
            webView.removeCallbacks(loginCheck);
            if (options.hasCookies()) webView.postDelayed(loginCheck, 1000);
        }
        standalone = options.isView();
        return true;
    }

    @Override
    public void onFullscreenChanged(BrowserWebView view, boolean fullscreen) {
        if (webView != view) return;
        this.fullscreen = fullscreen;
        Util.toggleFullscreen(this, fullscreen || Util.isLeanback());
        if (!fullscreen) onPageReady(view);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && fullscreen) Util.hideSystemUI(this);
    }

    private void checkLogin() {
        if (webView == null || options == null || !options.hasCookies() || isFinishing()) return;
        if (options.isComplete(webView.getUrl(), url -> CookieManager.getInstance().getCookie(url))) {
            CookieManager.getInstance().flush();
            finish();
            return;
        }
        webView.postDelayed(loginCheck, 1000);
    }

    @Override
    public void onOpenExternal(Uri uri) {
        if (isSpiderPage() && uri.toString().equals("tv-spider://close")) {
            finish();
            return;
        }
        if (standalone) return;
        try {
            Intent intent = externalIntent(uri);
            if (intent != null) startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException | URISyntaxException e) {
            Notify.show(R.string.error_file_open);
        }
    }

    static Intent externalIntent(Uri uri) throws URISyntaxException {
        Intent parsed = "intent".equals(uri.getScheme()) ? Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME) : new Intent(Intent.ACTION_VIEW, uri);
        Uri data = parsed.getData();
        if (data == null || data.getScheme() == null || Set.of("file", "content", "javascript", "data", "about", "intent").contains(UriUtil.scheme(data))) return null;
        return new Intent(Intent.ACTION_VIEW, data).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(parsed.getPackage());
    }

    @Override
    public void onShowFileChooser(ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams params) {
        completeFileChooser(null);
        fileCallback = callback;
        try {
            FileChooser.from(fileLauncher).show(params.getAcceptTypes());
        } catch (RuntimeException e) {
            completeFileChooser(null);
            Notify.show(Notify.getError(R.string.error_file_open, e));
        }
    }

    private void onFileChooserResult(ActivityResult result) {
        Uri[] uris = WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), result.getData());
        if (uris != null) {
            for (Uri uri : uris) {
                if (FileChooser.isFileSource(uri)) continue;
                Notify.show(R.string.error_file_open);
                completeFileChooser(null);
                return;
            }
        }
        completeFileChooser(uris);
    }

    private void completeFileChooser(Uri[] uris) {
        if (fileCallback == null) return;
        ValueCallback<Uri[]> callback = fileCallback;
        fileCallback = null;
        callback.onReceiveValue(uris);
    }

    @Override
    public void onDownload(BrowserWebView.Download download) {
        if (this.download != null) {
            Notify.show(R.string.error_file_save);
            return;
        }
        this.download = download;
        try {
            downloadLauncher.launch(createDownloadIntent(download));
        } catch (RuntimeException e) {
            this.download = null;
            Notify.show(Notify.getError(R.string.error_file_save, e));
        }
    }

    static Intent createDownloadIntent(BrowserWebView.Download download) {
        return new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(download.mimeType())
                .putExtra(Intent.EXTRA_TITLE, download.fileName())
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    @Override
    public void onDownloadError() {
        Notify.show(R.string.error_file_save);
    }

    @Override
    public void onRendererGone(BrowserWebView view) {
        if (webView != view) return;
        completeFileChooser(null);
        download = null;
        binding.container.removeView(view);
        webView = null;
        finish();
    }

    private void onDownloadResult(ActivityResult result) {
        BrowserWebView.Download download = this.download;
        this.download = null;
        if (download == null || result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
        Uri uri = result.getData().getData();
        if (!FileChooser.isFileSource(uri)) {
            Notify.show(R.string.error_file_save);
            return;
        }
        Task.execute(() -> writeDownload(uri, download));
    }

    void writeDownload(Uri uri, BrowserWebView.Download download) {
        try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
            if (output == null) throw new IOException("Unable to open target file");
            output.write(download.data());
            output.flush();
            App.post(() -> Notify.show(R.string.file_export_success));
        } catch (IOException | RuntimeException e) {
            App.post(() -> Notify.show(Notify.getError(R.string.error_file_save, e)));
        }
    }

    @Override
    protected boolean customWall() {
        return false;
    }

    @Override
    protected void onBackInvoked() {
        if (isSpiderPage()) {
            webView.evaluateJavascript("(() => { if (typeof window.spiderBack !== 'function') return false; window.spiderBack(); return true; })()", handled -> {
                if ("false".equals(handled) && isSpiderPage()) finish();
            });
            return;
        }
        if (webView != null && webView.handleBackNavigation()) return;
        super.onBackInvoked();
    }

    private boolean isSpiderPage() {
        if (webView == null || webView.getUrl() == null) return false;
        Uri page = Uri.parse(webView.getUrl());
        Uri local = Uri.parse(Server.get().getAddress("/spider.html"));
        return "http".equals(page.getScheme()) && local.getAuthority().equals(page.getAuthority()) && "/spider.html".equals(page.getPath());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.removeCallbacks(loginCheck);
            if (options != null && options.hasCookies()) webView.postDelayed(loginCheck, 1000);
        }
    }

    @Override
    protected void onPause() {
        if (webView != null) {
            webView.removeCallbacks(loginCheck);
            webView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        completeFileChooser(null);
        download = null;
        if (webView != null) {
            binding.container.removeView(webView);
            webView.removeCallbacks(loginCheck);
            webView.release();
            webView = null;
        }
        super.onDestroy();
    }
}
