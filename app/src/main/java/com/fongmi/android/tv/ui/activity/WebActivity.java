package com.fongmi.android.tv.ui.activity;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivityWebBinding;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.BrowserWebView;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Task;
import com.fongmi.android.tv.utils.UrlUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Set;

public final class WebActivity extends BaseActivity implements BrowserWebView.BrowserListener {

    private static final String EXTRA_URL = "url";
    private static final String EXTRA_LOGIN_COOKIE = "loginCookie";

    private ActivityWebBinding binding;
    private BrowserWebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private BrowserWebView.Download download;
    private String loginUrl;
    private String loginCookie;
    private String initialLoginCookie;
    private final Runnable loginCheck = this::checkLogin;

    private final ActivityResultLauncher<Intent> fileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            this::onFileChooserResult);

    private final ActivityResultLauncher<Intent> downloadLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            this::onDownloadResult);

    public static boolean open(String url) {
        return open(url, null);
    }

    public static boolean open(String url, String loginCookie) {
        String scheme = UrlUtil.scheme(url);
        if (App.activity() == null || !("http".equals(scheme) || "https".equals(scheme))) return false;
        if (!validCookieName(loginCookie)) return false;
        App.post(() -> start(url, loginCookie));
        return true;
    }

    public static String getUserAgent() {
        String userAgent = Setting.getUa();
        return userAgent.isEmpty() ? WebSettings.getDefaultUserAgent(App.get()) : userAgent;
    }

    private static void start(String url, String loginCookie) {
        Activity activity = App.activity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        Intent intent = new Intent(activity, WebActivity.class).putExtra(EXTRA_URL, url);
        if (loginCookie != null) intent.putExtra(EXTRA_LOGIN_COOKIE, loginCookie);
        activity.startActivity(intent);
        if (activity instanceof VideoActivity) activity.finish();
    }

    private static boolean validCookieName(String name) {
        if (name == null) return true;
        if (name.isEmpty()) return false;
        for (int i = 0; i < name.length(); i++) {
            char value = name.charAt(i);
            if (value <= 0x20 || value >= 0x7F || "()<>@,;:\\\"/[]?={}".indexOf(value) >= 0) return false;
        }
        return true;
    }

    @Override
    protected ActivityWebBinding getBinding() {
        return binding = ActivityWebBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        initLogin(getIntent());
        webView = BrowserWebView.createBrowser(this, getIntent().getStringExtra(EXTRA_URL), this);
        binding.container.addView(webView, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        webView.requestFocus();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        initLogin(intent);
        String url = intent.getStringExtra(EXTRA_URL);
        if (webView == null || url == null || url.equals(webView.getUrl())) return;
        webView.loadUrl(url);
    }

    private void initLogin(Intent intent) {
        loginUrl = intent.getStringExtra(EXTRA_URL);
        loginCookie = intent.getStringExtra(EXTRA_LOGIN_COOKIE);
        initialLoginCookie = loginCookie == null ? "" : cookieValue(CookieManager.getInstance().getCookie(loginUrl), loginCookie);
        if (webView != null) {
            webView.removeCallbacks(loginCheck);
            if (loginCookie != null) webView.postDelayed(loginCheck, 1000);
        }
    }

    private void checkLogin() {
        if (webView == null || loginCookie == null || isFinishing()) return;
        String cookies = CookieManager.getInstance().getCookie(loginUrl);
        if (isLoginComplete(loginUrl, webView.getUrl(), loginCookie, initialLoginCookie, cookies)) {
            CookieManager.getInstance().flush();
            finish();
            return;
        }
        webView.postDelayed(loginCheck, 1000);
    }

    static boolean isLoginComplete(String origin, String page, String name, String initial, String cookies) {
        if (origin == null || page == null || name == null || name.isEmpty()) return false;
        Uri expected = Uri.parse(origin);
        Uri current = Uri.parse(page);
        if (!"https".equals(expected.getScheme()) || !Objects.equals(expected.getScheme(), current.getScheme())
                || expected.getHost() == null || !Objects.equals(expected.getHost(), current.getHost())
                || expected.getPort() != current.getPort()) return false;
        String value = cookieValue(cookies, name);
        return !value.isEmpty() && !value.equals(initial);
    }

    private static String cookieValue(String cookies, String name) {
        if (cookies == null) return "";
        for (String item : cookies.split(";")) {
            int separator = item.indexOf('=');
            if (separator > 0 && item.substring(0, separator).trim().equals(name)) return item.substring(separator + 1).trim();
        }
        return "";
    }

    @Override
    public void onOpenExternal(Uri uri) {
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
        if (data == null || data.getScheme() == null || Set.of("file", "content", "javascript", "data", "about", "intent").contains(UrlUtil.scheme(data))) return null;
        // Keep only the browsable destination; never inherit components, extras or grant flags.
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
                uris = null;
                Notify.show(R.string.error_file_open);
                break;
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
        if (webView != null && webView.handleBackNavigation()) return;
        super.onBackInvoked();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.removeCallbacks(loginCheck);
            if (loginCookie != null) webView.postDelayed(loginCheck, 1000);
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
