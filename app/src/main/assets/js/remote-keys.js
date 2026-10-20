function initRemoteKeys() {
    const route = remoteReturnRoute(window.location.search);
    bindWebReturn(document.getElementById('remote_return'), route.tab, route.path);
    const keys = [...document.querySelectorAll('[data-key]')];
    const status = document.getElementById('remote_key_status');
    const queue = [];
    const session = Date.now().toString(36) + '-' + Math.random().toString(36).slice(2);
    let visible = !document.hidden, request, pressed, sequence = 0, generation = 0;
    const setStatus = key => {
        status.textContent = t(key);
        status.dataset.error = String(key !== 'remoteReady');
    };
    const render = () => {
        for (const button of keys) {
            button.disabled = !visible;
            button.dataset.pressed = String(pressed?.button === button);
            button.setAttribute('aria-busy', String(request?.press.button === button));
        }
    };
    const enqueue = (press, action) => {
        queue.push({press, action});
        drain();
    };
    const finish = cancelled => {
        if (!pressed) return;
        const press = pressed;
        pressed = null;
        clearTimeout(press.timer);
        for (let i = queue.length - 1; i >= 0; i--) if (queue[i].press === press && queue[i].action === 'repeat') queue.splice(i, 1);
        enqueue(press, cancelled ? 'cancel' : 'up');
        if (press.pointerId !== undefined && press.button.hasPointerCapture(press.pointerId)) press.button.releasePointerCapture(press.pointerId);
        render();
    };
    async function drain() {
        if (request) return;
        while (queue.length) {
            const current = queue.shift();
            const {press, action} = current;
            request = current;
            const controller = new AbortController();
            const timeout = setTimeout(() => controller.abort(), 5000);
            render();
            try {
                const result = await sendKey(press, action, controller.signal);
                repeatKey(press, action, result);
            } catch (error) {
                failKey(press, action, error);
            } finally {
                clearTimeout(timeout);
                request = null;
                render();
            }
        }
    }

    async function sendKey(press, action, signal) {
        const body = new URLSearchParams({key: press.button.dataset.key, action, id: press.id}).toString();
        const response = await fetch('/remote/key', {method: 'POST', cache: 'no-store', keepalive: true, headers: {'Content-Type': 'application/x-www-form-urlencoded'}, body, signal});
        if (!response.ok) throw response;
        return response.json();
    }

    function repeatKey(press, action, result) {
        if (pressed !== press || action !== 'down' && action !== 'repeat') return;
        if (result.holding) press.timer = setTimeout(() => {if (pressed === press) enqueue(press, 'repeat');}, action === 'down' ? 500 : 80);
        else finish(true);
    }

    function failKey(press, action, error) {
        if (visible && press.generation === generation) setStatus(error.status === 409 ? 'remoteKeyUnavailable' : 'actionFailed');
        for (let i = queue.length - 1; i >= 0; i--) if (queue[i].press === press) queue.splice(i, 1);
        if (pressed === press) finish(true);
        else if (action !== 'cancel') queue.unshift({press, action: 'cancel'});
    }

    const start = (button, pointerId, key) => {
        if (!visible || pressed) return false;
        pressed = {button, pointerId, key, generation, id: session + '-' + ++sequence};
        setStatus('remoteReady');
        enqueue(pressed, 'down');
        render();
        return true;
    };
    for (const button of keys) bindKey(button);

    function bindKey(button) {
        button.addEventListener('pointerdown', event => {
            if (event.button !== 0 || !event.isPrimary) return;
            if (start(button, event.pointerId)) button.setPointerCapture(event.pointerId);
        });
        button.addEventListener('pointerup', event => {if (pressed?.pointerId === event.pointerId) finish(false);});
        button.addEventListener('pointermove', event => {
            if (pressed?.pointerId !== event.pointerId) return;
            const bounds = button.getBoundingClientRect();
            if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) finish(true);
        });
        button.addEventListener('pointercancel', event => {if (pressed?.pointerId === event.pointerId) finish(true);});
        button.addEventListener('lostpointercapture', event => {if (pressed?.pointerId === event.pointerId) finish(true);});
        button.addEventListener('contextmenu', event => event.preventDefault());
        button.addEventListener('keydown', event => {
            if (event.key !== 'Enter' && event.key !== ' ') return;
            event.preventDefault();
            if (!event.repeat) start(button, undefined, event.key);
        });
        button.addEventListener('keyup', event => {
            if (event.key !== 'Enter' && event.key !== ' ') return;
            event.preventDefault();
            if (pressed?.button === button && pressed.key === event.key) finish(false);
        });
        button.addEventListener('blur', () => {if (pressed?.button === button && pressed.key) finish(true);});
        button.addEventListener('click', event => {
            if (event.detail === 0 && start(button)) finish(false);
        });
    }

    const cancel = () => {
        generation++;
        finish(true);
        for (let i = queue.length - 1; i >= 0; i--) {
            if (queue[i].action === 'down' || queue[i].action === 'repeat') queue.splice(i, 1);
            else queue[i].action = 'cancel';
        }
        render();
    };
    const updateVisibility = () => {
        visible = !document.hidden;
        if (!visible) cancel();
        else render();
    };
    document.addEventListener('visibilitychange', updateVisibility);
    window.addEventListener('pagehide', () => {
        visible = false;
        cancel();
    });
    window.addEventListener('blur', cancel);
    window.addEventListener('pageshow', updateVisibility);
    setStatus('remoteReady');
    render();
}

if (typeof module !== 'undefined') module.exports = {initRemoteKeys};
if (typeof document !== 'undefined') document.addEventListener('DOMContentLoaded', initRemoteKeys);
