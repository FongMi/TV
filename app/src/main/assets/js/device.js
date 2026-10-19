class DeviceStatus {
    constructor(update) {
        this.update = update;
        this.info = null;
        this.error = false;
        this.visible = false;
        this.pending = null;
        this.timer = null;
        this.stale = true;
    }

    setVisible(visible) {
        if (this.visible === visible) return;
        this.visible = visible;
        clearTimeout(this.timer);
        this.pending?.abort();
        this.pending = null;
        this.stale = true;
        if (visible) this.request();
        else this.update();
    }

    refresh() {
        if (!this.visible || this.pending && this.stale) return;
        this.pending?.abort();
        this.pending = null;
        this.stale = true;
        this.request();
    }

    async request() {
        if (!this.visible || this.pending) return;
        clearTimeout(this.timer);
        const controller = new AbortController();
        this.pending = controller;
        this.error = false;
        this.update();
        const timeout = setTimeout(() => controller.abort(), 5000);
        try {
            const response = await fetch('/remote/info', { cache: 'no-store', signal: controller.signal });
            if (!response.ok) throw new Error('Device unavailable');
            const info = await response.json();
            if (typeof info.device !== 'string' || typeof info.version !== 'string') throw new Error('Invalid device info');
            if (this.pending === controller) {
                this.info = info;
                this.stale = false;
            }
        } catch (error) {
            if (this.pending === controller) {
                this.error = true;
                this.stale = true;
            }
        } finally {
            clearTimeout(timeout);
            if (this.pending === controller) {
                this.pending = null;
                this.update();
                if (this.visible && !this.error) this.timer = setTimeout(() => this.request(), 10000);
            }
        }
    }
}

function initDevice() {
    const retry = document.getElementById('device_retry');
    const status = document.getElementById('device_status');
    const name = document.getElementById('device_name');
    const device = new DeviceStatus(() => {
        if (device.info) {
            const text = device.info.device + ' · v' + device.info.version;
            if (name.textContent !== text) name.textContent = text;
        }
        const text = t(device.error ? 'deviceDisconnected' : device.info && !device.stale ? 'deviceConnected' : 'deviceConnecting');
        if (status.textContent !== text) status.textContent = text;
        status.dataset.connected = String(!!device.info && !device.error && !device.stale);
        status.dataset.error = String(device.error);
        retry.hidden = !device.error && retry.dataset.playbackError !== 'true';
    });
    retry.addEventListener('click', () => device.request());
    const updateVisibility = () => device.setVisible(!document.hidden);
    document.addEventListener('visibilitychange', updateVisibility);
    window.addEventListener('pagehide', () => device.setVisible(false));
    const refresh = () => {
        updateVisibility();
        device.refresh();
    };
    window.addEventListener('pageshow', event => { if (event.persisted) refresh(); });
    window.addEventListener('focus', refresh);
    window.addEventListener('online', refresh);
    updateVisibility();
}

if (typeof module !== 'undefined' && module.exports) module.exports = { DeviceStatus };
else initDevice();
