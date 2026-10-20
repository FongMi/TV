const LOG_MAX_LINES = 5000;
const LOG_MAX_BYTES = 512 * 1024;
const LOG_PLAYER_STATES = ['UNKNOWN', 'IDLE', 'BUFFERING', 'READY', 'ENDED'];
const logEncoder = new TextEncoder();

function readLogPage(page, after) {
    if (!page || !Array.isArray(page.lines) || page.lines.length > 200 || !Number.isSafeInteger(page.next) || page.next < 0 || typeof page.backlog !== 'boolean' || typeof page.gap !== 'boolean' || !Number.isSafeInteger(page.remainingMillis) || page.remainingMillis < 0) throw new Error('Invalid log response');
    let previous = after === undefined ? 0 : after;
    for (const line of page.lines) {
        if (!line || !Number.isSafeInteger(line.seq) || line.seq <= previous || typeof line.text !== 'string' || logEncoder.encode(line.text).length > 16 * 1024) throw new Error('Invalid log line');
        previous = line.seq;
    }
    if (page.next !== previous || (page.backlog && !page.lines.length)) throw new Error('Invalid log cursor');
    return page;
}

class LogBuffer {
    constructor() {
        this.revision = 0;
        this.clear();
    }

    clear(markMissing = false) {
        const trimmed = markMissing && (this.trimmed || this.lines.length > 0);
        this.lines = [];
        this.bytes = 0;
        this.trimmed = trimmed;
        this.revision++;
    }

    append(lines) {
        if (!lines.length) return;
        for (const line of lines) {
            const bytes = logEncoder.encode(line.text + '\n').length;
            this.lines.push({ seq: line.seq, text: line.text, bytes });
            this.bytes += bytes;
        }
        let removed = 0;
        while (this.lines.length - removed > LOG_MAX_LINES || this.bytes > LOG_MAX_BYTES) {
            this.bytes -= this.lines[removed++].bytes;
            this.trimmed = true;
        }
        if (removed) this.lines.splice(0, removed);
        this.revision++;
    }

    text(search = '', level = '') {
        search = search.toLowerCase();
        return this.lines.filter(line => (!search || line.text.toLowerCase().includes(search)) && (!level || /^\S+\s+\S+\s+\d+\s+\d+\s+([VDIWEFS])\s/.exec(line.text)?.[1] === level)).map(line => line.text).join('\n');
    }
}

function readLogInfo(body) {
    if (!body || typeof body.version !== 'string' || !Number.isSafeInteger(body.versionCode) || typeof body.device !== 'string' || typeof body.android !== 'string' || !Number.isSafeInteger(body.sdk) || !body.playback || typeof body.playback.active !== 'boolean' || typeof body.playback.url !== 'string' || logEncoder.encode(JSON.stringify(body, null, 2)).length > LOG_MAX_BYTES - 1024) throw new Error('Invalid log info');
    return body;
}

function logReportInfo(info) {
    const body = JSON.parse(JSON.stringify(info));
    const current = body.playbackLog;
    const error = body.lastPlaybackError;
    if (current && error) {
        const shared = {};
        for (const [key, value] of Object.entries(current)) {
            if (key === 'capturedAtMillis' || key === 'elapsedRealtimeMs' || JSON.stringify(value) !== JSON.stringify(error[key])) continue;
            shared[key] = value;
            delete current[key];
            delete error[key];
        }
        if (Object.keys(shared).length) body.sharedPlayerFields = shared;
    }
    const urlFrom = body.sharedPlayerFields?.url !== undefined ? 'sharedPlayerFields' : 'playbackLog';
    const source = body[urlFrom];
    if (source && typeof source.url === 'string' && body.playback?.url === source.url && body.playback.urlTruncated === source.urlTruncated) {
        delete body.playback.url;
        delete body.playback.urlTruncated;
        body.playback.urlFrom = urlFrom;
    }
    return '{\n' + Object.entries(body).map(([key, value]) => '  ' + JSON.stringify(key) + ': ' + JSON.stringify(value)).join(',\n') + '\n}';
}

function logPlayerSummary(snapshot) {
    if (!snapshot) return 'not captured';
    const parts = [];
    if (snapshot.capturedAtMillis !== undefined) parts.push('capturedAt=' + new Date(snapshot.capturedAtMillis).toISOString());
    if (snapshot.playerPresent === false) parts.push('no player available');
    if (snapshot.playbackId) parts.push('playbackId=' + snapshot.playbackId + '; attempt=' + snapshot.playbackAttempt);
    if (snapshot.engine) parts.push('engine=' + snapshot.engine);
    if (snapshot.playerId) parts.push('playerId=' + snapshot.playerId);
    if (snapshot.playbackState !== undefined) parts.push('state=' + (LOG_PLAYER_STATES[snapshot.playbackState] || 'UNKNOWN') + ' (' + snapshot.playbackState + ')');
    if (snapshot.positionMs !== undefined) parts.push('positionMs=' + snapshot.positionMs);
    if (snapshot.errorCodeName || snapshot.errorCode !== undefined) parts.push('error=' + (snapshot.errorCodeName || snapshot.errorCode));
    return parts.join('; ') || 'recorded without player details';
}

function logReportRetention(lineCount, missing) {
    return 'Log entries: ' + lineCount + '\nEarlier entries missing: ' + missing + '\n\n';
}

function logReportText(buffer, info, gap, capturedAt) {
    let header = 'TV Playback Log Report\n\n';
    header += 'Format version: 1\nExported: ' + new Date().toISOString() + '\nStatus received: ' + capturedAt + '\n\n';
    header += 'Summary\n\nCurrent player: ' + logPlayerSummary(info.playbackLog) + '\n';
    header += 'Last error snapshot: ' + (info.lastPlaybackError === null ? 'none retained in this app process' : logPlayerSummary(info.lastPlaybackError)) + '\n\n';
    header += 'Report Data\n\n' + logReportInfo(info) + '\n\nRaw Logcat\n\n';
    const lines = buffer.lines;
    let start = 0;
    let bytes = buffer.bytes;
    const fixedBytes = logEncoder.encode(header).length;
    let retention = logReportRetention(lines.length, gap || buffer.trimmed);
    while (bytes + fixedBytes + logEncoder.encode(retention).length > LOG_MAX_BYTES) {
        if (start === lines.length) throw new Error('Log info too large');
        bytes -= lines[start++].bytes;
        retention = logReportRetention(lines.length - start, true);
    }
    return header + retention + lines.slice(start).map(line => line.text + '\n').join('');
}

function downloadLog(blob, filename) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 5000);
}

async function shareLogReport(blob, filename) {
    if (typeof File !== 'undefined' && typeof navigator.share === 'function' && typeof navigator.canShare === 'function') {
        const file = new File([blob], filename, { type: blob.type });
        if (navigator.canShare({ files: [file] })) {
            await navigator.share({ title: t('logTitle'), files: [file] });
            return 'logShared';
        }
    }
    downloadLog(blob, filename);
    return 'logReportDownloaded';
}

class LogReader {
    constructor(update) {
        this.update = update;
        this.buffer = new LogBuffer();
        this.generation = 0;
        this.visible = true;
        this.active = false;
        this.state = 'logWaiting';
        this.cursor = undefined;
        this.remainingMillis = null;
        this.receivedAt = 0;
        this.gap = false;
        this.pending = null;
        this.timer = null;
    }

    cancel() {
        this.generation++;
        if (this.state === 'logStopping') this.state = 'logStopFailed';
        clearTimeout(this.timer);
        this.timer = null;
        if (this.pending) this.pending.abort();
        this.pending = null;
    }

    connect(code = '') {
        this.cancel();
        this.code = code;
        this.cursor = undefined;
        this.remainingMillis = null;
        this.gap = false;
        this.buffer.clear();
        this.active = true;
        this.state = 'logConnecting';
        this.update();
        this.schedule(0);
    }

    async stop() {
        if (!this.code || this.state === 'logStopping') return;
        this.cancel();
        this.active = false;
        this.state = 'logStopping';
        this.remainingMillis = null;
        const generation = this.generation;
        const controller = new AbortController();
        this.pending = controller;
        const timeout = setTimeout(() => controller.abort(), 10000);
        this.update();
        try {
            const response = await fetch('/log', { method: 'DELETE', headers: { 'X-Log-Code': this.code }, cache: 'no-store', signal: controller.signal });
            if (generation !== this.generation) return;
            if (response.ok || response.status === 410) {
                this.code = '';
                this.state = 'logStopped';
            } else {
                this.state = response.status === 403 ? 'logDenied' : 'logStopFailed';
            }
        } catch (error) {
            if (generation === this.generation) this.state = 'logStopFailed';
        } finally {
            clearTimeout(timeout);
            if (generation === this.generation) {
                this.pending = null;
                this.update();
            }
        }
    }

    setVisible(visible) {
        this.visible = visible;
        if (!visible) this.cancel();
        else this.schedule(0);
        this.update();
    }

    schedule(delay) {
        clearTimeout(this.timer);
        this.timer = null;
        if (this.active && this.visible && !this.pending) this.timer = setTimeout(() => this.poll(), delay);
    }

    async poll() {
        clearTimeout(this.timer);
        this.timer = null;
        if (!this.active || !this.visible || this.pending) return;
        const generation = this.generation;
        const controller = new AbortController();
        this.pending = controller;
        const timeout = setTimeout(() => controller.abort(), 10000);
        const opening = !this.code;
        let delay = 1000;
        try {
            const url = this.cursor === undefined ? '/log' : '/log?after=' + this.cursor;
            const options = opening ? { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}' } : { method: 'GET', headers: { 'X-Log-Code': this.code } };
            const response = await fetch(url, { ...options, cache: 'no-store', signal: controller.signal });
            if (generation !== this.generation) return;
            if (opening) delay = await this.openSession(response, generation);
            else delay = await this.readPage(response, generation);
        } catch (error) {
            if (generation === this.generation) {
                this.state = opening ? 'logConnectFailed' : 'logRetrying';
                if (opening) this.active = false;
            }
        } finally {
            clearTimeout(timeout);
            if (generation === this.generation) {
                this.pending = null;
                this.update();
                this.schedule(delay);
            }
        }
    }

    async openSession(response, generation) {
        if (!response.ok) {
            this.active = false;
            this.state = response.status === 403 ? 'logDenied' : 'logConnectFailed';
            return 1000;
        }
        const session = await response.json();
        if (generation !== this.generation) return 1000;
        if (!session || typeof session.code !== 'string' || !/^[A-HJ-NP-Z2-9]{8}$/.test(session.code)) {
            this.active = false;
            this.state = 'invalidResponse';
            return 1000;
        }
        this.code = session.code;
        return 0;
    }

    async readPage(response, generation) {
        if ([400, 403, 410].includes(response.status)) {
            this.active = false;
            this.state = response.status === 410 ? 'logEnded' : response.status === 403 ? 'logDenied' : 'invalidResponse';
            this.remainingMillis = null;
            return 1000;
        }
        if (!response.ok) throw new Error('Log request failed');
        const page = readLogPage(await response.json(), this.cursor);
        if (generation !== this.generation) return 1000;
        this.buffer.append(page.lines);
        this.cursor = page.next;
        this.remainingMillis = page.remainingMillis;
        this.receivedAt = performance.now();
        this.gap ||= page.gap;
        this.state = 'logReceiving';
        return page.backlog ? 0 : 1000;
    }
}

function initLogs() {
    bindWebReturn(document.getElementById('log_return'), 'settings');
    const text = document.getElementById('log_text');
    const search = document.getElementById('log_search');
    const level = document.getElementById('log_level');
    const follow = document.getElementById('log_follow');
    const pause = document.getElementById('log_pause');
    const download = document.getElementById('log_download');
    const retry = document.getElementById('log_retry');
    const stop = document.getElementById('log_stop');
    const share = document.getElementById('log_share');
    const reportRetry = document.getElementById('log_report_retry');
    const remaining = document.getElementById('log_remaining');
    const count = document.getElementById('log_count');
    const exportStatus = document.getElementById('log_export_status');
    let info = null;
    let capturedAt = '';
    let infoRequest = null;
    let infoAttempt = -Infinity;
    let exporting = false;
    let infoError = false;
    let paused = false;
    let renderedRevision = -1;
    let renderedSearch;
    let renderedLevel;
    const reader = new LogReader(update);
    bindChoice(level, () => t('logLevel'), () => [{value: '', label: t('logAll')}, ...['V', 'D', 'I', 'W', 'E', 'F'].map(value => ({value, label: value}))]);

    function render(followLatest = false) {
        const changed = renderedRevision !== reader.buffer.revision || renderedSearch !== search.value || renderedLevel !== level.value;
        if (changed) {
            renderedRevision = reader.buffer.revision;
            renderedSearch = search.value;
            renderedLevel = level.value;
            const content = reader.buffer.text(search.value, level.value) || t(reader.buffer.lines.length ? 'logNoMatches' : 'logEmpty');
            if (text.textContent !== content) text.textContent = content;
        }
        if (follow.checked && (changed || followLatest)) text.scrollTop = text.scrollHeight;
    }

    function update() {
        if (reader.state === 'logConnecting') {
            cancelLogInfo();
            info = null;
            capturedAt = '';
            infoError = false;
            exportStatus.hidden = true;
        }
        const stopFocused = document.activeElement === stop;
        const reportRetryFocused = document.activeElement === reportRetry;
        const status = reader.active && !reader.visible ? 'logHidden' : reader.state;
        const statusNode = document.getElementById('log_status');
        statusNode.dataset.state = status;
        if (statusNode.textContent !== t(status)) statusNode.textContent = t(status);
        const seconds = Math.ceil(Math.max(0, (reader.remainingMillis || 0) - (performance.now() - reader.receivedAt)) / 1000);
        const time = reader.remainingMillis === null ? '' : t('logRemaining', Math.floor(seconds / 60) + ':' + String(seconds % 60).padStart(2, '0'));
        if (remaining.textContent !== time) remaining.textContent = time;
        document.getElementById('log_gap').hidden = !reader.gap;
        document.getElementById('log_trimmed').hidden = !reader.buffer.trimmed;
        const total = t('logCount', reader.buffer.lines.length);
        if (count.textContent !== total) count.textContent = total;
        const canRetry = ['logConnectFailed', 'logRetrying', 'logEnded', 'logDenied', 'invalidResponse', 'logStopped'].includes(reader.state);
        retry.hidden = !canRetry;
        stop.hidden = !reader.code || ['logEnded', 'logDenied', 'invalidResponse', 'logStopped'].includes(reader.state);
        stop.setAttribute('aria-disabled', String(reader.state === 'logStopping'));
        if (stopFocused && stop.hidden && !retry.hidden) retry.focus();
        download.disabled = share.disabled = exporting || !info;
        reportRetry.hidden = !infoError || !!info;
        reportRetry.setAttribute('aria-disabled', String(!!infoRequest));
        if (reportRetryFocused && reportRetry.hidden && !download.disabled) download.focus();
        if (reader.visible && !infoRequest && (reader.state === 'logReceiving' || (!info && (reader.buffer.lines.length || canRetry))) && performance.now() - infoAttempt >= 10000) refreshLogInfo();
        if (!paused && reader.visible) render();
    }

    function cancelLogInfo() {
        infoRequest?.abort();
        infoRequest = null;
        infoAttempt = -Infinity;
    }

    async function refreshLogInfo() {
        if (infoRequest) return;
        infoAttempt = performance.now();
        const controller = new AbortController();
        infoRequest = controller;
        reportRetry.setAttribute('aria-disabled', 'true');
        const timeout = setTimeout(() => controller.abort(), 5000);
        try {
            const response = await fetch('/log/info', { cache: 'no-store', signal: controller.signal });
            if (!response.ok) throw new Error('Log info unavailable');
            const body = readLogInfo(await response.json());
            if (infoRequest === controller && !controller.signal.aborted) {
                info = body;
                capturedAt = new Date().toISOString();
                if (infoError) exportStatus.hidden = true;
                infoError = false;
            }
        } catch (error) {
            if (infoRequest === controller && !info) {
                infoError = true;
                exportStatus.hidden = false;
                exportStatus.textContent = t('logReportFailed');
            }
        } finally {
            clearTimeout(timeout);
            if (infoRequest === controller) {
                infoRequest = null;
                update();
            }
        }
    }

    document.addEventListener('visibilitychange', () => {
        reader.setVisible(!document.hidden);
        if (document.hidden) cancelLogInfo();
    });
    window.addEventListener('pagehide', () => {
        reader.cancel();
        cancelLogInfo();
    });
    window.addEventListener('pageshow', () => reader.setVisible(!document.hidden));
    retry.addEventListener('click', () => reader.active ? reader.schedule(0) : reader.connect());
    stop.addEventListener('click', () => reader.stop());
    search.addEventListener('input', () => { if (!paused) render(); });
    level.addEventListener('change', () => { if (!paused) render(); });
    follow.addEventListener('change', () => { if (!paused) render(true); });
    text.addEventListener('scroll', () => {
        if (text.scrollHeight - text.clientHeight - text.scrollTop > 1) follow.checked = false;
    });
    pause.addEventListener('click', () => {
        paused = !paused;
        const label = t(paused ? 'logResume' : 'logPause');
        pause.setAttribute('aria-label', label); pause.title = label;
        pause.setAttribute('aria-pressed', String(paused));
        if (!paused) render(true);
    });
    document.getElementById('log_clear').addEventListener('click', () => {
        reader.buffer.clear(true);
        update();
        render();
    });
    download.addEventListener('click', () => exportLogReport(false));
    share.addEventListener('click', () => exportLogReport(true));
    reportRetry.addEventListener('click', refreshLogInfo);

    async function exportLogReport(shareFile) {
        if (exporting || !info) return;
        exporting = true;
        download.disabled = share.disabled = true;
        exportStatus.hidden = true;
        try {
            const blob = new Blob([logReportText(reader.buffer, info, reader.gap, capturedAt)], { type: 'text/plain;charset=utf-8' });
            const filename = 'tv-log-' + new Date().toISOString().replace(/[:.]/g, '-') + '.txt';
            let status = 'logReportDownloaded';
            if (shareFile) status = await shareLogReport(blob, filename);
            else downloadLog(blob, filename);
            exportStatus.textContent = t(status);
            exportStatus.hidden = false;
        } catch (error) {
            if (error.name !== 'AbortError') {
                exportStatus.textContent = t('logExportFailed');
                exportStatus.hidden = false;
            }
        } finally {
            exporting = false;
            update();
        }
    }
    reader.setVisible(!document.hidden);
    reader.connect();
}

if (typeof module !== 'undefined' && module.exports) module.exports = { readLogPage, LogBuffer, LogReader, readLogInfo, logReportText, shareLogReport };
else initLogs();
