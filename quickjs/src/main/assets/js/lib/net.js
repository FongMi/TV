import host from 'catvod:host';

function req(url, options = {}, id = null) {
    return Promise.resolve().then(() => host.req(url, opts(options, id))).catch(failure);
}

function ws(url, options = {}, id = null) {
    return Promise.resolve().then(() => host.ws(url, opts(options, id))).catch(failure);
}

function opts(options, id) {
    options ??= {};
    if (typeof options !== 'object' || Array.isArray(options)) throw new TypeError('Request options require an object');
    return id == null ? options : route(options, id);
}

function route(options, id) {
    return Object.assign(Object.create(null), options, {netId: id, toJSON: undefined});
}

async function stream(url, options = {}) {
    if (options != null && (typeof options !== 'object' || Array.isArray(options))) throw new TypeError('Invalid stream options');
    if (typeof url !== 'string') return streamBody(url, options);
    const reply = await host.stream(url, options);
    if (reply.error != null) throw new Error(String(reply.error));
    return {code: reply.code, headers: reply.headers, contentType: reply.contentType, stream: streamHandle(reply.token)};
}

function streamHandle(token) {
    return Object.freeze({$tvStream: token, close: () => host.streamClose(token)});
}

function streamBody(source, options) {
    if (!source || typeof source[Symbol.asyncIterator] !== 'function') throw new TypeError('Stream body requires an async iterable');
    if (options && Object.keys(options).length) throw new TypeError('Stream body does not accept request options');
    const iterator = source[Symbol.asyncIterator]();
    let chunk = null, offset = 0, closed = false;
    async function pull(size) {
        while (!closed && (!chunk || offset === chunk.byteLength)) {
            const next = await iterator.next();
            if (next.done || closed) return null;
            if (next.value instanceof ArrayBuffer) chunk = new Uint8Array(next.value);
            else if (ArrayBuffer.isView(next.value)) chunk = new Uint8Array(next.value.buffer, next.value.byteOffset, next.value.byteLength);
            else throw new TypeError('Stream body must yield bytes');
            offset = 0;
        }
        if (closed) return null;
        const end = Math.min(offset + size, chunk.byteLength);
        const result = chunk.buffer.slice(chunk.byteOffset + offset, chunk.byteOffset + end);
        offset = end;
        return result;
    }
    async function cancel() {
        closed = true;
        chunk = null;
        if (typeof iterator.return === 'function') await iterator.return();
    }
    const reply = host.streamBody(pull, cancel);
    return {code: 200, headers: {}, contentType: 'application/octet-stream', stream: streamHandle(reply.token)};
}

async function openStream(url, options = {}) {
    if (options != null && (typeof options !== 'object' || Array.isArray(options))) throw new TypeError('Invalid stream options');
    const reply = await host.openStream(url, options);
    if (reply.error != null) throw new Error(String(reply.error));
    const token = reply.token;
    let pending = false, closed = false, eof = false;
    return Object.freeze({
        code: reply.code, headers: reply.headers, contentType: reply.contentType,
        async read(size = 65536) {
            if (!Number.isInteger(size) || size < 1 || size > 1048576) throw new RangeError('Chunk size must be 1..1048576 bytes');
            if (eof) return null;
            if (closed) throw new Error('Stream reader is closed');
            if (pending) throw new Error('Stream read already pending');
            pending = true;
            try {
                const chunk = await host.streamRead(token, size);
                if (closed) throw new Error('Stream reader is closed');
                if (chunk === null) {
                    eof = true;
                    return null;
                }
                return chunk;
            } catch (error) {
                closed = true;
                host.streamClose(token);
                throw error;
            } finally {
                pending = false;
            }
        },
        close() {
            if (closed || eof) return false;
            closed = true;
            return host.streamClose(token);
        }
    });
}

function connect(url, options, callback) {
    if (options != null && (typeof options !== 'object' || Array.isArray(options))) throw new TypeError('Invalid socket options');
    if (typeof callback !== 'function') throw new TypeError('Socket listener must be a function');
    let closed = false;
    let ended = false;
    const socket = {
        send(data) {
            if (closed) return false;
            if (typeof data === 'string') return host.socketSend(id, data, null);
            const view = ArrayBuffer.isView(data);
            const buffer = view ? data.buffer : data;
            if (!(buffer instanceof ArrayBuffer)) throw new TypeError('send requires text or bytes');
            return host.socketSend(id, null, view ? new Uint8Array(buffer, data.byteOffset, data.byteLength) : buffer);
        },
        close() {
            if (closed) return;
            closed = true;
            host.socketClose(id, false);
        },
        cancel() {
            if (ended) return;
            closed = true;
            ended = true;
            host.socketClose(id, true);
        }
    };
    const id = host.connect(url, options, event => {
        if (event.type === 'close') closed = ended = true;
        if (event.binary) event.content = new Uint8Array(event.content);
        const result = callback(socket, event);
        if (result && typeof result.then === 'function') Promise.resolve(result).catch(error => {
            socket.cancel();
            console.error(error?.message || String(error));
        });
    });
    return socket;
}

function failure(error) {
    return {
        code: '', headers: {}, content: '',
        error: `${error?.name || 'Error'}: ${error?.message || String(error)}`
    };
}

async function sleep(ms, id) {
    if (!Number.isSafeInteger(ms) || ms < 0) throw new RangeError('Invalid sleep duration');
    await host.sleep(ms, id);
}

function cached(key, options, loader, id, background = false) {
    return new Promise((resolve, reject) => {
        validateCache(key, options, loader);
        const settings = background ? Object.assign({}, options, {stale: false}) : options;
        const accepted = host.cached(key, JSON.stringify(settings), id, ticket => loadCache(ticket, loader), (json, error) => {
            if (error != null) return reject(new Error(String(error)));
            try {
                resolve(background ? undefined : JSON.parse(json));
            } catch (error) {
                reject(error);
            }
        }, background);
        if (accepted !== true) reject(new Error('Network cache is unavailable'));
    });
}

function validateCache(key, options, loader) {
    if (typeof key !== 'string' || !key) throw new TypeError('Invalid cache key');
    if (!options || !Number.isSafeInteger(options.ttl) || options.ttl < 0) throw new RangeError('Invalid cache TTL');
    for (const name of ['stale', 'persist']) if (name in options && typeof options[name] !== 'boolean') throw new TypeError(`Invalid cache ${name}`);
    if (typeof loader !== 'function') throw new TypeError('Cache loader must be a function');
}

function refresh(key, options, loader, id = null) {
    cached(key, options, loader, id, true).catch(error => console.error(`Cache ${key}: ${error.message}`));
}

function download(url, path, options = {}) {
    return new Promise((resolve, reject) => {
        if (typeof path !== 'string' || !path) throw new TypeError('Invalid download path');
        if (options != null && (typeof options !== 'object' || Array.isArray(options))) throw new TypeError('Invalid download options');
        host.download(url, path, options, error => error ? reject(new Error(String(error))) : resolve());
    });
}

export function proxyUrl(params = {}, local = true) {
    params ??= {};
    if (Object.prototype.toString.call(params) !== '[object Object]' || Object.getOwnPropertySymbols(params).length) throw new TypeError('Proxy parameters require an object with string keys and values');
    if (typeof local !== 'boolean') throw new TypeError('Proxy local flag requires a boolean');
    const values = Object.create(null);
    for (const [key, value] of Object.entries(params)) {
        if (key === 'do' || key === 'siteKey') throw new TypeError('Proxy parameters cannot override routing');
        if (typeof value !== 'string') throw new TypeError('Proxy parameters require string keys and values');
        values[key] = value;
    }
    return host.proxyUrl(values, local);
}

async function batch(requests, limit = 4, client = net) {
    if (!Array.isArray(requests) || !Number.isSafeInteger(limit) || limit < 1) throw new TypeError('Invalid batch options');
    for (const item of requests) {
        if (!item || typeof item.url !== 'string' || item.options != null && (typeof item.options !== 'object' || Array.isArray(item.options))) throw new TypeError('Invalid batch request');
    }
    return map(requests, item => client.req(item.url, item.options ?? {}), limit);
}

async function map(items, mapper, limit = 4) {
    if (!Array.isArray(items) || typeof mapper !== 'function' || !Number.isSafeInteger(limit) || limit < 1) throw new TypeError('Invalid map options');
    const length = items.length, results = new Array(length);
    let index = 0, failed = false;
    async function worker() {
        while (!failed && index < length) {
            const current = index++;
            try {
                results[current] = await mapper(items[current], current);
            } catch (error) {
                failed = true;
                throw error;
            }
        }
    }
    await Promise.all(Array.from({length: Math.min(limit, length)}, worker));
    return results;
}

function clearCache(key, id) {
    if (key != null && (typeof key !== 'string' || !key)) throw new TypeError('Invalid cache key');
    const error = host.clearCache(key ?? null, id);
    if (error !== '') throw new Error(error || 'Network cache is unavailable');
}

function peek(key, id) {
    if (typeof key !== 'string' || !key) throw new TypeError('Invalid cache key');
    const result = JSON.parse(host.peek(key, id));
    if (result.error) throw new Error(result.error);
    return result.found ? result.value : null;
}

async function json(url, options = {}, id = null) {
    if (options?.buffer) throw new TypeError('JSON requests require text content');
    const result = await req(url, options, id);
    if (result.error) throw new Error(result.error);
    if (result.code < 200 || result.code >= 300) throw new Error(`HTTP ${result.code}`);
    return JSON.parse(result.content);
}

function localCall(operation, key, value) {
    if (typeof key !== 'string' || !key) throw new TypeError('Invalid Local key');
    const result = JSON.parse(host.local(operation, key, value ?? null));
    if (result.error) throw new Error(result.error);
    return result;
}

export const local = {
    get(key, fallback = null) {
        const result = localCall('get', key);
        return result.found ? result.value : fallback;
    },
    set(key, value) {
        localCall('set', key, stringify(value));
    },
    delete(key) {
        localCall('delete', key);
    }
};

export const js = {
    eval(source, options = {}) {
        if (typeof source !== 'string' || options != null && (typeof options !== 'object' || Array.isArray(options))) throw new TypeError('Script requires source and options');
        return host.eval(source, JSON.parse(stringify(options ?? {})));
    }
};

export function link(name, target) {
    if (!target || typeof target !== 'object' || Array.isArray(target)) throw new TypeError('link requires a target object');
    return `[a=cr:${stringify(target)}/]${name}[/a]`;
}

export function page(items, pg, limit, total) {
    pg = Math.max(1, Number(pg));
    if (!Number.isSafeInteger(pg) || !Number.isSafeInteger(limit) || limit < 1) throw new RangeError('Invalid page options');
    if (total == null) {
        total = items.length;
        items = items.slice((pg - 1) * limit, pg * limit);
    }
    if (!Number.isSafeInteger(total) || total < 0) throw new RangeError('Invalid page total');
    return {list: items, page: pg, pagecount: Math.max(1, Math.ceil(total / limit)), limit, total};
}

function loadCache(ticket, loader) {
    return Promise.resolve().then(loader).then(stringify).then(json => host.cacheResolve(ticket, json, null), error => host.cacheResolve(ticket, null, error?.message || String(error)));
}

export function stringify(value) {
    const json = JSON.stringify(value, (key, item) => {
        if (item && typeof item.then === 'function') throw new TypeError('Await asynchronous values before returning JSON');
        return item;
    });
    if (json === undefined) throw new TypeError('Value must be JSON');
    return json;
}

function session() {
    const id = host.session();
    let closed = false;
    const active = () => {
        if (closed) throw new Error('Network session is closed');
    };
    return {
        req: (url, options) => req(url, options, id),
        async stream(source, options) {
            if (typeof source !== 'string') active();
            return net.stream(source, typeof source === 'string' ? route(options, id) : options);
        },
        openStream: (url, options) => net.openStream(url, route(options, id)),
        ws: (url, options) => ws(url, options, id),
        connect: (url, options, callback) => net.connect(url, route(options, id), callback),
        sleep: ms => sleep(ms, id),
        cached: (key, options, loader) => cached(key, options, loader, id),
        refresh: (key, options, loader) => refresh(key, options, loader, id),
        batch: (requests, limit) => batch(requests, limit, {req: (url, options) => req(url, options, id)}),
        map(items, mapper, limit) {
            active();
            if (typeof mapper !== 'function') throw new TypeError('Invalid map options');
            return map(items, (item, index) => {
                active();
                return mapper(item, index);
            }, limit);
        },
        download: (url, path, options) => download(url, path, route(options, id)),
        peek: key => peek(key, id),
        clearCache: key => clearCache(key, id),
        json: (url, options) => json(url, options, id),
        getUserAgent: () => host.getUserAgent(),
        getCookie(url) {
            active();
            return host.getCookie(url, id);
        },
        setCookie(url, cookie) {
            active();
            return host.setCookie(url, cookie, id);
        },
        close() {
            if (closed) return;
            closed = true;
            host.closeSession(id);
        }
    };
}

export const net = {
    req, stream, openStream, json, ws, connect, batch, map, refresh, download,
    sleep: ms => sleep(ms, null),
    cached: (key, options, loader) => cached(key, options, loader, null),
    peek: key => peek(key, null),
    clearCache: key => clearCache(key, null),
    session,
    getCookie: url => host.getCookie(url, null),
    setCookie: (url, cookie) => host.setCookie(url, cookie, null),
    getUserAgent: () => host.getUserAgent()
};

export const {s2t, t2s, openWeb, getPort, danmakuUrl, joinUrl, htmlQuery, md5X, sha256X, randomX, aesX, desX, rsaX} = host;
