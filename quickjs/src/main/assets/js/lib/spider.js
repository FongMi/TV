import * as entry from %s;
import host from 'catvod:host';
import {Spider} from 'catvod';
import {stringify} from 'assets://js/lib/net.js';
import {dialogActions} from 'assets://js/lib/dialog.js';

if (typeof entry.default !== 'function' || !(entry.default.prototype instanceof Spider)) throw new TypeError('Spider requires a default class extending Spider');
host.attach(createSpider(new entry.default()), dialogActions);

function createSpider(api) {
    const wrapped = Object.create(api);
    for (const name of ['homeContent', 'homeVideoContent', 'categoryContent', 'detailContent', 'searchContent', 'playerContent', 'liveContent', 'action']) bindSpiderMethod(wrapped, api, name, true);
    for (const name of ['init', 'proxy', 'manualVideoCheck', 'isVideoFormat']) bindSpiderMethod(wrapped, api, name, false);
    bindDanmaku(wrapped, api);
    return wrapped;
}

function output(value, method) {
    if (method === 'liveContent' && typeof value === 'string') return value;
    if (!value || typeof value !== 'object') throw new TypeError('Spider data must be an object or array');
    const json = stringify(value);
    if (json[0] !== '{' && json[0] !== '[') throw new TypeError('Spider data must be an object or array');
    return json;
}

function bindSpiderMethod(wrapped, api, name, serialize) {
    Object.defineProperty(wrapped, name, {
        value: async function(...args) {
            const value = await api[name](...args);
            return serialize ? output(value, name) : value;
        },
        writable: true, configurable: true, enumerable: true
    });
}

function bindDanmaku(wrapped, api) {
    const danmakus = new Map();
    wrapped.startDanmaku = async function(id, token) {
        const stop = await api.danmaku(id, item => {
            if (!item || typeof item !== 'object' || Array.isArray(item)) throw new TypeError('Danmaku must be an object');
            host.emitDanmaku(token, item);
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
            await api.destroy();
        } catch (error) {
            failures.push(error);
        }
        if (failures.length) throw failures[0];
    }

    Object.defineProperty(wrapped, 'destroy', {value: destroy, writable: true, configurable: true, enumerable: true});
}
