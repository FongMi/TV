import * as spider from %s;

const site = Object.freeze({type: 3, key: %s});

loadSpider();
const api = globalThis.__JS_SPIDER__;
if (api) globalThis.__JS_SPIDER__ = createSpider(api);

function loadSpider() {
    if (globalThis.__JS_SPIDER__) return;
    if (spider.__jsEvalReturn) {
        globalThis.req = http;
        globalThis.__JS_SPIDER__ = spider.__jsEvalReturn();
    } else if (spider.default) {
        globalThis.__JS_SPIDER__ = typeof spider.default === 'function' ? spider.default(site) : spider.default;
    }
}

function createSpider(api) {
    const wrapped = Object.create(api);
    for (const name of ['home', 'homeVod', 'category', 'detail', 'search', 'play', 'live', 'action']) bindSpiderMethod(wrapped, api, name, true);
    for (const name of ['init', 'proxy', 'sniffer', 'isVideo']) bindSpiderMethod(wrapped, api, name, false);
    bindDanmaku(wrapped, api);
    return wrapped;
}

function output(value) {
    if (typeof value === 'string') return value;
    const json = JSON.stringify(value, netCacheValue);
    if (json === undefined) throw new TypeError('Spider data must be JSON');
    return json;
}

function bindSpiderMethod(wrapped, api, name, serialize) {
    if (typeof api[name] !== 'function') return;
    Object.defineProperty(wrapped, name, {
        value: function(...args) {
            const value = api[name].apply(api, args);
            if (!serialize) return value;
            return value && typeof value.then === 'function' ? value.then(output) : output(value);
        },
        writable: true,
        configurable: true,
        enumerable: true
    });
}

function bindDanmaku(wrapped, api) {
    const danmakus = new Map();
    wrapped.startDanmaku = async function(id, token) {
        if (typeof api.danmaku !== 'function') throw new Error('Spider does not support live danmaku');
        const stop = await api.danmaku(id, item => {
            if (!item || typeof item !== 'object' || Array.isArray(item)) throw new TypeError('Danmaku must be an object');
            emitDanmaku(token, item);
        });
        if (typeof stop !== 'function') throw new TypeError('danmaku must return a stop function');
        danmakus.set(token, stop);
    };
    wrapped.stopDanmaku = async function(token) {
        const stop = danmakus.get(token);
        danmakus.delete(token);
        if (stop) await stop();
    };
    async function destroy() {
        const failures = [];
        for (const token of danmakus.keys()) {
            try {
                await wrapped.stopDanmaku(token);
            } catch (error) {
                failures.push(error);
            }
        }
        try {
            if (typeof api.destroy === 'function') await api.destroy();
        } catch (error) {
            failures.push(error);
        }
        if (failures.length) throw failures[0];
    }

    Object.defineProperty(wrapped, 'destroy', {
        value: destroy,
        writable: true,
        configurable: true,
        enumerable: true
    });
}
