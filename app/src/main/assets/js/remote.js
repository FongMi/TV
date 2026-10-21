function readRemoteState(body) {
    if (!body || typeof body.active !== 'boolean') throw new Error('Invalid playback response');
    if (!body.active) return body;
    if (typeof body.canSetSpeed !== 'boolean' || !Number.isFinite(body.speed) || body.speed <= 0 || typeof body.mediaKey !== 'string' || !/^[0-9a-f]{32}$/.test(body.mediaKey)) throw new Error('Invalid playback response');
    if (typeof body.title !== 'string' || typeof body.subtitle !== 'string' || typeof body.artwork !== 'string' || !Number.isSafeInteger(body.position) || body.position < 0 || !Number.isSafeInteger(body.duration) || body.duration < 0 || ![1, 2, 3, 6].includes(body.state) || typeof body.playWhenReady !== 'boolean' || typeof body.repeatOne !== 'boolean' || typeof body.live !== 'boolean' || typeof body.canNavigate !== 'boolean' || typeof body.canSeek !== 'boolean') throw new Error('Invalid playback response');
    return body;
}

function playbackTime(millis) {
    const seconds = Math.floor(millis / 1000);
    const minutes = Math.floor(seconds / 60);
    return (minutes >= 60 ? Math.floor(minutes / 60) + ':' + String(minutes % 60).padStart(2, '0') : minutes) + ':' + String(seconds % 60).padStart(2, '0');
}

function artworkUrl(value) {
    if (!value) return '';
    try {
        const url = new URL(value, window.location.href);
        return url.origin === window.location.origin && url.pathname === '/remote/artwork' ? url.href : '';
    } catch {
        return '';
    }
}

function artworkTint(pixels) {
    const colors = new Map();
    for (let i = 0; i < pixels.length; i += 4) {
        const r = pixels[i], g = pixels[i + 1], b = pixels[i + 2];
        const high = Math.max(r, g, b), low = Math.min(r, g, b);
        const chroma = high - low, saturation = chroma / high;
        if (pixels[i + 3] < 128 || high < 40 || saturation < 0.3) continue;
        let hue = high === r ? (g - b) / chroma : high === g ? (b - r) / chroma + 2 : (r - g) / chroma + 4;
        hue = (hue * 60 + 360) % 360;
        const key = Math.floor(hue / 15), weight = saturation * saturation;
        const color = colors.get(key) || { score: 0, r: 0, g: 0, b: 0 };
        color.score += weight;
        color.r += r * weight;
        color.g += g * weight;
        color.b += b * weight;
        colors.set(key, color);
    }
    let selected;
    for (const color of colors.values()) {
        if (!selected || color.score > selected.score) selected = color;
    }
    if (!selected) return '';
    const channels = [selected.r, selected.g, selected.b];
    const high = Math.max(...channels), low = Math.min(...channels);
    const accent = channels.map(value => (value - low) / (high - low));
    const luminance = accent.reduce((sum, value, index) => sum + (value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4) * [0.2126, 0.7152, 0.0722][index], 0);
    const scale = Math.min(1, (0.35 / luminance) ** (1 / 2.2));
    return accent.map(value => Math.round(value * scale * 255)).join(', ');
}

class RemoteControl {
    constructor(update) {
        this.update = update;
        this.state = {};
        this.error = false;
        this.pending = null;
        this.command = '';
        this.timer = null;
        this.generation = 0;
        this.visible = true;
        this.stale = false;
    }

    cancel() {
        this.generation++;
        clearTimeout(this.timer);
        this.timer = null;
        this.pending?.abort();
        this.pending = null;
        this.command = '';
    }

    setVisible(visible) {
        if (this.visible === visible) return;
        this.cancel();
        this.visible = visible;
        this.stale = true;
        if (visible) this.request();
        else this.update();
    }

    refresh() {
        if (!this.visible || this.command || this.pending && (this.stale || this.state.active === undefined)) return;
        this.cancel();
        this.stale = true;
        this.request();
    }

    async request(type, value) {
        if (!this.visible || type && (!this.state.active || this.error || this.stale)) return;
        if (type === 'seek' && (!this.state.canSeek || !Number.isSafeInteger(value) || value < 0)) return;
        if (type === 'speed' && (!this.state.canSetSpeed || !Number.isFinite(value) || value < 0.1 || value > 5)) return;
        if (this.pending) {
            if (!type || this.command) return;
            this.cancel();
        }
        clearTimeout(this.timer);
        const generation = this.generation;
        const controller = new AbortController();
        this.pending = controller;
        this.command = type || '';
        if (type || this.error || this.stale && this.state.active || this.state.active === undefined) {
            this.error = false;
            this.update();
        }
        const timeout = setTimeout(() => controller.abort(), 5000);
        try {
            const params = type === 'seek' ? { type, position: value } : type === 'speed' ? { type, speed: value } : { type };
            const options = type ? { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams(params).toString() } : { method: 'GET' };
            const response = await fetch('/remote', { ...options, cache: 'no-store', signal: controller.signal });
            if (!response.ok) throw new Error('Playback request failed');
            const body = readRemoteState(await response.json());
            if (generation === this.generation) { this.state = body; this.stale = false; }
        } catch (error) {
            if (generation === this.generation) { this.error = true; this.stale = true; }
        } finally {
            clearTimeout(timeout);
            if (generation === this.generation) {
                this.pending = null;
                this.command = '';
                this.update();
                if (this.visible && !this.error) this.timer = setTimeout(() => this.request(), 1000);
            }
        }
    }
}

function initRemote() {
    const panel = document.getElementById('panel2');
    const mediaPanel = document.getElementById('panel3');
    const card = document.getElementById('remote_card');
    const artwork = document.getElementById('remote_artwork');
    const prev = document.getElementById('remote_prev');
    const toggle = document.getElementById('remote_toggle');
    const next = document.getElementById('remote_next');
    const stop = document.getElementById('remote_stop');
    const repeat = document.getElementById('remote_repeat');
    const retry = document.getElementById('remote_retry');
    const deviceRetry = document.getElementById('device_retry');
    const progress = document.getElementById('remote_bar');
    const track = document.getElementById('remote_track');
    const elapsed = document.getElementById('remote_elapsed');
    const duration = document.getElementById('remote_duration');
    const speeds = [...document.getElementById('remote_speed').querySelectorAll('button')];
    const subtitle = document.getElementById('remote_subtitle');
    const uploader = document.getElementById('subtitle_uploader');
    const danmakuUrl = document.getElementById('danmaku_url');
    const danmakuApply = document.getElementById('danmaku_apply');
    const danmakuUpload = document.getElementById('danmaku_upload');
    const danmakuUploader = document.getElementById('danmaku_uploader');
    const seeks = [[document.getElementById('remote_back30'), -30000], [document.getElementById('remote_back10'), -10000], [document.getElementById('remote_forward10'), 10000], [document.getElementById('remote_forward30'), 30000]];
    let seekPosition = null;
    let applying = false;
    const clearArtwork = () => {
        artwork.hidden = true;
        card.dataset.artwork = 'empty';
        card.style.removeProperty('--remote-tint');
    };
    const renderProgress = () => {
        const total = control.state.duration || 0;
        const position = Math.min(seekPosition ?? control.state.position ?? 0, total);
        if (Number(progress.max) !== (total || 1)) progress.max = String(total || 1);
        if (Number(progress.value) !== position) progress.value = String(position);
        const fill = (total ? position / total * 100 : 0) + '%';
        if (track.style.getPropertyValue('--remote-fill') !== fill) track.style.setProperty('--remote-fill', fill);
        const currentTime = playbackTime(position), totalTime = playbackTime(total);
        if (elapsed.textContent !== currentTime) elapsed.textContent = currentTime;
        if (duration.textContent !== totalTime) duration.textContent = totalTime;
        const valueText = currentTime + ' / ' + totalTime;
        if (progress.getAttribute('aria-valuetext') !== valueText) progress.setAttribute('aria-valuetext', valueText);
    };
    const control = new RemoteControl(() => {
        const state = control.state;
        const title = document.getElementById('remote_title');
        if (title.textContent !== (state.title || t('remoteTitle'))) title.textContent = state.title || t('remoteTitle');
        if (title.title !== title.textContent) title.title = title.textContent;
        const subtitleLabel = document.getElementById('remote_subtitle_text');
        const subtitleText = state.active ? state.subtitle : '';
        if (subtitleLabel.textContent !== subtitleText) subtitleLabel.textContent = subtitleText;
        if (subtitleLabel.title !== subtitleText) subtitleLabel.title = subtitleText;
        subtitleLabel.hidden = !subtitleText;
        const status = control.error ? 'remoteOffline' : !state.active ? control.pending ? 'remoteConnecting' : 'remoteIdle' : state.state === 1 ? 'remoteEnded' : state.state === 6 ? 'remoteBuffering' : state.playWhenReady ? 'remotePlaying' : 'remotePaused';
        const label = document.getElementById('remote_state');
        if (label.textContent !== t(status)) label.textContent = t(status);
        const source = state.active ? artworkUrl(state.artwork) : '';
        const layout = state.active ? state.live ? 'live' : 'vod' : state.active === undefined && !control.error ? 'loading' : 'idle';
        if (card.dataset.layout !== layout) card.dataset.layout = layout;
        const busy = String(layout === 'loading');
        if (card.getAttribute('aria-busy') !== busy) card.setAttribute('aria-busy', busy);
        if (artwork.dataset.source !== source) {
            artwork.dataset.source = source;
            if (source) {
                if (artwork.hidden) card.dataset.artwork = 'loading';
                artwork.src = source;
            }
            else {
                artwork.removeAttribute('src');
                clearArtwork();
            }
        }
        if (layout === 'loading') card.dataset.artwork = 'loading';
        else if (!source && card.dataset.artwork !== 'empty') clearArtwork();
        document.getElementById('remote_progress').hidden = !state.active || state.live || !state.duration;
        if (progress.getAttribute('aria-label') !== t('remoteProgress')) progress.setAttribute('aria-label', t('remoteProgress'));
        if (!state.active || !state.canSeek || control.error) seekPosition = null;
        renderProgress();
        const playing = String(control.visible && !control.stale && !!state.active && state.state === 3 && state.playWhenReady && !state.live && state.duration > 0 && !control.error);
        if (track.dataset.playing !== playing) track.dataset.playing = playing;
        const disabled = !state.active || control.error || control.stale;
        deviceRetry.dataset.playbackError = String(control.visible && control.error);
        deviceRetry.hidden = deviceRetry.dataset.playbackError !== 'true' && document.getElementById('device_status').dataset.error !== 'true';
        for (const [button, available] of [[progress, state.canSeek], ...seeks.map(([button]) => [button, state.canSeek]), ...speeds.map(button => [button, state.canSetSpeed]), [prev, state.canNavigate], [next, state.canNavigate], [toggle, true], [stop, true], [repeat, !state.live]]) {
            const pending = String(!disabled && !!available && !!control.command);
            if (button.dataset.pending !== pending) button.dataset.pending = pending;
            button.disabled = disabled || !available || !!control.command;
        }
        for (const button of [subtitle, danmakuUpload, danmakuApply]) {
            button.dataset.pending = String(!disabled && !state.live && !window.uploadTransfer?.busy && (applying || !!control.command));
            button.disabled = disabled || state.live || applying || !!control.command || !!window.uploadTransfer?.busy;
        }
        danmakuUrl.disabled = applying;
        const mediaStatus = document.getElementById('media_status');
        const mediaMessage = control.error ? t('remoteOffline') : !state.active ? t('mediaPlaybackRequired') : state.live ? t('mediaVodOnly') : [state.title, state.subtitle].filter(Boolean).join(' · ');
        if (mediaStatus.textContent !== mediaMessage) mediaStatus.textContent = mediaMessage;
        mediaStatus.dataset.error = String(control.error);
        document.getElementById('remote_actions').hidden = !state.active;
        document.getElementById('remote_extra').hidden = !state.active || state.live;
        for (const button of speeds) button.setAttribute('aria-pressed', String(!!state.active && state.speed === Number(button.value)));
        repeat.hidden = !!state.live;
        if (repeat.getAttribute('aria-pressed') !== String(!!state.repeatOne)) repeat.setAttribute('aria-pressed', String(!!state.repeatOne));
        const repeatIcon = repeat.querySelector('use');
        const repeatSource = '#remote-icon-' + (state.repeatOne ? 'repeat_one' : 'repeat_off');
        if (repeatIcon.getAttribute('href') !== repeatSource) repeatIcon.setAttribute('href', repeatSource);
        const toggleIcon = toggle.querySelector('use');
        const toggleSource = '#remote-icon-' + (state.playWhenReady ? 'pause' : 'play');
        if (toggleIcon.getAttribute('href') !== toggleSource) toggleIcon.setAttribute('href', toggleSource);
        for (const [button, key] of [[prev, state.live ? 'remotePrevChannel' : 'remotePrev'], [next, state.live ? 'remoteNextChannel' : 'remoteNext'], [toggle, state.playWhenReady ? 'remotePause' : 'remotePlay'], [stop, 'remoteStop'], [repeat, 'remoteRepeat']]) {
            if (button.getAttribute('aria-label') !== t(key)) button.setAttribute('aria-label', t(key));
            if (button.title !== t(key)) button.title = t(key);
        }
        retry.hidden = !control.error;
    });
    artwork.addEventListener('load', () => {
        if (!artwork.getAttribute('src') || artwork.currentSrc !== artwork.dataset.source) return;
        artwork.hidden = false;
        card.dataset.artwork = 'ready';
        const canvas = document.createElement('canvas');
        canvas.width = canvas.height = 32;
        const context = canvas.getContext('2d');
        if (!context) return;
        context.drawImage(artwork, 0, 0, 32, 32);
        const tint = artworkTint(context.getImageData(0, 0, 32, 32).data);
        if (tint) card.style.setProperty('--remote-tint', tint);
        else card.style.removeProperty('--remote-tint');
    });
    artwork.addEventListener('error', () => {
        if (artwork.currentSrc && artwork.currentSrc !== artwork.dataset.source) return;
        clearArtwork();
    });
    prev.addEventListener('click', () => control.request('prev'));
    next.addEventListener('click', () => control.request('next'));
    stop.addEventListener('click', () => control.request('stop'));
    toggle.addEventListener('click', () => control.request(control.state.playWhenReady ? 'pause' : 'play'));
    repeat.addEventListener('click', () => control.request(control.state.repeatOne ? 'repeat_off' : 'repeat_on'));
    retry.addEventListener('click', () => control.request());
    deviceRetry.addEventListener('click', () => control.refresh());
    for (const [button, delta] of seeks) button.addEventListener('click', () => control.request('seek', Math.max(0, Math.min(control.state.duration, control.state.position + delta))));
    for (const button of speeds) button.addEventListener('click', () => control.request('speed', Number(button.value)));
    subtitle.addEventListener('click', () => uploader.click());
    uploader.addEventListener('change', () => {
        const file = uploader.files[0];
        uploader.value = '';
        if (!file) return;
        if (!/\.(srt|ssa|ass)$/i.test(file.name)) { warnToast(t('subtitleFormat')); return; }
        return applyMediaSource('subtitle', '/' + file.name, file);
    });
    danmakuUpload.addEventListener('click', () => danmakuUploader.click());
    danmakuUploader.addEventListener('change', () => {
        const file = danmakuUploader.files[0];
        danmakuUploader.value = '';
        if (file) return applyMediaSource('danmaku', '/' + file.name, file);
    });
    danmakuApply.addEventListener('click', () => {
        let url;
        try {
            url = new URL(danmakuUrl.value.trim());
            if (!['http:', 'https:'].includes(url.protocol)) throw new Error('Invalid URL');
        } catch {
            warnToast(t('danmakuUrlInvalid')); danmakuUrl.focus(); return;
        }
        return applyMediaSource('danmaku', url.href);
    });
    async function applyMediaSource(type, path, file) {
        if (applying) return;
        const failed = type === 'subtitle' ? 'subtitleApplyFailed' : 'danmakuApplyFailed';
        if (!control.state.active || control.state.live || control.error || control.stale || control.command) {warnToast(t(failed)); return;}
        if (window.uploadTransfer.busy) {warnToast(t('uploadBusy')); return;}
        const key = control.state.mediaKey;
        applying = true; control.update();
        try {
            if (file && !await window.uploadTransfer.send([file], '')) return;
            if (file && !window.uploadTransfer.files.some(entry => entry.file === file && entry.status === 'uploadDone' && !entry.cancelled)) return;
            const applied = await applyPlaybackMedia(type, path, key);
            warnToast(t(applied ? type === 'subtitle' ? 'subtitleApplied' : 'danmakuApplied' : failed));
        } catch {
            warnToast(t(failed));
        } finally {
            applying = false; control.update(); control.request();
        }
    }
    document.addEventListener('uploadchange', () => control.update());
    const commitSeek = async () => {
        if (seekPosition === null || progress.disabled) return;
        const request = control.request('seek', Number(progress.value));
        const generation = control.generation;
        await request;
        if (generation !== control.generation) return;
        seekPosition = null;
        control.update();
    };
    progress.addEventListener('input', () => {
        if (progress.disabled) return;
        seekPosition = Number(progress.value);
        renderProgress();
    });
    progress.addEventListener('change', commitSeek);
    progress.addEventListener('keydown', event => {
        if (progress.disabled || !['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown'].includes(event.key)) return;
        event.preventDefault();
        const delta = event.key === 'ArrowLeft' || event.key === 'ArrowDown' ? -5000 : 5000;
        seekPosition = Math.max(0, Math.min(Number(progress.max), Number(progress.value) + delta));
        renderProgress();
        commitSeek();
    });
    for (const event of ['pointercancel', 'blur']) progress.addEventListener(event, () => {
        if (control.command === 'seek') return;
        seekPosition = null;
        renderProgress();
    });
    const updateVisibility = () => {
        const visible = !document.hidden && (panel.classList.contains('active') || mediaPanel.classList.contains('active'));
        if (!visible) seekPosition = null;
        control.setVisible(visible);
    };
    document.addEventListener('panelchange', updateVisibility);
    document.addEventListener('visibilitychange', updateVisibility);
    window.addEventListener('pagehide', () => { seekPosition = null; control.setVisible(false); });
    const refresh = () => { updateVisibility(); seekPosition = null; control.refresh(); };
    window.addEventListener('pageshow', event => { if (event.persisted) refresh(); });
    window.addEventListener('focus', refresh);
    window.addEventListener('online', refresh);
    updateVisibility();
    control.request();
}

async function applyPlaybackMedia(type, path, key) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 5000);
    try {
        const response = await fetch('/remote/' + type, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams({ path, key }).toString(), signal: controller.signal });
        return response.ok;
    } catch (error) {
        return false;
    } finally {
        clearTimeout(timeout);
    }
}

if (typeof module !== 'undefined' && module.exports) module.exports = { readRemoteState, playbackTime, artworkUrl, artworkTint, RemoteControl, applyPlaybackMedia };
else initRemote();
