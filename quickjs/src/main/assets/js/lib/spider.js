import * as spider from %s

const site = Object.freeze({type: 3, key: %s})

if (!globalThis.__JS_SPIDER__) {
    if (spider.__jsEvalReturn) {
        globalThis.req = http
        globalThis.__JS_SPIDER__ = spider.__jsEvalReturn()
    } else if (spider.default) {
        globalThis.__JS_SPIDER__ = typeof spider.default === 'function' ? spider.default(site) : spider.default
    }
}

function output(value) {
    if (typeof value === 'string') return value;
    const json = JSON.stringify(value, netCacheValue);
    if (json === undefined) throw new TypeError('Spider data must be JSON');
    return json;
}

function bindSpiderMethod(wrapped, api, data, name) {
    if (typeof api[name] !== 'function') return;
    Object.defineProperty(wrapped, name, {
        value: function(...args) {
            const value = api[name].apply(api, args);
            if (!data.has(name)) return value;
            return value && typeof value.then === 'function' ? value.then(output) : output(value);
        },
        writable: true,
        configurable: true,
        enumerable: true
    });
}

const api = globalThis.__JS_SPIDER__;
if (api) {
    const wrapped = Object.create(api);
    const data = new Set(['home', 'homeVod', 'category', 'detail', 'search', 'play', 'live', 'action']);
    for (const name of [...data, 'init', 'proxy', 'sniffer', 'isVideo', 'destroy']) bindSpiderMethod(wrapped, api, data, name);
    globalThis.__JS_SPIDER__ = wrapped;
}
