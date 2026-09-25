package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.SystemClock;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.setting.Setting;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Util;
import com.google.common.net.HttpHeaders;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressLint("ViewConstructor")
public final class BrowserWebView extends WebView {

    public interface BrowserListener {

        void onShowFileChooser(@NonNull ValueCallback<Uri[]> callback, @NonNull WebChromeClient.FileChooserParams params);

        void onDownload(@NonNull Download download);

        void onDownloadError();

        default void onRendererGone(@NonNull BrowserWebView view) {}

        default void onOpenExternal(@NonNull Uri uri) {}
    }

    public record Download(@NonNull String fileName, @NonNull String mimeType, @NonNull byte[] data) {}

    public interface PlaybackListener {

        void onPlayingChanged(@NonNull BrowserWebView view, boolean isPlaying);

        void onStateChanged(@NonNull BrowserWebView view, @NonNull PlaybackState state);

        void onFailure(@NonNull BrowserWebView view, boolean rendererGone);

        default void onFullscreenChanged(@NonNull BrowserWebView view, boolean fullscreen) {}

        default void onChallengeChanged(@NonNull BrowserWebView view, boolean visible) {}
    }

    public enum PlaybackType {
        PAGE,
        VIDEO,
        FRAME
    }

    public record PlaybackState(PlaybackType type, boolean playing, boolean buffering, boolean ended, int width, int height, long positionMs, long durationMs, boolean started) {

        private static PlaybackState from(String value) throws JSONException {
            JSONArray array = new JSONArray(value);
            int index = array.optInt(0, -1);
            if (index < 0 || index >= PlaybackType.values().length) throw new JSONException("Unknown playback type");
            PlaybackType type = PlaybackType.values()[index];
            return new PlaybackState(type, array.getBoolean(1), array.getBoolean(2), array.getBoolean(3), array.optInt(4), array.optInt(5), Math.max(0, array.optLong(6)), array.optLong(7, -1), array.optBoolean(8));
        }

        public boolean isSeekable() {
            return type == PlaybackType.VIDEO && durationMs > 0;
        }

        private boolean shouldShowContent() {
            return type == PlaybackType.FRAME || (type == PlaybackType.VIDEO && started);
        }
    }

    private static final String BLANK = "about:blank";
    private static final String TAG = "BrowserWebView";
    private static final int MAX_DOWNLOAD_BYTES = 512 * 1024;
    private static final int MAX_DOWNLOAD_BASE64_CHARS = ((MAX_DOWNLOAD_BYTES + 2) / 3) * 4;
    private static final long DOWNLOAD_POLL_DELAY = 50;
    private static final long DOWNLOAD_TIMEOUT = 10_000;
    private static final String DOWNLOAD_BRIDGE_NAME = "FongMiDownloads";
    private static final String DOWNLOAD_HOOK_SCRIPT = """
      (() => {
          if (window.__fongmiDownloadHooked) return 'already';
          const bridge = window[$BRIDGE];
          if (!bridge || typeof bridge.onDownload !== 'function') return 'bridge-missing';
          if (typeof URL.createObjectURL !== 'function' || typeof URL.revokeObjectURL !== 'function') return 'blob-api-missing';
          window.__fongmiDownloadHooked = true;
          const blobs = window.__fongmiDownloadBlobs = Object.create(null);
          const createObjectURL = URL.createObjectURL.bind(URL);
          const revokeObjectURL = URL.revokeObjectURL.bind(URL);
          URL.createObjectURL = blob => {
              const url = createObjectURL(blob);
              blobs[url] = blob;
              return url;
          };
          URL.revokeObjectURL = url => {
              delete blobs[url];
              revokeObjectURL(url);
          };
          const readAnchor = anchor => {
              if (!anchor || !anchor.hasAttribute('download') || !anchor.href.startsWith('blob:')) return false;
              const pageUrl = location.href;
              const blobUrl = anchor.href;
              const fileName = anchor.download || 'config.json';
              const retained = blobs[blobUrl];
              const source = retained ? Promise.resolve(retained) : fetch(blobUrl).then(response => response.blob());
              source.then(blob => {
                  if (blob.size > $LIMIT) {
                      bridge.onError($TOKEN, pageUrl, blobUrl);
                      return;
                  }
                  const reader = new FileReader();
                  reader.onload = () => {
                      const value = String(reader.result || '');
                      const comma = value.indexOf(',');
                      if (comma < 0) {
                          bridge.onError($TOKEN, pageUrl, blobUrl);
                          return;
                      }
                      bridge.onDownload($TOKEN, pageUrl, blobUrl, fileName, blob.type || '', value.slice(comma + 1));
                  };
                  reader.onerror = () => bridge.onError($TOKEN, pageUrl, blobUrl);
                  reader.readAsDataURL(blob);
              }).catch(() => bridge.onError($TOKEN, pageUrl, blobUrl));
              return true;
          };
          const click = HTMLAnchorElement.prototype.click;
          HTMLAnchorElement.prototype.click = function() {
              if (!readAnchor(this)) click.call(this);
          };
          document.addEventListener('click', event => {
              const target = event.target;
              const anchor = target && target.closest ? target.closest('a[download]') : null;
              if (!readAnchor(anchor)) return;
              event.preventDefault();
              event.stopImmediatePropagation();
          }, true);
          return 'installed';
      })()
      """;
    private static final String DOWNLOAD_READ_SCRIPT = """
      (() => {
          const store = window.__fongmiDownloads || (window.__fongmiDownloads = Object.create(null));
          const key = $KEY;
          store[key] = {state: 'pending'};
          const blobs = window.__fongmiDownloadBlobs;
          const retained = blobs && blobs[$URL];
          const source = retained ? Promise.resolve(retained) : fetch($URL).then(response => {
              if (!response.ok) throw new Error('Download failed');
              return response.blob();
          });
          source
              .then(blob => {
                  if (blob.size > $LIMIT) {
                      store[key] = {state: 'error'};
                      return;
                  }
                  const reader = new FileReader();
                  reader.onload = () => {
                      const value = String(reader.result || '');
                      const comma = value.indexOf(',');
                      store[key] = comma < 0
                          ? {state: 'error'}
                          : {state: 'done', type: blob.type || '', data: value.slice(comma + 1)};
                  };
                  reader.onerror = () => store[key] = {state: 'error'};
                  reader.readAsDataURL(blob);
              })
              .catch(() => store[key] = {state: 'error'});
      })()
      """;
    private static final String DOWNLOAD_POLL_SCRIPT = """
      (() => {
          const store = window.__fongmiDownloads;
          const result = store && store[$KEY];
          if (!result || result.state === 'pending') return null;
          delete store[$KEY];
          return JSON.stringify(result);
      })()
      """;
    private static final String PLAYBACK_RUNTIME_SCRIPT = """
          const runtime = window.__fongmiPlaybackRuntime || (window.__fongmiPlaybackRuntime = (() => {
          const visibleArea = element => {
              if (!element || !element.isConnected) return -1;
              const rect = element.getBoundingClientRect();
              const view = element.ownerDocument.defaultView;
              const style = view.getComputedStyle(element);
              const width = Math.max(0, Math.min(rect.right, view.innerWidth) - Math.max(rect.left, 0));
              const height = Math.max(0, Math.min(rect.bottom, view.innerHeight) - Math.max(rect.top, 0));
              return width > 0 && height > 0 && style.display !== 'none' && style.visibility !== 'hidden' && Number(style.opacity || 1) !== 0 ? width * height : -1;
          };
          const effectiveArea = (element, ancestors) => {
              let area = visibleArea(element);
              if (area <= 0) return -1;
              for (const ancestor of ancestors) {
                  const ancestorArea = visibleArea(ancestor);
                  if (ancestorArea <= 0) return -1;
                  area = Math.min(area, ancestorArea);
              }
              return area;
          };
          const shadowHosts = element => {
              const hosts = [];
              let root = element && element.getRootNode ? element.getRootNode() : null;
              while (root && root.host) {
                  hosts.unshift(root.host);
                  root = root.host.getRootNode ? root.host.getRootNode() : null;
              }
              return hosts;
          };
          const walkRoots = (root, visitor) => {
              visitor(root);
              for (const element of root.querySelectorAll('*')) {
                  if (element.shadowRoot) walkRoots(element.shadowRoot, visitor);
              }
          };
          const renderedVideos = new WeakSet();
          const observedVideos = new WeakSet();
          const trackedVideos = new WeakSet();
          const observeFirstFrame = element => {
              if (!trackedVideos.has(element)) {
                  trackedVideos.add(element);
                  element.addEventListener('loadstart', () => {
                      renderedVideos.delete(element);
                      observedVideos.delete(element);
                  });
              }
              if (observedVideos.has(element)) return;
              observedVideos.add(element);
              const markRendered = () => {
                  if (element.paused && element.currentTime <= 0) {
                      observedVideos.delete(element);
                      element.addEventListener('playing', () => observeFirstFrame(element), {once: true});
                      return;
                  }
                  renderedVideos.add(element);
              };
              if (typeof element.requestVideoFrameCallback === 'function') element.requestVideoFrameCallback(markRendered);
              else if (element.readyState >= 2 && element.videoWidth > 0) markRendered();
              else element.addEventListener('loadeddata', markRendered, {once: true});
          };
          const stateOf = target => {
              if (!target) return [0, false, true, false, window.innerWidth, window.innerHeight, 0, -1, false];
              const element = target.element;
              if (!element || !element.isConnected || (target.containers || []).some(container => !container.isConnected)) return null;
              const rect = element.getBoundingClientRect();
              if (target.kind === 'frame') return [2, false, false, false, Math.round(rect.width), Math.round(rect.height), 0, -1, false];
              const playing = !element.paused && !element.ended && element.readyState >= 3;
              const width = element.videoWidth || Math.round(rect.width);
              const height = element.videoHeight || Math.round(rect.height);
              const position = Number.isFinite(element.currentTime) ? Math.round(element.currentTime * 1000) : 0;
              const duration = Number.isFinite(element.duration) ? Math.round(element.duration * 1000) : -1;
              observeFirstFrame(element);
              const started = renderedVideos.has(element);
              const buffering = !element.ended && (!started || element.readyState < 2 || (!element.paused && element.readyState < 3));
              return [1, playing, buffering, element.ended, width, height, position, duration, started];
          };
          return {effectiveArea, shadowHosts, walkRoots, stateOf};
          })());
      """;
    private static final String ACTIVATE_SCRIPT = "(() => {" + PLAYBACK_RUNTIME_SCRIPT + """
              const {effectiveArea, shadowHosts, walkRoots, stateOf} = runtime;
              const observe = root => {
                  if (!window.__fongmiPlaybackObserver || !root) return;
                  window.__fongmiPlaybackObserver.observe(root, {childList: true, subtree: true, attributes: true, attributeFilter: ['src', 'hidden', 'class']});
              };
              const hasPlaybackCandidate = node => {
                  if (!node || node.nodeType !== 1) return false;
                  let found = false;
                  walkRoots(node, root => {
                      if (found) return;
                      found = !!((root.matches && root.matches('video,iframe')) || root.querySelector('video,iframe'));
                  });
                  return found;
              };
              if (!window.__fongmiPlaybackObserver && window.MutationObserver) {
                  window.__fongmiPlaybackDirty = true;
                  window.__fongmiPlaybackObserver = new MutationObserver(records => {
                      const active = window.__fongmiPlaybackTarget;
                      if (active && (!active.element || !active.element.isConnected)) {
                          window.__fongmiPlaybackDirty = true;
                          return;
                      }
                      for (const record of records) {
                          if (record.type === 'attributes' && hasPlaybackCandidate(record.target)) {
                              window.__fongmiPlaybackDirty = true;
                              return;
                          }
                          if (record.type !== 'childList') continue;
                          for (const node of [...record.addedNodes, ...record.removedNodes]) {
                              if (!hasPlaybackCandidate(node)) continue;
                              window.__fongmiPlaybackDirty = true;
                              return;
                          }
                      }
                  });
                  observe(document.documentElement);
              }
              const now = Date.now();
              const previous = window.__fongmiPlaybackTarget || null;
              const previousState = stateOf(previous);
              const lastScan = window.__fongmiPlaybackScanAt || 0;
              const stablePlayback = previous && previous.kind === 'video' && previousState && previousState[8] && !previousState[3] && (previousState[1] || previousState[2]);
              if (!window.__fongmiPlaybackDirty && stablePlayback) return previousState;
              if (!window.__fongmiPlaybackDirty && previousState && now - lastScan < 10000) return previousState;
              window.__fongmiPlaybackDirty = false;
              window.__fongmiPlaybackScanAt = now;
              let bestVideo = null;
              let bestVideoArea = -1;
              let hiddenVideo = null;
              let bestFrame = null;
              let bestFrameScore = -1;
              const restore = () => {
                  for (const entry of window.__fongmiPlaybackStyled || []) {
                      if (!entry.element || !entry.element.isConnected) continue;
                      if (entry.style === null) entry.element.removeAttribute('style');
                      else entry.element.setAttribute('style', entry.style);
                  }
                  window.__fongmiPlaybackStyled = [];
              };
              restore();
              const inspectDocument = (doc, containers) => {
                  if (!doc || !doc.documentElement) return;
                  walkRoots(doc, root => {
                      observe(root);
                      for (const video of root.querySelectorAll('video')) {
                          const ancestors = [...containers, ...shadowHosts(video)];
                          if (!hiddenVideo) hiddenVideo = {kind: 'video', element: video, containers: [...ancestors, video]};
                          const area = effectiveArea(video, ancestors);
                          if (area > bestVideoArea) {
                              bestVideo = {kind: 'video', element: video, containers: [...ancestors, video]};
                              bestVideoArea = area;
                          }
                      }
                      for (const frame of root.querySelectorAll('iframe')) {
                          const frameAncestors = [...containers, ...shadowHosts(frame)];
                          const frameContainers = [...frameAncestors, frame];
                          let childDocument = null;
                          try {
                              childDocument = frame.contentDocument;
                          } catch (ignored) {
                          }
                          if (childDocument && childDocument.documentElement) inspectDocument(childDocument, frameContainers);
                          const area = effectiveArea(frame, frameAncestors);
                          if (area <= 0) continue;
                          const rect = frame.getBoundingClientRect();
                          const ratio = rect.height > 0 ? rect.width / rect.height : 0;
                          const attributes = `${frame.id} ${frame.className} ${frame.title} ${frame.name} ${frame.allow} ${frame.src}`.toLowerCase();
                          const signaled = frame.allowFullscreen || /(autoplay|fullscreen|media|player|video|stream|live|embed|play)/.test(attributes);
                          const viewport = Math.max(1, frame.ownerDocument.defaultView.innerWidth * frame.ownerDocument.defaultView.innerHeight);
                          const plausibleRatio = ratio >= 0.45 && ratio <= 2.5;
                          const substantial = area >= viewport * 0.45;
                          const largeEnough = area >= viewport * 0.08;
                          if (!plausibleRatio || !largeEnough || (!signaled && !substantial)) continue;
                          const score = area + (signaled ? viewport : 0);
                          if (score > bestFrameScore) {
                              bestFrame = {kind: 'frame', element: frame, containers: frameContainers};
                              bestFrameScore = score;
                          }
                      }
                  });
              };
              inspectDocument(document, []);
              const target = bestVideo || bestFrame || hiddenVideo;
              const changed = !previous || !target || previous.kind !== target.kind || previous.element !== target.element;
              window.__fongmiPlaybackTarget = target;
              if (!target) return stateOf(null);
              const styled = window.__fongmiPlaybackStyled;
              const remember = element => {
                  if (!styled.some(entry => entry.element === element)) styled.push({element, style: element.getAttribute('style')});
              };
              const isolate = element => {
                  let current = element;
                  while (current) {
                      remember(current);
                      current.style.setProperty('visibility', 'visible', 'important');
                      current.style.setProperty('opacity', '1', 'important');
                      const parent = current.parentElement;
                      if (parent) {
                          for (const sibling of parent.children) {
                              if (sibling === current) continue;
                              remember(sibling);
                              sibling.style.setProperty('visibility', 'hidden', 'important');
                              sibling.style.setProperty('opacity', '0', 'important');
                              sibling.style.setProperty('pointer-events', 'none', 'important');
                          }
                          current = parent;
                      } else {
                          const root = current.getRootNode ? current.getRootNode() : null;
                          current = root && root.host ? root.host : null;
                      }
                  }
              };
              const documents = new Set();
              for (const element of target.containers) {
                  isolate(element);
                  remember(element);
                  const style = element.style;
                  style.setProperty('position', 'fixed', 'important');
                  style.setProperty('inset', '0', 'important');
                  style.setProperty('width', '100vw', 'important');
                  style.setProperty('height', '100vh', 'important');
                  style.setProperty('max-width', 'none', 'important');
                  style.setProperty('max-height', 'none', 'important');
                  style.setProperty('margin', '0', 'important');
                  style.setProperty('padding', '0', 'important');
                  style.setProperty('z-index', '2147483647', 'important');
                  style.setProperty('background', '#000', 'important');
                  style.setProperty('transform', 'none', 'important');
                  if (target.kind === 'video' && element === target.element) {
                      style.setProperty('display', 'block', 'important');
                      style.setProperty('object-fit', 'contain', 'important');
                  }
                  documents.add(element.ownerDocument);
              }
              for (const doc of documents) {
                  if (doc.documentElement) {
                      remember(doc.documentElement);
                      doc.documentElement.style.setProperty('overflow', 'hidden', 'important');
                  }
                  if (doc.body) {
                      remember(doc.body);
                      doc.body.style.setProperty('overflow', 'hidden', 'important');
                  }
              }
              if (target.kind === 'video' && changed) {
                  const promise = target.element.play();
                  if (promise && promise.catch) promise.catch(() => {});
              }
              return stateOf(target);
          })()
          """;
    private static final String STATE_SCRIPT = """
      (() => {
          const runtime = window.__fongmiPlaybackRuntime;
          return runtime ? runtime.stateOf(window.__fongmiPlaybackTarget || null) : null;
      })()
      """;
    private static final String PAUSE_SCRIPT = """
      (() => {
          const target = window.__fongmiPlaybackTarget;
          if (!target || target.kind !== 'video' || !target.element.isConnected) return false;
          target.element.pause();
          return target.element.paused;
      })()
      """;
    private static final String PLAY_SCRIPT = """
      (() => {
          const target = window.__fongmiPlaybackTarget;
          if (!target || target.kind !== 'video' || !target.element.isConnected) return false;
          const promise = target.element.play();
          if (promise && promise.catch) promise.catch(() => {});
          return true;
      })()
      """;
    private static final String SEEK_SCRIPT = """
      (() => {
          const target = window.__fongmiPlaybackTarget;
          if (!target || target.kind !== 'video' || !target.element.isConnected || !Number.isFinite(target.element.duration)) return false;
          target.element.currentTime = %s;
          return true;
      })()
      """;
    private static final long TARGET_SCAN_DELAY = 2000;
    private static final long STARTUP_SCAN_DELAY = 250;
    private static final long STARTUP_POLL_DELAY = 100;
    private static final long STATE_POLL_DELAY = 500;

    private final Runnable targetTask = this::scanTarget;
    private final Runnable stateTask = this::pollState;
    @Nullable private final BrowserListener browserListener;
    @Nullable private final PlaybackListener listener;
    @Nullable private WebChromeClient.CustomViewCallback customViewCallback;
    @Nullable private View customView;
    private boolean released;
    private boolean targetReady;
    private boolean failureReported;
    private boolean challengeVisible;
    private boolean playbackPaused;
    private boolean playbackActive;
    private boolean awaitingPlaybackStart;
    private boolean videoPlaying;
    private boolean contentRevealed;
    @Nullable private PlaybackState playbackState;
    private final String downloadBridgeToken = UUID.randomUUID().toString();
    private int loadGeneration;
    private int pageGeneration;
    private String clickScript;
    private String defaultUserAgent;
    @Nullable private String previousPlaybackUrl;
    @Nullable private String requestedPlaybackUrl;
    private int downloadGeneration;

    public static BrowserWebView createBrowser(@NonNull Context context, String url, @NonNull BrowserListener listener) {
        BrowserWebView view = new BrowserWebView(context, null, listener);
        view.loadUrl(url);
        return view;
    }

    public static BrowserWebView createPlayback(@NonNull Context context, @NonNull PlaybackListener listener) {
        return new BrowserWebView(context, listener, null);
    }

    private BrowserWebView(@NonNull Context context, @Nullable PlaybackListener listener, @Nullable BrowserListener browserListener) {
        super(context);
        this.listener = listener;
        this.browserListener = browserListener;
        initSettings();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initSettings() {
        WebSettings setting = getSettings();
        setting.setSupportZoom(true);
        setting.setUseWideViewPort(true);
        setting.setDatabaseEnabled(true);
        setting.setDomStorageEnabled(true);
        setting.setJavaScriptEnabled(true);
        setting.setBuiltInZoomControls(true);
        setting.setDisplayZoomControls(false);
        setting.setLoadWithOverviewMode(true);
        String userAgent = Setting.getUa();
        if (userAgent.isEmpty() && isPlayback()) userAgent = Util.CHROME;
        if (!userAgent.isEmpty()) setting.setUserAgentString(userAgent);
        defaultUserAgent = setting.getUserAgentString();
        setting.setMediaPlaybackRequiresUserGesture(false);
        setting.setJavaScriptCanOpenWindowsAutomatically(false);
        setting.setAllowContentAccess(false);
        setting.setAllowFileAccess(false);
        setting.setMixedContentMode(isPlayback() ? WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE : WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager manager = CookieManager.getInstance();
        manager.setAcceptCookie(true);
        manager.setAcceptThirdPartyCookies(this, true);
        setBackgroundColor(Color.BLACK);
        setWebChromeClient(new ChromeClient());
        setWebViewClient(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new OreoClient() : new Client());
        if (browserListener != null) {
            addJavascriptInterface(new DownloadBridge(), DOWNLOAD_BRIDGE_NAME);
            setDownloadListener(this::onDownloadStart);
        }
    }

    public void loadPlayback(String url, @Nullable Map<String, String> headers, @Nullable String clickScript) {
        if (released || !isPlayback()) return;
        hideCustomView();
        setVisibility(View.VISIBLE);
        previousPlaybackUrl = getUrl();
        requestedPlaybackUrl = url;
        playbackActive = true;
        awaitingPlaybackStart = true;
        loadGeneration++;
        pageGeneration++;
        this.clickScript = clickScript;
        reportChallenge(false);
        failureReported = false;
        playbackPaused = false;
        resetPlaybackState();
        stopLoading();
        loadUrl(url, prepareHeaders(url, headers));
    }

    private Map<String, String> prepareHeaders(String url, @Nullable Map<String, String> headers) {
        Map<String, String> requestHeaders = new HashMap<>();
        String userAgent = defaultUserAgent;
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key == null || value == null || value.isEmpty()) continue;
                if (HttpHeaders.USER_AGENT.equalsIgnoreCase(key) || "ua".equalsIgnoreCase(key)) userAgent = value;
                else if (HttpHeaders.COOKIE.equalsIgnoreCase(key)) WebViewCookies.set(this, url, value);
                else requestHeaders.put(key, value);
            }
        }
        getSettings().setUserAgentString(userAgent);
        return requestHeaders;
    }

    private void scanTarget() {
        int load = loadGeneration;
        int page = pageGeneration;
        if (!isCurrentPlayback(load, page) || playbackPaused) return;
        evaluateJavascript(ACTIVATE_SCRIPT, value -> {
            if (!isCurrentPlayback(load, page) || playbackPaused) return;
            PlaybackState state = parseState(value);
            targetReady = state != null;
            if (state != null) {
                if (state.type() != PlaybackType.PAGE) reportChallenge(false);
                reportState(state);
                if (state.type() == PlaybackType.VIDEO && !state.ended()) {
                    scheduleStatePoll(state);
                }
            }
            removeCallbacks(targetTask);
            postDelayed(targetTask, state != null && state.type() != PlaybackType.PAGE ? TARGET_SCAN_DELAY : STARTUP_SCAN_DELAY);
        });
    }

    public void pauseVideo() {
        if (released || !playbackActive) return;
        playbackPaused = true;
        cancelPlaybackTasks();
        evaluateJavascript(PAUSE_SCRIPT, null);
        reportPlaying(false);
    }

    public void stopPlayback() {
        if (released || !playbackActive) return;
        hideCustomView();
        setVisibility(View.INVISIBLE);
        playbackActive = false;
        awaitingPlaybackStart = false;
        previousPlaybackUrl = null;
        requestedPlaybackUrl = null;
        loadGeneration++;
        pageGeneration++;
        reportChallenge(false);
        playbackPaused = true;
        resetPlaybackState();
        stopLoading();
        loadUrl(BLANK);
    }

    public void resumeVideo() {
        if (released || !playbackActive || failureReported) return;
        playbackPaused = false;
        cancelPlaybackTasks();
        evaluateJavascript(PLAY_SCRIPT, null);
        post(targetTask);
    }

    public void seekVideo(long positionMs) {
        PlaybackState state = playbackState;
        if (released || !playbackActive || state == null || !state.isSeekable()) return;
        long targetMs = Math.max(0, Math.min(positionMs, state.durationMs()));
        evaluateJavascript(SEEK_SCRIPT.replace("%s", Double.toString(targetMs / 1000.0)), null);
    }

    public boolean isVideoPlaying() {
        return playbackActive && videoPlaying;
    }

    public boolean isPlaybackPaused() {
        return playbackPaused;
    }

    public boolean isPlaybackActive() {
        return playbackActive;
    }

    public int getVideoWidth() {
        return playbackState == null ? 0 : playbackState.width();
    }

    public int getVideoHeight() {
        return playbackState == null ? 0 : playbackState.height();
    }

    public long getVideoPosition() {
        PlaybackState state = playbackState;
        return state == null || state.type() != PlaybackType.VIDEO ? -1 : state.positionMs();
    }

    public long getVideoDuration() {
        PlaybackState state = playbackState;
        return state == null || state.type() != PlaybackType.VIDEO ? -1 : state.durationMs();
    }

    public boolean hasMediaTarget() {
        return playbackActive && playbackState != null && playbackState.type() != PlaybackType.PAGE;
    }

    public boolean handleBackNavigation() {
        if (hideCustomView()) return true;
        if (isPlayback() || !canGoBack()) return false;
        goBack();
        return true;
    }

    public boolean hideCustomView() {
        View view = customView;
        WebChromeClient.CustomViewCallback callback = customViewCallback;
        if (view == null) return false;
        customView = null;
        customViewCallback = null;
        if (view.getParent() instanceof ViewGroup parent) parent.removeView(view);
        setVisibility(View.VISIBLE);
        if (callback != null) callback.onCustomViewHidden();
        if (listener != null) listener.onFullscreenChanged(this, false);
        requestFocus();
        return true;
    }

    void showCustomView(@Nullable View view, @NonNull WebChromeClient.CustomViewCallback callback) {
        if (released || challengeVisible || view == null || !(getParent() instanceof ViewGroup parent)) {
            callback.onCustomViewHidden();
            return;
        }
        hideCustomView();
        if (view.getParent() instanceof ViewGroup oldParent) oldParent.removeView(view);
        customView = view;
        customViewCallback = callback;
        if (listener != null) listener.onFullscreenChanged(this, true);
        int index = Math.max(0, parent.indexOfChild(this) + 1);
        parent.addView(view, index, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        view.setBackgroundColor(Color.BLACK);
        setVisibility(View.INVISIBLE);
        view.requestFocus();
    }

    private boolean isCurrentLoad(int generation) {
        return !released && !failureReported && generation == loadGeneration;
    }

    private boolean isCurrentPlaybackPage(WebView view, String url) {
        if (!isPlayback() || !playbackActive || url == null) return false;
        if (awaitingPlaybackStart) return url.equals(requestedPlaybackUrl);
        return url.equals(view.getUrl());
    }

    private boolean isCurrentPlayback(int load, int page) {
        return isPlayback() && playbackActive && !awaitingPlaybackStart && isCurrentPageLoad(load, page);
    }

    private boolean isCurrentPageLoad(int load, int page) {
        return isCurrentLoad(load) && page == pageGeneration;
    }

    private boolean isCurrentPage(int load, int page, String url) {
        return isCurrentPageLoad(load, page) && url != null && url.equals(getUrl());
    }

    private boolean isPlayback() {
        return listener != null;
    }

    private void cancelPlaybackTasks() {
        removeCallbacks(targetTask);
        removeCallbacks(stateTask);
    }

    private void resetPlaybackState() {
        targetReady = false;
        contentRevealed = false;
        playbackState = null;
        cancelPlaybackTasks();
        reportPlaying(false);
        setAlpha(0f);
    }

    private void postChallenge() {
        int load = loadGeneration;
        int page = pageGeneration;
        App.post(() -> {
            if (isCurrentPageLoad(load, page) && (!isPlayback() || (playbackActive && !awaitingPlaybackStart))) showChallenge();
        });
    }

    private void pollState() {
        int load = loadGeneration;
        int page = pageGeneration;
        if (!isCurrentPlayback(load, page) || playbackPaused || !targetReady) return;
        evaluateJavascript(STATE_SCRIPT, value -> {
            if (!isCurrentPlayback(load, page) || playbackPaused) return;
            PlaybackState state = parseState(value);
            if (state == null) {
                targetReady = false;
                removeCallbacks(targetTask);
                post(targetTask);
                return;
            }
            reportState(state);
            if (state.type() == PlaybackType.VIDEO && !state.ended()) scheduleStatePoll(state);
        });
    }

    private void scheduleStatePoll(@NonNull PlaybackState state) {
        removeCallbacks(stateTask);
        postDelayed(stateTask, state.started() ? STATE_POLL_DELAY : STARTUP_POLL_DELAY);
    }

    @Nullable
    private PlaybackState parseState(String value) {
        try {
            return value == null || "null".equals(value) ? null : PlaybackState.from(value);
        } catch (JSONException e) {
            return null;
        }
    }

    private void reportState(@NonNull PlaybackState state) {
        if (!challengeVisible) {
            if (state.shouldShowContent()) contentRevealed = true;
            setAlpha(contentRevealed ? 1f : 0f);
        }
        reportPlaying(state.playing());
        if (state.equals(playbackState)) return;
        playbackState = state;
        if (listener != null) listener.onStateChanged(this, state);
    }

    private void reportPlaying(boolean isPlaying) {
        if (videoPlaying == isPlaying) return;
        videoPlaying = isPlaying;
        if (listener != null) listener.onPlayingChanged(this, isPlaying);
    }

    private void reportFailure(int generation, boolean rendererGone) {
        if (!isCurrentLoad(generation)) return;
        failureReported = true;
        playbackActive = false;
        awaitingPlaybackStart = false;
        previousPlaybackUrl = null;
        requestedPlaybackUrl = null;
        cancelPlaybackTasks();
        reportPlaying(false);
        if (rendererGone) released = true;
        if (listener != null) listener.onFailure(this, rendererGone);
    }

    private static boolean isHttpScheme(@Nullable String scheme) {
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
        if (released || browserListener == null) return;
        int generation = ++downloadGeneration;
        if (!isSameOriginDownload(url, getUrl())) {
            reportDownloadError(generation, null, "native-origin");
            return;
        }
        if (contentLength > MAX_DOWNLOAD_BYTES) {
            reportDownloadError(generation, null, "native-size");
            return;
        }
        String key = Integer.toString(generation);
        String script = DOWNLOAD_READ_SCRIPT.replace("$KEY", JSONObject.quote(key)).replace("$URL", JSONObject.quote(url)).replace("$LIMIT", Integer.toString(MAX_DOWNLOAD_BYTES));
        String fileName = sanitizeFileName(URLUtil.guessFileName(url, contentDisposition, mimeType));
        long deadline = SystemClock.uptimeMillis() + DOWNLOAD_TIMEOUT;
        evaluateJavascript(script, value -> pollDownload(generation, key, fileName, mimeType, deadline));
    }

    private void installDownloadHook() {
        String script = DOWNLOAD_HOOK_SCRIPT.replace("$BRIDGE", JSONObject.quote(DOWNLOAD_BRIDGE_NAME)).replace("$TOKEN", JSONObject.quote(downloadBridgeToken)).replace("$LIMIT", Integer.toString(MAX_DOWNLOAD_BYTES));
        evaluateJavascript(script, null);
    }

    private void onBridgeDownload(String token, String pageUrl, String blobUrl, String fileName, String mimeType, String encoded) {
        if (!isTrustedBridgeDownload(token, pageUrl, blobUrl) || encoded == null) {
            SpiderDebug.log(TAG, "download bridge rejected");
            return;
        }
        int generation = ++downloadGeneration;
        try {
            deliverDownload(fileName, mimeType, encoded);
        } catch (JSONException e) {
            reportDownloadError(generation, null, "bridge-" + e.getMessage());
        }
    }

    private void onBridgeDownloadError(String token, String pageUrl, String blobUrl) {
        if (!isTrustedBridgeDownload(token, pageUrl, blobUrl)) {
            SpiderDebug.log(TAG, "download bridge error rejected");
            return;
        }
        int generation = ++downloadGeneration;
        reportDownloadError(generation, null, "bridge-read");
    }

    private boolean isTrustedBridgeDownload(String token, String pageUrl, String blobUrl) {
        return !released && browserListener != null && downloadBridgeToken.equals(token) && isSameOriginBlob(blobUrl, pageUrl) && isSameHttpOrigin(pageUrl, getUrl());
    }

    private void pollDownload(int generation, String key, String fileName, String mimeType, long deadline) {
        if (released || generation != downloadGeneration || browserListener == null) return;
        String script = DOWNLOAD_POLL_SCRIPT.replace("$KEY", JSONObject.quote(key));
        evaluateJavascript(script, value -> {
            if (released || generation != downloadGeneration || browserListener == null) return;
            if (value == null || "null".equals(value)) {
                if (SystemClock.uptimeMillis() >= deadline) {
                    reportDownloadError(generation, key, "native-timeout");
                } else {
                    postDelayed(() -> pollDownload(generation, key, fileName, mimeType, deadline), DOWNLOAD_POLL_DELAY);
                }
                return;
            }
            completeDownload(generation, key, fileName, mimeType, value);
        });
    }

    private void completeDownload(int generation, String key, String fileName, String reportedMimeType, String value) {
        try {
            String json = new JSONArray("[" + value + "]").getString(0);
            JSONObject result = new JSONObject(json);
            if (!"done".equals(result.optString("state"))) throw new JSONException("Download read failed");
            String mimeType = result.optString("type");
            if (mimeType.isEmpty()) mimeType = reportedMimeType;
            deliverDownload(fileName, mimeType, result.getString("data"));
        } catch (JSONException e) {
            reportDownloadError(generation, key, "native-" + e.getMessage());
        }
    }

    private void deliverDownload(String fileName, String mimeType, String encoded) throws JSONException {
        String normalized = normalizeDownloadMimeType(mimeType);
        if (normalized == null) throw new JSONException("Unsupported download type");
        byte[] data = decodeDownloadData(encoded);
        downloadGeneration++;
        browserListener.onDownload(new Download(sanitizeFileName(fileName), normalized, data));
    }

    static byte[] decodeDownloadData(@NonNull String encoded) throws JSONException {
        if (encoded.length() > MAX_DOWNLOAD_BASE64_CHARS) throw new JSONException("Download is too large");
        try {
            byte[] data = Base64.decode(encoded, Base64.DEFAULT);
            if (data.length > MAX_DOWNLOAD_BYTES) throw new JSONException("Download is too large");
            return data;
        } catch (IllegalArgumentException e) {
            throw new JSONException("Invalid download data");
        }
    }

    private void reportDownloadError(int generation, @Nullable String key, String reason) {
        if (generation != downloadGeneration || browserListener == null) return;
        downloadGeneration++;
        SpiderDebug.log(TAG, "download failed reason=%s", reason);
        if (key != null) {
            String script = "if (window.__fongmiDownloads) delete window.__fongmiDownloads[" + JSONObject.quote(key) + "]";
            evaluateJavascript(script, null);
        }
        browserListener.onDownloadError();
    }

    private static boolean isSameOriginBlob(@Nullable String blobUrl, @Nullable String pageUrl) {
        if (blobUrl == null || pageUrl == null || !blobUrl.startsWith("blob:")) return false;
        Uri blob = Uri.parse(blobUrl.substring(5));
        Uri page = Uri.parse(pageUrl);
        if (!isHttpScheme(blob.getScheme()) || !isHttpScheme(page.getScheme())) return false;
        return isSameHttpOrigin(blob, page);
    }

    static boolean isSameOriginDownload(@Nullable String downloadUrl, @Nullable String pageUrl) {
        if (downloadUrl == null) return false;
        return downloadUrl.startsWith("blob:") ? isSameOriginBlob(downloadUrl, pageUrl) : isSameHttpOrigin(downloadUrl, pageUrl);
    }

    static boolean isSameHttpOrigin(@Nullable String firstUrl, @Nullable String secondUrl) {
        if (firstUrl == null || secondUrl == null) return false;
        return isSameHttpOrigin(Uri.parse(firstUrl), Uri.parse(secondUrl));
    }

    private static boolean isSameHttpOrigin(@NonNull Uri first, @NonNull Uri second) {
        if (!isHttpScheme(first.getScheme()) || !isHttpScheme(second.getScheme())) return false;
        return first.getScheme().equalsIgnoreCase(second.getScheme()) && equalsIgnoreCase(first.getHost(), second.getHost()) && effectivePort(first) == effectivePort(second);
    }

    private static int effectivePort(@NonNull Uri uri) {
        if (uri.getPort() != -1) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static boolean equalsIgnoreCase(@Nullable String first, @Nullable String second) {
        return first != null && second != null && first.equalsIgnoreCase(second);
    }

    @Nullable
    static String normalizeDownloadMimeType(@Nullable String mimeType) {
        if (mimeType == null) return null;
        int parameter = mimeType.indexOf(';');
        String value = parameter < 0 ? mimeType : mimeType.substring(0, parameter);
        value = value.trim();
        if ("application/json".equalsIgnoreCase(value)) return "application/json";
        if ("text/plain".equalsIgnoreCase(value)) return "text/plain";
        return null;
    }

    static String sanitizeFileName(@Nullable String value) {
        if (value == null) return "config.json";
        String fileName = value.replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]", "_").trim();
        if (fileName.isEmpty()) return "config.json";
        return fileName.length() > 128 ? fileName.substring(0, 128) : fileName;
    }

    public void release() {
        if (released) return;
        hideCustomView();
        reportChallenge(false);
        playbackActive = false;
        awaitingPlaybackStart = false;
        previousPlaybackUrl = null;
        requestedPlaybackUrl = null;
        released = true;
        downloadGeneration++;
        cancelPlaybackTasks();
        if (browserListener != null) removeJavascriptInterface(DOWNLOAD_BRIDGE_NAME);
        stopLoading();
        loadUrl(BLANK);
        destroy();
    }

    private final class ChromeClient extends WebChromeClient {

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (browserListener == null) return false;
            browserListener.onShowFileChooser(callback, params);
            return true;
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            BrowserWebView.this.showCustomView(view, callback);
        }

        @Override
        public void onHideCustomView() {
            hideCustomView();
        }
    }

    private class Client extends WebViewClient {

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            boolean cloudflare = WebViewCloudflare.isResource(uri);
            if (WebViewCloudflare.isTurnstile(uri)) postChallenge();
            if (!cloudflare && WebViewRules.isAd(uri)) return WebViewRules.empty();
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        @SuppressLint("WebViewClientOnReceivedSslError")
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.cancel();
            if (isCurrentPlaybackPage(view, error.getUrl())) reportFailure(loadGeneration, false);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame() && isCurrentPlaybackPage(view, request.getUrl().toString())) reportFailure(loadGeneration, false);
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            if (WebViewCloudflare.isChallenge(errorResponse)) {
                postChallenge();
                return;
            }
            if (request.isForMainFrame() && isCurrentPlaybackPage(view, request.getUrl().toString())) reportFailure(loadGeneration, false);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (isHttpScheme(request.getUrl().getScheme())) return false;
            if (browserListener != null && request.isForMainFrame() && request.hasGesture()) browserListener.onOpenExternal(request.getUrl());
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            if (isPlayback()) {
                if (!playbackActive) return;
                if (awaitingPlaybackStart) {
                    if (url != null && url.equals(previousPlaybackUrl) && !url.equals(requestedPlaybackUrl)) return;
                    awaitingPlaybackStart = false;
                    previousPlaybackUrl = null;
                    requestedPlaybackUrl = null;
                } else if (url == null || !url.equals(view.getUrl())) {
                    return;
                }
            }
            pageGeneration++;
            if (!isPlayback()) return;
            resetPlaybackState();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (BLANK.equals(url)) return;
            if (isPlayback() && !isCurrentPlaybackPage(view, url)) return;
            if (browserListener != null) installDownloadHook();
            int load = loadGeneration;
            int page = pageGeneration;
            evaluateJavascript(WebViewCloudflare.CHECK_SCRIPT, value -> onChallengeChecked(url, load, page, value));
        }

        private void onChallengeChecked(String url, int load, int page, String value) {
            if (!isCurrentPage(load, page, url)) return;
            if ("true".equalsIgnoreCase(value)) {
                showChallenge();
                return;
            }
            reportChallenge(false);
            WebViewRules.apply(BrowserWebView.this, Uri.parse(url), clickScript, () -> isCurrentPage(load, page, url));
            if (!isPlayback()) return;
            setAlpha(0f);
            if (playbackPaused) return;
            removeCallbacks(targetTask);
            post(targetTask);
        }
    }

    private final class DownloadBridge {

        @JavascriptInterface
        public void onDownload(String token, String pageUrl, String blobUrl, String fileName, String mimeType, String encoded) {
            post(() -> onBridgeDownload(token, pageUrl, blobUrl, fileName, mimeType, encoded));
        }

        @JavascriptInterface
        public void onError(String token, String pageUrl, String blobUrl) {
            post(() -> onBridgeDownloadError(token, pageUrl, blobUrl));
        }
    }

    void showChallenge() {
        if (released) return;
        if (!challengeVisible) {
            SpiderDebug.log(TAG, "cloudflare=visible");
            reportChallenge(true);
        }
        setAlpha(1f);
        requestFocus();
    }

    private void reportChallenge(boolean visible) {
        if (challengeVisible == visible) return;
        challengeVisible = visible;
        if (listener != null) listener.onChallengeChanged(this, visible);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private final class OreoClient extends Client {

        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            hideCustomView();
            if (isPlayback()) {
                if (!released) reportFailure(loadGeneration, true);
            } else if (!released) {
                released = true;
                downloadGeneration++;
                if (browserListener != null) browserListener.onRendererGone(BrowserWebView.this);
            }
            destroy();
            return true;
        }
    }
}
