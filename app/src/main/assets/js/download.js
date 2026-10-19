class DeviceDownload {
    constructor(update, complete) {
        this.update = update;
        this.complete = complete;
        this.current = null;
        this.error = '';
        this.pending = false;
        this.generation = 0;
        this.timer = null;
        this.completed = null;
    }

    get busy() { return this.current && ['downloading', 'pausing', 'cancelling'].includes(this.current.state); }

    async request(action, values) {
        const body = new URLSearchParams();
        for (const [name, value] of Object.entries(values)) body.append(name, value);
        const controller = new AbortController(), timeout = setTimeout(() => controller.abort(), 10000);
        try {
            const response = await fetch('/download/' + action, { method: 'POST', body, signal: controller.signal });
            const text = await response.text();
            if (!response.ok) throw Object.assign(new Error(text.trim().slice(0, 300) || 'Download request failed'), { status: response.status });
            return this.readState(text);
        } finally {
            clearTimeout(timeout);
        }
    }

    readState(text) {
        const state = JSON.parse(text);
        if (!state || !/^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$/.test(state.id) || typeof state.name !== 'string' || typeof state.path !== 'string' || !['downloading', 'pausing', 'paused', 'cancelling', 'cancelled', 'failed', 'completed'].includes(state.state)
            || !Number.isSafeInteger(state.total) || state.total < -1 || !Number.isSafeInteger(state.downloaded) || state.downloaded < 0 || state.total >= 0 && state.downloaded > state.total
            || state.state === 'completed' && state.total >= 0 && state.downloaded !== state.total || state.error !== null && typeof state.error !== 'string'
            || !Number.isFinite(state.speed) || state.speed < 0) throw new Error('Invalid download response');
        return state;
    }

    apply(state) {
        this.current = state;
        this.error = state.error || '';
        this.update();
        if (state.state === 'completed' && this.completed !== state.id) {
            this.completed = state.id;
            this.complete(state);
        }
        if (this.busy) this.timer = setTimeout(() => this.poll(), 500);
    }

    async command(action, values = {}) {
        clearTimeout(this.timer);
        const generation = ++this.generation;
        this.pending = true;
        this.error = '';
        this.update();
        try {
            const state = await this.request(action, { ...(this.current ? { id: this.current.id } : {}), ...values });
            if (this.generation !== generation) return false;
            this.apply(state);
            return true;
        } catch (error) {
            if (this.generation === generation) {
                this.error = error.message;
                if (action !== 'start' && this.current && [404, 409].includes(error.status)) this.current = { ...this.current, state: 'failed' };
                else if (this.busy || this.current && (action === 'resume' || action === 'cancel')) this.timer = setTimeout(() => this.poll(), 500);
                this.update();
            }
            return false;
        } finally {
            if (this.generation === generation) {
                this.pending = false;
                this.update();
            }
        }
    }

    async poll() {
        const generation = this.generation;
        try {
            const state = await this.request('status', { id: this.current.id });
            if (this.generation === generation) this.apply(state);
        } catch (error) {
            if (this.generation === generation) {
                this.error = error.message;
                if ([404, 409].includes(error.status)) this.current = { ...this.current, state: 'failed' };
                this.update();
            }
        }
    }

    async restore() {
        if (this.pending) return;
        clearTimeout(this.timer);
        const generation = ++this.generation;
        try {
            const state = await this.request('status', {});
            if (this.generation === generation) this.apply(state);
        } catch (error) {
            if (this.generation === generation && (this.current || error.status !== 404)) {
                this.error = error.message;
                if (this.current && error.status === 404) this.current = { ...this.current, state: 'failed' };
                this.update();
            }
        }
    }

    start(url, name, path) {
        try {
            if (!['http:', 'https:'].includes(new URL(url).protocol)) throw new Error();
        } catch {
            this.error = 'Invalid download URL';
            this.update();
            return Promise.resolve(false);
        }
        return this.command('start', { url, name, path });
    }

    pause() { return this.command('pause'); }
    resume() { return this.command(this.current && ['paused', 'failed'].includes(this.current.state) ? 'resume' : 'status'); }
    cancel() { return this.command('cancel'); }
}

function initDownloads() {
    const box = document.getElementById('download_status'), progress = document.getElementById('download_progress');
    const pause = document.getElementById('download_pause'), resume = document.getElementById('download_resume');
    const cancel = document.getElementById('download_cancel'), dismiss = document.getElementById('download_dismiss');
    const states = { downloading: 'downloadSending', pausing: 'downloadPausing', paused: 'transferPaused', cancelling: 'downloadCancelling', cancelled: 'transferCancelled', failed: 'downloadFailed', completed: 'downloadDone' };
    const transfer = new DeviceDownload(() => {
        if (!transfer.current) return;
        const state = transfer.current;
        const expired = /Download changed|Download not found/.test(transfer.error);
        box.hidden = false;
        document.getElementById('download_message').textContent = state.name + ' · ' + t(states[state.state]);
        document.getElementById('download_detail').textContent = transferDetail(state.total, state.downloaded, state.state === 'downloading' ? state.speed : 0);
        const error = document.getElementById('download_error');
        error.textContent = expired ? t('downloadExpired') : transfer.error ? transferError(transfer.error) : '';
        error.hidden = !transfer.error;
        if (state.total > 0) progress.value = Math.min(100, state.downloaded / state.total * 100);
        else progress.removeAttribute('value');
        progress.hidden = !transfer.busy;
        pause.hidden = state.state !== 'downloading';
        resume.hidden = expired || !['paused', 'failed'].includes(state.state) && !transfer.error;
        cancel.hidden = expired || ['completed', 'cancelled'].includes(state.state);
        dismiss.hidden = transfer.busy && !transfer.error;
        for (const button of [pause, resume, cancel]) button.disabled = transfer.pending || ['pausing', 'cancelling'].includes(state.state);
    }, state => { if (typeof currentRoot !== 'undefined' && state.path === currentRoot) listFile(currentRoot); });
    pause.addEventListener('click', () => transfer.pause());
    resume.addEventListener('click', () => transfer.resume());
    cancel.addEventListener('click', () => transfer.cancel());
    dismiss.addEventListener('click', () => {
        clearTimeout(transfer.timer);
        transfer.generation++;
        box.hidden = true;
    });
    document.getElementById('download_start').addEventListener('click', async () => {
        const button = document.getElementById('download_start'), error = document.getElementById('download_dialog_error');
        button.disabled = true;
        try {
            const success = await transfer.start(document.getElementById('download_url').value.trim(), document.getElementById('download_name').value.trim(), currentRoot);
            error.textContent = success ? '' : transferError(transfer.error);
            error.hidden = success;
            if (success) closeDialog('downloadDialog');
        } finally {
            button.disabled = false;
        }
    });
    transfer.restore();
    window.addEventListener('online', () => transfer.restore());
    window.addEventListener('pageshow', event => { if (event.persisted) transfer.restore(); });
    return transfer;
}

if (typeof module !== 'undefined' && module.exports) module.exports = { DeviceDownload };
else window.deviceDownload = initDownloads();
