function _http(url, options = {}) {
    return __http(url, options, options.complete);
}

function _ws(url, options = {}) {
    return __ws(url, options, options.complete);
}

function req(url, options) {
    return http(url, Object.assign({async: false}, options));
}

function http(url, options = {}) {
    if (options?.async === false) {
        try {
            return _http(url, Object.assign({}, options, {complete: undefined}));
        } catch (error) {
            return netError(error);
        }
    }
    return netRequest(url, options, false);
}

function ws(url, options = {}) {
    return netRequest(url, options, true);
}

async function netStream(url, options = {}) {
    if (options != null && (typeof options !== 'object' || Array.isArray(options))) throw new TypeError('Invalid stream options');
    if (typeof url !== 'string') return netStreamBody(url, options);
    const reply = await _stream(url, options);
    if (reply?.error != null) throw new Error(String(reply.error));
    if (!reply || typeof reply.token !== 'string') throw new Error('Invalid stream response');
    return {code: reply.code, headers: reply.headers, contentType: reply.contentType, stream: netStreamHandle(reply.token)};
}

function netStreamHandle(token) {
    return Object.freeze({$tvStream: token, close: () => _streamClose(token)});
}

function netStreamBody(source, options) {
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
    const reply = _streamBody(pull, cancel);
    return {code: 200, headers: {}, contentType: 'application/octet-stream', stream: netStreamHandle(reply.token)};
}

async function netOpenStream(url, options = {}) {
    if (options != null && (typeof options !== 'object' || Array.isArray(options))) throw new TypeError('Invalid stream options');
    const reply = await _openStream(url, options);
    if (reply?.error != null) throw new Error(String(reply.error));
    if (!reply || typeof reply.token !== 'string') throw new Error('Invalid stream response');
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
                const chunk = await _streamRead(token, size);
                if (closed) throw new Error('Stream reader is closed');
                if (chunk === null) {
                    eof = true;
                    return null;
                }
                if (!(chunk instanceof ArrayBuffer) || chunk.byteLength > size) throw new Error('Invalid stream chunk');
                return chunk;
            } catch (error) {
                closed = true;
                _streamClose(token);
                throw error;
            } finally {
                pending = false;
            }
        },
        close() {
            if (closed || eof) return false;
            closed = true;
            return _streamClose(token);
        }
    });
}

function connect(url, options, callback) {
    if (!options || typeof options !== 'object' || Array.isArray(options)) throw new TypeError('Invalid socket options');
    if (typeof callback !== 'function') throw new TypeError('Socket listener must be a function');
    let closed = false;
    let ended = false;
    const socket = {
        send(data) {
            if (closed) return false;
            if (typeof data === 'string') return _socketSend(id, data, null) === true;
            const view = ArrayBuffer.isView(data);
            const buffer = view ? data.buffer : data;
            if (!(buffer instanceof ArrayBuffer)) throw new TypeError('send requires text or bytes');
            return _socketSend(id, null, view ? new Uint8Array(buffer, data.byteOffset, data.byteLength) : buffer) === true;
        },
        close() {
            if (closed) return;
            closed = true;
            _socketClose(id, false);
        },
        cancel() {
            if (ended) return;
            closed = true;
            ended = true;
            _socketClose(id, true);
        }
    };
    const id = _connect(url, options, event => {
        if (event.type === 'close') closed = ended = true;
        if (event.binary) event.content = new Uint8Array(event.content);
        const result = callback(socket, event);
        if (result && typeof result.then === 'function') Promise.resolve(result).catch(error => {
            socket.cancel();
            console.error(error?.message || String(error));
        });
    });
    if (!Number.isInteger(id) || id <= 0) throw new Error('WebSocket connection is unavailable');
    return socket;
}

function netRequest(url, options, socket) {
    const complete = options?.complete;
    if (!socket && complete == null) return Promise.resolve().then(() => _httpAsync(url, Object.assign({}, options, {complete: undefined}))).catch(netError);
    return new Promise(resolve => (socket ? _ws : _http)(url, Object.assign({}, options, {
        complete: res => {
            try {
                complete?.(res);
            } finally {
                resolve(res);
            }
        }
    }))).catch(netError);
}

function netError(error) {
    return {
        code: '', headers: {}, content: '',
        error: `${error?.name || 'Error'}: ${error?.message || String(error)}`
    };
}

async function netSleep(ms, id) {
    if (!Number.isSafeInteger(ms) || ms < 0) throw new RangeError('Invalid sleep duration');
    await _sleep(ms, id);
}

function netCached(key, options, loader, id, background = false) {
    return new Promise((resolve, reject) => {
        validateNetCache(key, options, loader);
        const settings = background ? Object.assign({}, options, {stale: false}) : options;
        const accepted = _cached(key, JSON.stringify(settings), id, ticket => netCacheLoad(ticket, loader), (json, error) => {
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

function validateNetCache(key, options, loader) {
    if (typeof key !== 'string' || !key) throw new TypeError('Invalid cache key');
    if (!options || !Number.isSafeInteger(options.ttl) || options.ttl < 0) throw new RangeError('Invalid cache TTL');
    for (const name of ['stale', 'persist']) if (name in options && typeof options[name] !== 'boolean') throw new TypeError(`Invalid cache ${name}`);
    if (typeof loader !== 'function') throw new TypeError('Cache loader must be a function');
}

function netRefresh(key, options, loader, id = null) {
    netCached(key, options, loader, id, true).catch(error => console.error(`Cache ${key}: ${error.message}`));
}

function netDownload(url, path, options = {}) {
    return new Promise((resolve, reject) => {
        if (typeof path !== 'string' || !path) throw new TypeError('Invalid download path');
        if (!options || typeof options !== 'object' || Array.isArray(options)) throw new TypeError('Invalid download options');
        _download(url, path, options, error => error ? reject(new Error(String(error))) : resolve());
    });
}

function getProxyUrl(params = {}) {
    if (!params || typeof params !== 'object' || Array.isArray(params)) throw new TypeError('Proxy parameters require an object');
    if ('do' in params || 'siteKey' in params) throw new TypeError('Proxy parameters cannot override routing');
    const url = proxyUrl(params);
    if (typeof url !== 'string') throw new Error('Proxy URL is unavailable');
    return url;
}

async function netBatch(requests, limit = 4, client = net) {
    if (!Array.isArray(requests) || !Number.isSafeInteger(limit) || limit < 1) throw new TypeError('Invalid batch options');
    for (const item of requests) {
        if (!item || typeof item.url !== 'string' || item.options != null && (typeof item.options !== 'object' || Array.isArray(item.options))) throw new TypeError('Invalid batch request');
    }
    return netMap(requests, item => client.http(item.url, item.options), limit);
}

async function netMap(items, mapper, limit = 4) {
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

function netClearCache(key, id) {
    if (key != null && (typeof key !== 'string' || !key)) throw new TypeError('Invalid cache key');
    const error = _clearCache(key ?? null, id);
    if (error !== '') throw new Error(error || 'Network cache is unavailable');
}

function netPeek(key, id) {
    if (typeof key !== 'string' || !key) throw new TypeError('Invalid cache key');
    const result = JSON.parse(_peek(key, id));
    if (result.error) throw new Error(result.error);
    return result.found ? result.value : null;
}

async function netJson(url, options = {}) {
    if (options.buffer) throw new TypeError('JSON requests require text content');
    const result = await net.http(url, options);
    if (result.error) throw new Error(result.error);
    if (result.code < 200 || result.code >= 300) throw new Error(`HTTP ${result.code}`);
    return JSON.parse(result.content);
}

function localCall(operation, key, value) {
    if (typeof key !== 'string' || !key) throw new TypeError('Invalid Local key');
    const result = JSON.parse(_local(operation, key, value ?? null));
    if (result.error) throw new Error(result.error);
    return result;
}

globalThis.local = {
    get(key, fallback = null) {
        const result = localCall('get', key);
        return result.found ? result.value : fallback;
    },
    set: (key, value) => localCall('set', key, JSON.stringify(value, netCacheValue)),
    delete: key => localCall('delete', key)
};

globalThis.js = {
    eval(source, options = {}) {
        if (typeof source !== 'string' || !options || typeof options !== 'object' || Array.isArray(options)) throw new TypeError('Script requires source and options');
        return evalJs(source, JSON.parse(JSON.stringify(options, netCacheValue)));
    }
};

function link(name, target) {
    if (!target || typeof target !== 'object' || Array.isArray(target)) throw new TypeError('link requires a target object');
    return `[a=cr:${JSON.stringify(target, netCacheValue)}/]${name}[/a]`;
}

function page(items, pg, limit, total) {
    pg = Math.max(1, Number(pg));
    if (!Number.isSafeInteger(pg) || !Number.isSafeInteger(limit) || limit < 1) throw new RangeError('Invalid page options');
    if (total == null) {
        total = items.length;
        items = items.slice((pg - 1) * limit, pg * limit);
    }
    if (!Number.isSafeInteger(total) || total < 0) throw new RangeError('Invalid page total');
    return {list: items, page: pg, pagecount: Math.max(1, Math.ceil(total / limit)), limit, total};
}

function netCacheLoad(ticket, loader) {
    return Promise.resolve().then(loader).then(value => JSON.stringify(value, netCacheValue)).then(
        json => _cacheResolve(ticket, json, null),
        error => _cacheResolve(ticket, null, error?.message || String(error))
    );
}

function netCacheValue(key, item) {
    const type = Object.prototype.toString.call(item);
    if (item === undefined || typeof item === 'function' || typeof item === 'symbol'
        || typeof item === 'number' && !Number.isFinite(item)
        || type === '[object Number]' && !Number.isFinite(Number(item))
        || type === '[object Map]' || type === '[object Set]') throw new TypeError('Cached value must be JSON');
    return item;
}

function session() {
    const id = _netSession();
    if (!Number.isInteger(id) || id <= 0) throw new Error('Network session is unavailable');
    let closed = false;
    const route = options => Object.assign(Object.create(null), options, {__net: id, toJSON: undefined});
    const active = () => {
        if (closed) throw new Error('Network session is closed');
    };
    return {
        req: (url, options) => net.req(url, route(options)),
        http: (url, options) => net.http(url, route(options)),
        async stream(source, options) {
            if (typeof source !== 'string') active();
            return net.stream(source, typeof source === 'string' ? route(options) : options);
        },
        openStream: (url, options) => net.openStream(url, route(options)),
        ws: (url, options) => net.ws(url, route(options)),
        connect: (url, options, callback) => net.connect(url, route(options), callback),
        sleep: ms => netSleep(ms, id),
        cached: (key, options, loader) => netCached(key, options, loader, id),
        refresh: (key, options, loader) => netRefresh(key, options, loader, id),
        batch: (requests, limit) => netBatch(requests, limit, {http: (url, options) => net.http(url, route(options))}),
        map(items, mapper, limit) {
            active();
            if (typeof mapper !== 'function') throw new TypeError('Invalid map options');
            return netMap(items, (item, index) => {
                active();
                return mapper(item, index);
            }, limit);
        },
        download: (url, path, options) => netDownload(url, path, route(options)),
        peek: key => netPeek(key, id),
        clearCache: key => netClearCache(key, id),
        json: (url, options) => netJson(url, route(options)),
        getCookie(url) {
            active();
            const cookie = _netGetCookie(url, id);
            if (typeof cookie !== 'string') throw new Error('Network session is unavailable');
            return cookie;
        },
        setCookie(url, cookie) {
            active();
            const result = _netSetCookie(url, cookie, id);
            if (typeof result !== 'boolean') throw new Error('Network session is unavailable');
            return result;
        },
        close() {
            if (closed) return;
            closed = true;
            if (_netClose(id) !== true) throw new Error('Network session is unavailable');
        }
    };
}

globalThis.net = {
    req, http, stream: netStream, openStream: netOpenStream, json: netJson, ws, connect,
    batch: netBatch, map: netMap, refresh: netRefresh, download: netDownload,
    sleep: ms => netSleep(ms, null),
    cached: (key, options, loader) => netCached(key, options, loader, null),
    peek: key => netPeek(key, null),
    clearCache: key => netClearCache(key, null),
    session, getCookie, setCookie, getUserAgent
};
for (const name of ['sleep', 'cached', 'clearCache']) globalThis[name] = net[name];

function defineGlobalAlias(name) {
    const descriptor = Object.getOwnPropertyDescriptor(globalThis, name);
    if (descriptor && !descriptor.configurable) return;
    Object.defineProperty(globalThis, name, {
        enumerable: true,
        configurable: true,
        get() {
            return globalThis;
        },
        set() {}
    });
}

['global', 'window', 'self'].forEach(defineGlobalAlias);
