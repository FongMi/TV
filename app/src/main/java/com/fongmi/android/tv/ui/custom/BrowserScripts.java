package com.fongmi.android.tv.ui.custom;

final class BrowserScripts {

    static final String DOWNLOAD_HOOK = """
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
    static final String DOWNLOAD_READ = """
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
    static final String DOWNLOAD_POLL = """
      (() => {
          const store = window.__fongmiDownloads;
          const result = store && store[$KEY];
          if (!result || result.state === 'pending') return null;
          delete store[$KEY];
          return JSON.stringify(result);
      })()
      """;
    private static final String PLAYBACK_RUNTIME = """
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
                      window.__fongmiPlaybackDirty = true;
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
                  window.__fongmiPlaybackDirty = true;
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
    static final String ACTIVATE = "(() => {" + PLAYBACK_RUNTIME + """
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
              const changedPlaybackRecord = record => {
                  if (record.type === 'attributes') return hasPlaybackCandidate(record.target);
                  if (record.type !== 'childList') return false;
                  for (const node of [...record.addedNodes, ...record.removedNodes]) {
                      if (hasPlaybackCandidate(node)) return true;
                  }
                  return false;
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
                          if (!changedPlaybackRecord(record)) continue;
                          window.__fongmiPlaybackDirty = true;
                          return;
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
              const inspectVideo = (video, containers) => {
                  const ancestors = [...containers, ...shadowHosts(video)];
                  if (!hiddenVideo) hiddenVideo = {kind: 'video', element: video, containers: [...ancestors, video]};
                  const area = effectiveArea(video, ancestors);
                  if (area > bestVideoArea) {
                      bestVideo = {kind: 'video', element: video, containers: [...ancestors, video]};
                      bestVideoArea = area;
                  }
              };
              const frameScore = (frame, area) => {
                  const rect = frame.getBoundingClientRect();
                  const ratio = rect.height > 0 ? rect.width / rect.height : 0;
                  const attributes = `${frame.id} ${frame.className} ${frame.title} ${frame.name} ${frame.allow} ${frame.src}`.toLowerCase();
                  const signaled = frame.allowFullscreen || /(autoplay|fullscreen|media|player|video|stream|live|embed|play)/.test(attributes);
                  const viewport = Math.max(1, frame.ownerDocument.defaultView.innerWidth * frame.ownerDocument.defaultView.innerHeight);
                  const plausibleRatio = ratio >= 0.45 && ratio <= 2.5;
                  const substantial = area >= viewport * 0.45;
                  const largeEnough = area >= viewport * 0.08;
                  if (!plausibleRatio || !largeEnough || (!signaled && !substantial)) return -1;
                  return area + (signaled ? viewport : 0);
              };
              const inspectFrame = (frame, containers) => {
                  const frameAncestors = [...containers, ...shadowHosts(frame)];
                  const frameContainers = [...frameAncestors, frame];
                  let childDocument = null;
                  try {
                      childDocument = frame.contentDocument;
                  } catch (ignored) {
                  }
                  if (childDocument && childDocument.documentElement) inspectDocument(childDocument, frameContainers);
                  const area = effectiveArea(frame, frameAncestors);
                  if (area <= 0) return;
                  const score = frameScore(frame, area);
                  if (score > bestFrameScore) {
                      bestFrame = {kind: 'frame', element: frame, containers: frameContainers};
                      bestFrameScore = score;
                  }
              };
              const inspectDocument = (doc, containers) => {
                  if (!doc || !doc.documentElement) return;
                  walkRoots(doc, root => {
                      observe(root);
                      for (const video of root.querySelectorAll('video')) inspectVideo(video, containers);
                      for (const frame of root.querySelectorAll('iframe')) inspectFrame(frame, containers);
                  });
              };
              inspectDocument(document, []);
              const target = bestVideo || bestFrame || hiddenVideo;
              const changed = !previous || !target || previous.kind !== target.kind || previous.element !== target.element;
              window.__fongmiPlaybackTarget = target;
              if (!target) return stateOf(null);
              if (target.kind === 'video' && changed) {
                  const promise = target.element.play();
                  if (promise && promise.catch) promise.catch(() => {});
              }
              const state = stateOf(target);
              const waiting = target.kind === 'video' && !state[8];
              const containers = [...target.containers];
              const presentation = waiting ? target.element.parentElement || target.element.getRootNode().host || target.element : target.element;
              containers[containers.length - 1] = presentation;
              const styled = window.__fongmiPlaybackStyled;
              const remember = element => {
                  if (!styled.some(entry => entry.element === element)) styled.push({element, style: element.getAttribute('style')});
              };
              const hideSibling = sibling => {
                  remember(sibling);
                  sibling.style.setProperty('visibility', 'hidden', 'important');
                  sibling.style.setProperty('opacity', '0', 'important');
                  sibling.style.setProperty('pointer-events', 'none', 'important');
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
                              hideSibling(sibling);
                          }
                          current = parent;
                      } else {
                          const root = current.getRootNode ? current.getRootNode() : null;
                          current = root && root.host ? root.host : null;
                      }
                  }
              };
              const fillViewport = style => {
                  style.setProperty('position', 'fixed', 'important');
                  style.setProperty('inset', '0', 'important');
                  style.setProperty('width', '100vw', 'important');
                  style.setProperty('height', '100vh', 'important');
                  style.setProperty('max-width', 'none', 'important');
                  style.setProperty('max-height', 'none', 'important');
                  style.setProperty('margin', '0', 'important');
                  style.setProperty('padding', '0', 'important');
                  style.setProperty('z-index', '2147483647', 'important');
              };
              const styleContainer = element => {
                  isolate(element);
                  remember(element);
                  const style = element.style;
                  fillViewport(style);
                  if (!waiting || element !== presentation) style.setProperty('background', '#000', 'important');
                  style.setProperty('transform', 'none', 'important');
                  if (target.kind === 'video' && element === target.element) {
                      style.setProperty('display', 'block', 'important');
                      style.setProperty('object-fit', 'contain', 'important');
                  }
              };
              const lockOverflow = element => {
                  if (!element) return;
                  remember(element);
                  element.style.setProperty('overflow', 'hidden', 'important');
              };
              const documents = new Set();
              for (const element of containers) {
                  styleContainer(element);
                  documents.add(element.ownerDocument);
              }
              for (const doc of documents) {
                  lockOverflow(doc.documentElement);
                  lockOverflow(doc.body);
              }
              return stateOf(target);
          })()
          """;
    static final String STATE = """
      (() => {
          const runtime = window.__fongmiPlaybackRuntime;
          return runtime ? runtime.stateOf(window.__fongmiPlaybackTarget || null) : null;
      })()
      """;
    static final String PAUSE = """
      (() => {
          const target = window.__fongmiPlaybackTarget;
          if (!target || target.kind !== 'video' || !target.element.isConnected) return false;
          target.element.pause();
          return target.element.paused;
      })()
      """;
    static final String PLAY = """
      (() => {
          const target = window.__fongmiPlaybackTarget;
          if (!target || target.kind !== 'video' || !target.element.isConnected) return false;
          const promise = target.element.play();
          if (promise && promise.catch) promise.catch(() => {});
          return true;
      })()
      """;
    static final String SEEK = """
      (() => {
          const target = window.__fongmiPlaybackTarget;
          if (!target || target.kind !== 'video' || !target.element.isConnected || !Number.isFinite(target.element.duration)) return false;
          target.element.currentTime = %s;
          return true;
      })()
      """;

    private BrowserScripts() {
    }
}
