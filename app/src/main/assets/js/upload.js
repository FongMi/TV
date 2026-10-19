function fileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

class TransferRate {
    constructor(bytes = 0) {
        this.bytes = bytes;
        this.time = Date.now();
        this.speed = 0;
    }

    update(bytes) {
        const now = Date.now(), elapsed = now - this.time;
        if (elapsed >= 500) {
            this.speed = Math.max(0, bytes - this.bytes) * 1000 / elapsed;
            this.bytes = bytes;
            this.time = now;
        }
    }
}

function transferDetail(total, loaded, speed) {
    let text = (total > 0 ? Math.min(100, Math.round(loaded / total * 100)) + '% · ' : '') + fileSize(loaded) + (total >= 0 ? ' / ' + fileSize(total) : '');
    if (speed > 0) {
        text += ' · ' + fileSize(speed) + '/s';
        if (total > loaded) text += ' · ' + t('transferRemaining').replace('{time}', Math.ceil((total - loaded) / speed) + 's');
    }
    return text;
}

function transferError(message) {
    if (/Insufficient storage space|No space left|Not enough space|ENOSPC/i.test(message)) return t('transferNoSpace');
    if (/checksum mismatch/i.test(message)) return t('transferChecksum');
    if (/expired|revision changed|Download changed/i.test(message)) return t('transferExpired');
    return message || t('uploadFailed');
}

class UploadTransfer {
    constructor(update) {
        this.update = update;
        this.job = null;
        this.files = [];
        this.error = '';
        this.rate = new TransferRate();
        this.pending = new Map();
        this.running = false;
        this.status = '';
        this.loaded = 0;
        this.total = 0;
    }

    get busy() { return !!this.job; }

    send(files, path, threads = 0) {
        if (!files.length) return Promise.resolve(false);
        if (!Number.isInteger(threads) || threads < 0 || threads > 4) throw new Error('Invalid upload threads');
        const entries = Array.from(files, file => ({ file, path, loaded: 0, status: 'uploadQueued', cancelled: false, error: '' }));
        if (this.job) {
            this.files.push(...entries);
            this.refresh();
            return this.job.promise;
        }
        this.files = entries;
        let resolve;
        const promise = new Promise(done => { resolve = done; });
        this.job = { files: entries, index: 0, current: null, manualPaused: false, threads, resolve, promise };
        this.rate = new TransferRate();
        this.resume();
        return promise;
    }

    refresh() {
        this.total = this.files.reduce((total, entry) => total + (entry.cancelled ? 0 : entry.file.size), 0);
        this.loaded = this.files.reduce((total, entry) => total + (entry.cancelled ? 0 : entry.loaded), 0);
        this.rate.update(this.loaded);
        this.update();
    }

    async resume() {
        if (!this.job || this.running) return;
        const job = this.job;
        job.manualPaused = false;
        this.running = true;
        this.error = '';
        this.rate = new TransferRate(this.loaded);
        try {
            await this.uploadFiles(job);
        } finally {
            if (this.job === job) {
                this.running = false;
                this.refresh();
            }
        }
    }

    async uploadFiles(job) {
        while (job.index < job.files.length) {
            const entry = job.current = job.files[job.index], file = entry.file;
            if (entry.cancelled) {
                job.index++;
                continue;
            }
            try {
                if (!entry.hash) {
                    const hash = await this.hashFile(file, entry, job);
                    if (this.job !== job) return;
                    if (entry.cancelled) {
                        job.index++;
                        continue;
                    }
                    if (job.manualPaused) {
                        this.status = entry.status = 'uploadPaused';
                        return;
                    }
                    this.rememberFileHash(entry, file, hash);
                }
                await this.sendFile(entry, job);
                if (this.job !== job) return;
                this.completeFile(entry, file, job);
            } catch (error) {
                if (this.job !== job) return;
                if (entry.cancelled) {
                    job.index++;
                    continue;
                }
                this.status = entry.status = error.network || job.manualPaused ? 'uploadPaused' : 'uploadFailed';
                this.error = entry.error = job.manualPaused ? '' : error.message;
                return;
            }
        }
        this.finish('uploadDone', this.files.some(entry => entry.status === 'uploadDone'));
    }

    rememberFileHash(entry, file, hash) {
        entry.hash = hash.hex();
        entry.key = sha256(JSON.stringify([entry.path, file.name, file.size, entry.hash]));
        entry.id = this.checkpoint(entry.key);
    }

    async hashFile(file, entry, job) {
        this.status = entry.status = 'uploadPreparing';
        this.refresh();
        const hash = sha256.create();
        for (let offset = 0; offset < file.size; offset += 4 * 1024 * 1024) {
            const bytes = await file.slice(offset, offset + 4 * 1024 * 1024).arrayBuffer();
            if (this.job !== job) return;
            if (entry.cancelled || job.manualPaused) break;
            hash.update(bytes);
        }
        return hash;
    }

    completeFile(entry, file, job) {
        entry.status = 'uploadDone';
        entry.loaded = file.size;
        this.checkpoint(entry.key, true);
        job.index++;
    }

    async sendFile(entry, job) {
        const file = entry.file;
        this.status = entry.status = 'uploadSending';
        entry.error = '';
        this.refresh();
        let state = await this.request('/upload/start', { id: entry.id, path: entry.path, name: file.name, size: file.size, sha256: entry.hash, parallel: true }, job);
        this.validate(state, file.size);
        state = await this.sendChunks(entry, state, job);
        if (!state.complete) {
            this.status = entry.status = 'uploadProcessing';
            this.refresh();
            state = await this.request('/upload/finish', { id: entry.id, ...(state.revision ? { revision: state.revision } : {}) }, job);
            this.validate(state, file.size);
            if (!state.complete) throw new Error('Upload not completed');
        }
    }

    validate(state, size) {
        if (!state || !Number.isSafeInteger(state.offset) || state.offset < 0 || state.offset > size || typeof state.complete !== 'boolean' || state.complete && state.offset !== size || !Number.isSafeInteger(state.chunkSize) || state.chunkSize <= 0 || state.chunkSize > 4 * 1024 * 1024) throw new Error('Invalid upload response');
        if (state.threads === undefined || state.threads === 1) return;
        const count = Math.ceil(size / state.chunkSize);
        if (!Number.isInteger(state.threads) || state.threads < 1 || state.threads > 4 || !/^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$/.test(state.revision) || !Array.isArray(state.chunks) || new Set(state.chunks).size !== state.chunks.length || state.chunks.some(index => !Number.isSafeInteger(index) || index < 0 || index >= count)) throw new Error('Invalid upload response');
        let offset = 0;
        const saved = new Set(state.chunks);
        while (saved.has(offset / state.chunkSize) && offset < size) offset += Math.min(state.chunkSize, size - offset);
        if (!state.complete && state.offset !== offset) throw new Error('Invalid upload response');
    }

    async sendChunks(entry, state, job) {
        const file = entry.file, parallel = state.threads > 1, saved = new Set(parallel ? state.chunks : []), pending = new Map();
        let cursor = parallel ? 0 : state.offset, received = state.complete ? file.size : parallel ? Array.from(saved).reduce((total, index) => total + Math.min(state.chunkSize, file.size - index * state.chunkSize), 0) : state.offset;
        let failure = null, complete = state.complete, limit = parallel ? Math.min(job.threads || state.threads, state.threads) : 1;
        let probe = 0, baseline = 0, settled = job.threads > 0 || !parallel || Math.ceil((file.size - received) / state.chunkSize) <= state.threads;
        const progress = () => {
            if (this.job !== job || entry.cancelled) return;
            entry.loaded = complete ? file.size : received + Array.from(pending.values()).reduce((total, bytes) => total + bytes, 0);
            this.refresh();
        };
        progress();
        this.rate = new TransferRate(this.loaded);
        const nextOffset = () => {
            if (parallel) {
                while (saved.has(cursor)) cursor++;
                return cursor++ * state.chunkSize;
            }
            return cursor;
        };
        const active = () => !complete && !failure && this.job === job && !entry.cancelled && !job.manualPaused;
        const send = async offset => {
            try {
                pending.set(offset, 0);
                const {reply, size, end} = await this.sendChunk(entry, state, job, offset, bytes => {
                    pending.set(offset, bytes);
                    progress();
                });
                pending.delete(offset);
                received += size;
                if (!parallel) cursor = end;
                complete ||= reply.complete;
                progress();
                return size;
            } catch (error) {
                if (!failure) {
                    failure = error;
                    this.abort(job);
                }
                return 0;
            }
        };
        const worker = async () => {
            while (active()) {
                const offset = nextOffset();
                if (offset >= file.size) return;
                await send(offset);
            }
        };
        const probeOffsets = () => {
            const offsets = [];
            while (offsets.length < limit) {
                const offset = nextOffset();
                if (offset >= file.size) break;
                offsets.push(offset);
            }
            return offsets;
        };
        const sendProbe = async (offset, started) => {
            const size = await send(offset);
            if (probe === 0 && !failure && Date.now() - started < 500) settled = true;
            if (settled) await worker();
            return size;
        };
        const updateConcurrency = (offsets, started, sizes) => {
            const elapsed = Math.max(1, Date.now() - started), bytes = sizes.reduce((sum, size) => sum + size, 0), speed = bytes * 1000 / elapsed;
            if (!failure && offsets.length === limit) {
                if (probe === 0) {
                    startProbe(elapsed, bytes, speed);
                } else if (probe === 1) {
                    compareProbe(speed);
                } else {
                    if (speed < baseline * 0.95) limit = Math.min(2, state.threads);
                    settled = true;
                }
            }
        };
        const startProbe = (elapsed, bytes, speed) => {
            baseline = speed;
            if (elapsed >= 500 && (file.size - received) * elapsed / bytes >= 5000) {
                limit = Math.min(2, state.threads);
                probe = 1;
            } else {
                settled = true;
            }
        };
        const compareProbe = speed => {
            if (speed >= baseline * 0.95) {
                baseline = speed;
                limit = 1;
                probe = 2;
            } else {
                limit = state.threads;
                settled = true;
            }
        };
        while (active()) {
            entry.threads = limit;
            if (settled) {
                await Promise.all(Array.from({ length: limit }, worker));
                break;
            }
            const offsets = probeOffsets();
            if (!offsets.length) break;
            const started = Date.now();
            const sizes = await Promise.all(offsets.map(offset => sendProbe(offset, started)));
            updateConcurrency(offsets, started, sizes);
        }
        if (failure) throw failure;
        if (this.job !== job || entry.cancelled || job.manualPaused) throw new Error('Upload cancelled');
        return { ...state, offset: file.size, complete };
    }

    async sendChunk(entry, state, job, offset, progress) {
        const file = entry.file, parallel = state.threads > 1;
        const end = Math.min(file.size, offset + state.chunkSize), size = end - offset;
        const reply = await this.request('/upload/chunk', { id: entry.id, offset, size, chunk: file.slice(offset, end), ...(parallel ? { revision: state.revision } : {}) }, job, progress, size);
        this.validate(reply, file.size);
        if (reply.chunkSize !== state.chunkSize || (parallel ? reply.revision !== state.revision || reply.threads !== state.threads || !reply.complete && !reply.chunks.includes(offset / state.chunkSize) : reply.offset !== end || reply.complete)) throw new Error('Invalid upload acknowledgement');
        return {reply, size, end};
    }

    checkpoint(key, remove = false) {
        let saved = {};
        try {
            saved = JSON.parse(localStorage.getItem('tv.uploads')) || {};
        } catch {}
        if (typeof saved !== 'object' || Array.isArray(saved)) saved = {};
        const cutoff = Date.now() - 7 * 24 * 60 * 60 * 1000;
        for (const name of Object.keys(saved)) if (!saved[name] || !Number.isFinite(saved[name].time) || saved[name].time < cutoff || !/^[0-9a-f]{32}$/.test(saved[name].id)) delete saved[name];
        const id = saved[key]?.id || Array.from(crypto.getRandomValues(new Uint8Array(16)), byte => byte.toString(16).padStart(2, '0')).join('');
        if (remove) delete saved[key];
        else saved[key] = { id, time: Date.now() };
        const keys = Object.keys(saved).sort((a, b) => saved[b].time - saved[a].time);
        for (const name of keys.slice(20)) delete saved[name];
        try {
            localStorage.setItem('tv.uploads', JSON.stringify(saved));
        } catch {}
        return id;
    }

    request(url, values, job, progress = null, size = 0) {
        if (this.job !== job) return Promise.reject(new Error('Upload cancelled'));
        const body = new FormData();
        for (const [key, value] of Object.entries(values)) body.append(key, value);
        const request = new XMLHttpRequest();
        this.pending.set(request, job);
        return new Promise((resolve, reject) => {
            const finish = error => {
                if (!this.pending.delete(request)) return;
                if (this.job !== job) {
                    reject(new Error('Upload cancelled'));
                    return;
                }
                if (error) {
                    reject(error);
                    return;
                }
                try {
                    if (request.status < 200 || request.status >= 300) throw new Error(request.responseText.trim().slice(0, 300) || 'Upload request failed');
                    resolve(JSON.parse(request.responseText));
                } catch (error) {
                    reject(error);
                }
            };
            request.upload.addEventListener('progress', event => {
                if (!this.pending.has(request) || this.job !== job || !progress) return;
                progress(event.lengthComputable && event.total > 0 ? Math.floor(size * Math.min(1, event.loaded / event.total)) : 0);
            });
            request.addEventListener('load', () => finish());
            for (const event of ['error', 'timeout', 'abort']) request.addEventListener(event, () => finish(Object.assign(new Error('Upload interrupted'), { network: true })));
            request.open('POST', url);
            request.timeout = url === '/upload/finish' ? 120000 : 60000;
            request.send(body);
        });
    }

    recover() {
        if (this.status === 'uploadPaused' && !this.job?.manualPaused) this.resume();
    }

    finish(status, success) {
        const job = this.job;
        this.job = null;
        this.running = false;
        this.abort(job);
        this.status = status;
        this.refresh();
        job?.resolve(success);
    }

    abort(job) {
        for (const [request, owner] of this.pending) if (owner === job) request.abort();
    }

    pause() {
        if (this.job && this.running && this.status !== 'uploadProcessing') {
            this.job.manualPaused = true;
            this.abort(this.job);
        }
    }

    cancelEntry(index) {
        const entry = this.files[index], job = this.job;
        if (!job || !entry || entry.status === 'uploadDone' || entry.status === 'uploadProcessing') return;
        entry.cancelled = true;
        entry.status = 'uploadCancelled';
        if (job.current === entry) this.abort(job);
        this.refresh();
        if (!this.running && !job.manualPaused) this.resume();
    }

    retryEntry(index) {
        if (this.files[index] === this.job?.current && this.files[index].status === 'uploadFailed') this.resume();
    }

    cancel() {
        if (!this.job) return;
        for (const entry of this.files) {
            if (entry.status !== 'uploadDone') {
                entry.cancelled = true;
                entry.status = 'uploadCancelled';
            }
        }
        this.finish('uploadCancelled', false);
    }

}

function initUploads() {
    const rows = new Map();
    const box = document.getElementById('upload_status'), progress = document.getElementById('upload_progress');
    const cancel = document.getElementById('upload_cancel'), resume = document.getElementById('upload_resume');
    const pause = document.getElementById('upload_pause'), dismiss = document.getElementById('upload_dismiss');
    const transfer = new UploadTransfer(() => {
        box.hidden = false;
        document.getElementById('upload_message').textContent = t(transfer.status === 'uploadPaused' && transfer.job?.manualPaused ? 'transferPaused' : transfer.status);
        document.getElementById('upload_detail').textContent = transfer.status === 'uploadSending' ? transferDetail(transfer.total, transfer.loaded, transfer.rate.speed) : '';
        const error = document.getElementById('upload_error');
        error.textContent = transfer.error ? transferError(transfer.error) : '';
        error.hidden = !transfer.error;
        progress.value = transfer.total > 0 ? Math.min(100, transfer.loaded / transfer.total * 100) : 0;
        progress.hidden = !transfer.running;
        cancel.hidden = !transfer.busy;
        cancel.disabled = transfer.status === 'uploadProcessing';
        pause.hidden = !transfer.running;
        pause.disabled = transfer.status === 'uploadProcessing';
        resume.hidden = !transfer.busy || transfer.running;
        dismiss.hidden = transfer.busy;
        const files = document.getElementById('upload_files');
        for (const [entry, row] of rows) {
            if (!transfer.files.includes(entry)) {
                row.root.remove();
                rows.delete(entry);
            }
        }
        for (const entry of transfer.files) renderUploadEntry(entry, files);
        document.dispatchEvent(new Event('uploadchange'));
    });

    function renderUploadEntry(entry, files) {
        if (!rows.has(entry)) rows.set(entry, createUploadRow(entry, files));
        const row = rows.get(entry);
        row.text.textContent = entry.file.name + ' · ' + t(entry.status === 'uploadPaused' ? 'transferPaused' : entry.status === 'uploadCancelled' ? 'transferCancelled' : entry.status) + (entry.status === 'uploadSending' ? ' ' + Math.min(100, Math.round(entry.loaded / Math.max(1, entry.file.size) * 100)) + '%' : '');
        row.actions.hidden = !transfer.busy || entry.cancelled || ['uploadDone', 'uploadProcessing'].includes(entry.status);
        row.retry.hidden = entry.status !== 'uploadFailed';
        row.retry.title = t('transferRetry');
        row.remove.title = t('cancel');
        row.retry.setAttribute('aria-label', t('transferRetry') + ' ' + entry.file.name);
        row.remove.setAttribute('aria-label', t('cancel') + ' ' + entry.file.name);
    }

    function createUploadRow(entry, files) {
        const root = document.createElement('div');
        root.className = 'transfer-file';
        const text = document.createElement('span');
        text.className = 'transfer-file-name';
        const actions = document.createElement('div');
        actions.className = 'transfer-file-actions';
        const retry = createUploadRetry(entry);
        const remove = createUploadCancel(entry);
        actions.append(retry, remove);
        root.append(text, actions);
        files.append(root);
        return { root, text, actions, retry, remove };
    }

    function createUploadRetry(entry) {
        const retry = document.createElement('button');
        retry.className = 'md-btn md-btn-tonal md-icon-button';
        retry.type = 'button';
        retry.innerHTML = '<svg class="button-icon" viewBox="0 0 24 24" aria-hidden="true"><use href="icons.svg#refresh"/></svg>';
        retry.addEventListener('click', () => transfer.retryEntry(transfer.files.indexOf(entry)));
        return retry;
    }

    function createUploadCancel(entry) {
        const remove = document.createElement('button');
        remove.className = 'md-btn md-btn-outlined md-icon-button';
        remove.type = 'button';
        remove.innerHTML = '<svg class="button-icon" viewBox="0 0 24 24" aria-hidden="true"><use href="icons.svg#close"/></svg>';
        remove.addEventListener('click', () => transfer.cancelEntry(transfer.files.indexOf(entry)));
        return remove;
    }

    cancel.addEventListener('click', () => transfer.cancel());
    pause.addEventListener('click', () => transfer.pause());
    resume.addEventListener('click', () => transfer.resume());
    dismiss.addEventListener('click', () => { if (!transfer.busy) box.hidden = true; });
    const recover = () => { if (!document.hidden) transfer.recover(); };
    document.addEventListener('visibilitychange', recover);
    window.addEventListener('pageshow', recover);
    window.addEventListener('online', recover);
    return transfer;
}

if (typeof module !== 'undefined' && module.exports) module.exports = { UploadTransfer, TransferRate, fileSize };
else window.uploadTransfer = initUploads();
