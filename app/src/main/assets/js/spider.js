const SPIDER_METHODS = [
    ['home', '[true]', 'home(filter)'], ['homeVod', '[]', 'homeVod()'],
    ['category', '["","1",true,{}]', 'category(tid, page, filter, extend)'],
    ['search', '["",false,"1"]', 'search(keyword, quick, page)'],
    ['detail', '[[""]]', 'detail([ids])'], ['play', '["","",[]]', 'play(flag, id, vipFlags)'],
    ['action', '[""]', 'action(value)'], ['live', '[""]', 'live(url)'],
    ['sniffer', '[]', 'sniffer()'], ['isVideo', '[""]', 'isVideo(url)'], ['proxy', '[{}]', 'proxy(params)']
];

const SPIDER_PARAMETERS = {
    home: [['filter', 'boolean']], homeVod: [], category: [['category', 'string'], ['page', 'string'], ['filter', 'boolean'], ['extend', 'object']],
    search: [['keyword', 'string'], ['quick', 'boolean'], ['page', 'string']], detail: [['ids', 'array']], play: [['flag', 'string'], ['id', 'string'], ['vipFlags', 'array']],
    action: [['value', 'string']], live: [['url', 'string']], sniffer: [], isVideo: [['url', 'string']], proxy: [['params', 'object']]
};

const SPIDER_SYMBOLS = {
    py: ['()', '[]', '{}', '""', "''", ':', '=', '#', '.', ',', '_'],
    js: ['()', '{}', '[]', '""', "''", '``', ':', ';', '=', '=>', '.', ',', '//'],
    java: ['()', '{}', '[]', '""', "''", ';', '=', '.', ',', '<', '>'],
    json: ['{}', '[]', '""', ':', ',']
};

function spiderParameterValue(value, type) {
    const parsed = type === 'string' ? value : JSON.parse(value);
    if (type === 'boolean' && typeof parsed !== 'boolean' || type === 'array' && (!Array.isArray(parsed) || parsed.some(value => typeof value !== 'string')) || type === 'object' && (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))) throw new Error('Invalid argument');
    return parsed;
}

function spiderParameters(method, text) {
    try {
        const args = JSON.parse(text), schema = SPIDER_PARAMETERS[method];
        if (!schema || !Array.isArray(args) || args.length !== schema.length && !(method === 'search' && args.length === 2)) return null;
        return args.map((value, index) => spiderParameter(schema[index], value));
    } catch {
        return null;
    }
}

function spiderParameter([name, type], value) {
    const text = type === 'string' ? value : JSON.stringify(value, null, 2);
    if (type === 'string' && typeof value !== 'string') throw new Error('Invalid argument');
    spiderParameterValue(text, type);
    return {name, type, text};
}

function spiderFormat(text, language) {
    return new Promise((resolve, reject) => {
        const worker = new Worker('/js/spider-format/worker.js', {type: 'module'});
        const finish = (error, text) => {
            clearTimeout(timer);
            worker.terminate();
            error ? reject(new Error(error)) : resolve(text);
        };
        const timer = setTimeout(() => finish(t('spiderFormatTimeout')), 60000);
        worker.onmessage = ({data}) => finish(data.error ? t('spiderFormatFailed') + '\n' + data.error : null, data.text);
        worker.onerror = event => finish(t('spiderFormatUnavailable') + (event.message ? '\n' + event.message : ''));
        worker.postMessage({text, language});
    });
}

function spiderSourceKey({api, jar, ext}) {return JSON.stringify([api.trim(), jar.trim(), ext]);}

class SpiderHistory {
    constructor(storage) {
        this.storage = storage;
        this.entries = [];
        this.error = null;
        try {
            const entries = JSON.parse(storage.getItem('tv.spider.history.v1') || '[]');
            if (!Array.isArray(entries)) throw new Error('Invalid crawler history');
            for (const entry of entries.slice(0, 8)) this.addEntry(entry);
        } catch (error) {
            this.error = error;
        }
    }

    addEntry(entry) {
        if (!entry || !['api', 'jar', 'ext'].every(key => typeof entry[key] === 'string') || !entry.api.trim()) return;
        const method = SPIDER_METHODS.some(([name]) => name === entry.method) ? entry.method : 'home';
        const parameters = Object.fromEntries(SPIDER_METHODS.filter(([name]) => typeof entry.parameters?.[name] === 'string').map(([name]) => [name, entry.parameters[name]]));
        const value = {api: entry.api.trim(), jar: entry.jar.trim(), ext: entry.ext, method, parameters};
        if (!this.find(value)) this.entries.push(value);
    }

    find(config) {return this.entries.find(entry => spiderSourceKey(entry) === spiderSourceKey(config));}

    remember(config, method, args) {
        const entry = this.find(config) || {api: config.api.trim(), jar: config.jar.trim(), ext: config.ext, parameters: {}};
        entry.method = method;
        entry.parameters[method] = args;
        this.entries = [entry, ...this.entries.filter(value => value !== entry)].slice(0, 8);
    }

    update(config, method, args) {
        const entry = this.find(config);
        if (entry) {
            entry.method = method;
            entry.parameters[method] = args;
        }
    }

    write() {
        try {
            this.storage.setItem('tv.spider.history.v1', JSON.stringify(this.entries));
            this.error = null;
        } catch (error) {
            this.error = error;
        }
    }
}

function spiderDiff(before, after) {
    const changes = [];
    let left, right;
    try {
        left = JSON.parse(before);
        right = JSON.parse(after);
    } catch {
        return {changes: before === after ? [] : [{path: '$', kind: 'changed', before, after}], truncated: false};
    }
    const visit = (a, b, path) => {
        if (a === b || changes.length > 100) return;
        if (a && b && typeof a === 'object' && typeof b === 'object' && Array.isArray(a) === Array.isArray(b)) {
            for (const key of new Set([...Object.keys(a), ...Object.keys(b)])) {
                const next = path + '/' + key.replace(/~/g, '~0').replace(/\//g, '~1');
                if (!Object.prototype.hasOwnProperty.call(a, key)) changes.push({path: next, kind: 'added', after: b[key]});
                else if (!Object.prototype.hasOwnProperty.call(b, key)) changes.push({path: next, kind: 'removed', before: a[key]});
                else visit(a[key], b[key], next);
                if (changes.length > 100) break;
            }
        } else changes.push({path: path || '$', kind: 'changed', before: a, after: b});
    };
    visit(left, right, '');
    return {changes: changes.slice(0, 100), truncated: changes.length > 100};
}

class SpiderResults {
    constructor() {
        this.previous = null;
        this.comparison = null;
    }

    record(config, method, args, text) {
        const key = JSON.stringify([spiderSourceKey(config), method]), parameters = JSON.stringify(args);
        const previous = this.previous;
        this.comparison = previous?.key === key && !spiderDiff(previous.parameters, parameters).changes.length ? {before: previous.text, after: text} : null;
        this.previous = {key, parameters, text};
    }
}

class SpiderCases {
    constructor(storage) {
        this.storage = storage;
        this.entries = [];
        this.error = null;
        try {
            this.readEntries(storage);
        } catch (error) {
            this.error = error;
        }
    }

    readEntries(storage) {
        const entries = JSON.parse(storage.getItem('tv.spider.cases.v1') || '[]');
        if (!Array.isArray(entries)) throw new Error('Invalid crawler cases');
        for (const entry of entries.slice(0, 32)) this.addEntry(entry);
    }

    addEntry(entry) {
        if (typeof entry?.source !== 'string' || typeof entry.name !== 'string' || !entry.name.trim() || entry.name.length > 80 || !SPIDER_METHODS.some(([name]) => name === entry.method)) return;
        let expected;
        try {
            if (!Array.isArray(JSON.parse(entry.args))) return;
            expected = spiderExpected(entry.expected === undefined ? {} : entry.expected);
        } catch {
            return;
        }
        if (!this.entries.some(value => value.source === entry.source && value.name === entry.name.trim())) this.entries.push({source: entry.source, name: entry.name.trim(), method: entry.method, args: entry.args, expected});
    }

    list(config) {return this.entries.filter(entry => entry.source === spiderSourceKey(config));}

    save(config, name, method, args, conditions = {}) {
        name = name.trim();
        if (!name || name.length > 80 || !SPIDER_METHODS.some(([value]) => value === method) || !Array.isArray(JSON.parse(args))) throw new Error('Invalid crawler case');
        const expected = spiderExpected(conditions);
        const source = spiderSourceKey(config);
        const existing = this.entries.find(entry => entry.source === source && entry.name === name);
        if (!existing && this.entries.length >= 32) throw new Error('Crawler case limit reached');
        if (existing) Object.assign(existing, {method, args, expected});
        else this.entries.push({source, name, method, args, expected});
        this.write();
    }

    remove(entry) {
        this.entries = this.entries.filter(value => value !== entry);
        this.write();
    }

    write() {
        try {
            this.storage.setItem('tv.spider.cases.v1', JSON.stringify(this.entries));
            this.error = null;
        } catch (error) {
            this.error = error;
        }
    }
}

function spiderPointer(path) {return typeof path === 'string' && (path === '$' || /^\/(?:[^~]|~[01])*$/.test(path));}

function spiderField(value, path) {
    if (path === '$') return {found: true, value};
    for (const key of path.slice(1).split('/').map(value => value.replace(/~1/g, '/').replace(/~0/g, '~'))) {
        if (value === null || typeof value !== 'object' || !Object.prototype.hasOwnProperty.call(value, key)) return {found: false};
        value = value[key];
    }
    return {found: true, value};
}

function spiderExpected(value) {
    if (!value || typeof value !== 'object' || Array.isArray(value) || Object.keys(value).some(key => !['minItems', 'required', 'equals'].includes(key))) throw new Error('Invalid expected result');
    if (value.minItems !== undefined && (!Number.isSafeInteger(value.minItems) || value.minItems < 0)) throw new Error('Invalid minimum item count');
    if (value.required !== undefined && (!Array.isArray(value.required) || !value.required.every(spiderPointer))) throw new Error('Invalid required fields');
    if (value.equals !== undefined && (!value.equals || typeof value.equals !== 'object' || Array.isArray(value.equals) || !Object.keys(value.equals).every(spiderPointer))) throw new Error('Invalid expected fields');
    const expected = {};
    if (value.minItems !== undefined) expected.minItems = value.minItems;
    if (value.required?.length) expected.required = value.required;
    if (value.equals && Object.keys(value.equals).length) expected.equals = value.equals;
    return JSON.parse(JSON.stringify(expected));
}

function spiderAssert(text, expected) {
    if (expected.minItems === undefined && !expected.required?.length && !Object.keys(expected.equals || {}).length) return [];
    let value;
    try {
        value = JSON.parse(text);
    } catch {
        return [{path: '$', code: 'json'}];
    }
    const issues = [];
    if (expected.minItems !== undefined && (!Array.isArray(value?.list) || value.list.length < expected.minItems)) issues.push({path: '/list', code: 'minItems', expected: expected.minItems, actual: Array.isArray(value?.list) ? value.list.length : null});
    for (const path of expected.required || []) {
        if (issues.length > 100) break;
        if (!spiderField(value, path).found) issues.push({path, code: 'required'});
    }
    for (const [path, target] of Object.entries(expected.equals || {})) {
        if (issues.length > 100) break;
        const actual = spiderField(value, path);
        if (!actual.found || spiderDiff(JSON.stringify(target), JSON.stringify(actual.value)).changes.length) issues.push({path, code: 'equals', expected: target, actual: actual.found ? actual.value : null});
    }
    return issues;
}

function spiderChecks(method, text, expected = {}) {
    const validation = spiderValidate(method, text), issues = [...validation.issues, ...spiderAssert(text, expected)];
    return {checked: validation.checked || expected.minItems !== undefined || !!expected.required?.length || !!Object.keys(expected.equals || {}).length, issues: issues.slice(0, 100), truncated: validation.truncated || issues.length > 100};
}

function spiderRunKey(run) {
    const canonical = value => Array.isArray(value) ? value.map(canonical) : value && typeof value === 'object' ? Object.fromEntries(Object.keys(value).sort().map(key => [key, canonical(value[key])])) : value;
    return JSON.stringify([spiderSourceKey(run.source), run.method, canonical(run.args)]);
}

class SpiderBaselines {
    constructor(storage) {
        this.storage = storage;
        this.entries = [];
        this.error = null;
        try {
            const entries = JSON.parse(storage.getItem('tv.spider.baselines.v1') || '[]');
            if (!Array.isArray(entries)) throw new Error('Invalid baselines');
            for (const entry of entries.slice(0, 16)) {
                if (!entry || typeof entry.key !== 'string' || typeof entry.text !== 'string' || typeof entry.time !== 'string' || !Array.isArray(entry.ignored) || !entry.ignored.every(spiderPointer)) continue;
                if (!this.entries.some(value => value.key === entry.key)) this.entries.push(entry);
            }
        } catch (error) {
            this.error = error;
        }
    }

    find(run) {return this.entries.find(entry => entry.key === spiderRunKey(run));}

    save(run, ignored) {
        if (!ignored.every(spiderPointer)) throw new Error('Invalid ignored fields');
        const previous = this.find(run);
        if (!previous && this.entries.length >= 16) throw new Error('Baseline limit reached');
        const entry = {key: spiderRunKey(run), text: run.result, ignored: [...new Set(ignored)], time: new Date().toISOString()};
        if (previous) Object.assign(previous, entry); else this.entries.push(entry);
        this.write();
    }

    remove(entry) {
        this.entries = this.entries.filter(value => value !== entry);
        this.write();
    }

    write() {
        try {
            this.storage.setItem('tv.spider.baselines.v1', JSON.stringify(this.entries));
            this.error = null;
        } catch (error) {
            this.error = error;
        }
    }
}

function spiderBaselineDiff(before, after, ignored) {
    if (!ignored.length) return spiderDiff(before, after);
    const exclude = text => {
        let value = JSON.parse(text);
        for (const path of ignored) {
            if (path === '$') {
                value = null;
                continue;
            }
            const parts = path.slice(1).split('/'), last = parts.pop().replace(/~1/g, '/').replace(/~0/g, '~');
            const parent = parts.length ? spiderField(value, '/' + parts.join('/')) : {found: true, value};
            if (parent.found && parent.value && typeof parent.value === 'object') delete parent.value[last];
        }
        return JSON.stringify(value);
    };
    return spiderDiff(exclude(before), exclude(after));
}

async function spiderBatch(entries, call, changed, active = () => true) {
    const steps = entries.map(entry => ({source: entry.source, name: entry.name, method: entry.method, args: JSON.parse(entry.args), expected: entry.expected || {}, state: 'pending'}));
    for (const step of steps) {
        if (!active()) {
            step.state = 'cancelled';
            changed(steps);
            continue;
        }
        step.state = 'running';
        changed(steps);
        try {
            const run = await call(step.method, step.args, step.expected, step.name);
            Object.assign(step, {state: active() ? run.status : 'cancelled', text: active() ? run.result : '', elapsed: run.elapsed, issues: run.issues, truncated: run.truncated, requests: active() ? run.requests : undefined});
        } catch (error) {
            step.state = active() ? 'failed' : 'cancelled';
            step.text = error.message || String(error);
        }
        changed(steps);
    }
    return steps;
}

function spiderRetryCases(steps, source) {
    return steps.filter(step => ['failed', 'timedout'].includes(step.state) && step.source === spiderSourceKey(source)).map(step => ({source: step.source, name: step.name, method: step.method, args: JSON.stringify(step.args), expected: spiderExpected(step.expected || {})}));
}

function spiderSearch(text, query) {
    let value, json = true;
    try {
        value = JSON.parse(text);
    } catch {
        value = text;
        json = false;
    }
    const matches = [], search = query.trim().toLocaleLowerCase();
    if (!search) return {value, json, matches, truncated: false};
    if (json) {
        const pending = [{path: '$', label: '$', value}];
        while (pending.length && matches.length <= 100) {
            const entry = pending.pop(), object = entry.value !== null && typeof entry.value === 'object';
            if (entry.label.toLocaleLowerCase().includes(search) || !object && String(entry.value).toLocaleLowerCase().includes(search)) matches.push(entry);
            if (object) for (const key of Object.keys(entry.value).reverse()) pending.push({path: (entry.path === '$' ? '' : entry.path) + '/' + key.replace(/~/g, '~0').replace(/\//g, '~1'), label: key, value: entry.value[key]});
        }
    } else {
        const lines = text.split('\n');
        for (let index = 0; index < lines.length && matches.length <= 100; index++) if (lines[index].toLocaleLowerCase().includes(search)) matches.push({line: index + 1, value: lines[index]});
    }
    return {value, json, matches: matches.slice(0, 100), truncated: matches.length > 100};
}

function spiderReport(records, format, title = 'Crawler test report') {
    const report = {version: 1, createdAt: new Date().toISOString(), records};
    if (format === 'json') return JSON.stringify(report, null, 2);
    const escape = text => String(text).replace(/[&<>"']/g, value => ({'&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'}[value]));
    const rows = records.map(record => '<section><h2>' + escape(record.name || record.method) + '</h2><p>' + escape(record.status) + ' · ' + escape(record.elapsed) + ' ms · ' + escape(record.time) + '</p><pre>' + escape(JSON.stringify({source: record.source, sourceVersion: record.sourceVersion, method: record.method, args: record.args, expected: record.expected, issues: record.issues, truncated: record.truncated, requests: record.requests}, null, 2)) + '</pre><pre>' + escape(record.result) + '</pre></section>').join('');
    return '<!doctype html><html><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>' + escape(title) + '</title><style>:root{color-scheme:light dark;--surface:#F4F7F4;--card:#FFFFFF;--text:#15140E;--outline:#C7C7C0;--primary:#006C4C}@media(prefers-color-scheme:dark){:root{--surface:#101510;--card:#1C211D;--text:#DEE4DF;--outline:#414942;--primary:#6CDBA4}}*{box-sizing:border-box}body{font:16px/1.6 Roboto,-apple-system,system-ui,sans-serif;background:var(--surface);color:var(--text);margin:24px auto;padding:0 20px;max-width:880px}section{background:var(--card);border:1px solid var(--outline);border-radius:24px;padding:24px;margin:24px 0}pre{white-space:pre-wrap;overflow-wrap:anywhere;font:14px/1.6 monospace}h1{font-size:24px;color:var(--primary)}h2{font-size:20px;overflow-wrap:anywhere}</style><h1>' + escape(title) + '</h1><p>' + escape(report.createdAt) + '</p>' + rows + '</html>';
}

function spiderErrorLocation(text, path, root = '') {
    const name = path.split('/').at(-1);
    const entry = /^[\p{ID_Start}_][\p{ID_Continue}]*$/u.test(name.slice(0, -3)) ? name : 'spider.py';
    const relative = root && path.startsWith(root + '/') ? path.slice(root.length + 1) : '';
    const matches = file => {
        if (file === path || file === name) return true;
        try {
            file = decodeURIComponent(file);
        } catch {
            return false;
        }
        return file === path || file === name || relative && file.endsWith('/file/' + relative);
    };
    const pythonFile = file => matches(file) || file === entry || file.match(/\/cache\/py\/[a-f0-9]{64}\/[a-f0-9]{32}\/([^/]+)$/)?.[1] === entry;
    const python = name.endsWith('.py') ? [...text.matchAll(/File "([^"]+)", line (\d+)/g)].filter(match => pythonFile(match[1])) : [];
    if (python.length && Number(python.at(-1)[2]) > 0) return {line: Number(python.at(-1)[2]), column: 1};
    for (const match of text.matchAll(/(?:\(|\s|^)([^\s()]+\.js)(?::(\d+))(?::(\d+))?/g)) {
        if (matches(match[1]) && Number(match[2]) > 0) return {line: Number(match[2]), column: Number(match[3]) || 1};
    }
    for (const match of text.matchAll(/at <python>\.([^\s(]+)\(([^)]+\.py):(\d+)\)/g)) {
        const call = match[1].replace(/^_tv_py_[a-f0-9]+\./, '');
        if (name.endsWith('.py') && call.startsWith(entry.slice(0, -3) + '.') && pythonFile(match[2]) && Number(match[3]) > 0) return {line: Number(match[3]), column: 1};
    }
    return null;
}

function spiderPlaybackUrls(url) {
    if (typeof url === 'string') return url.trim() ? [url] : [];
    if (Array.isArray(url) && url.length % 2 === 0 && url.every((value, index) => typeof value === 'string' && (index % 2 === 0 || value.trim()))) return url.filter((value, index) => index % 2);
    if (url && Array.isArray(url.values) && url.values.every(value => typeof value?.v === 'string' && value.v.trim())) {
        const position = url.position ?? 0;
        if (Number.isInteger(position) && position >= 0 && position < url.values.length) return url.values.map(value => value.v);
    }
    return [];
}

function spiderValidate(method, text) {
    const issues = [];
    let truncated = false;
    const add = (path, code) => {if (issues.length < 100) issues.push({path, code}); else truncated = true;};
    if (method === 'action' || method === 'live') return {checked: false, issues};
    let result;
    try {
        result = JSON.parse(text);
    } catch {
        return {checked: true, issues: [{path: '$', code: 'json'}]};
    }
    if (method === 'sniffer' || method === 'isVideo') {
        if (typeof result !== 'boolean') add('$', 'boolean');
        return {checked: true, issues};
    }
    const object = value => value !== null && typeof value === 'object' && !Array.isArray(value);
    const value = field => (typeof field === 'string' || typeof field === 'number') && String(field).trim() !== '';
    if (!object(result)) return {checked: true, issues: [{path: '$', code: 'object'}]};
    if (method === 'play') {
        if (!spiderPlaybackUrls(result.url).length) add('/url', 'url');
        if (result.parse != null && ![0, 1, '0', '1'].includes(result.parse)) add('/parse', 'parse');
        let header = result.header;
        if (typeof header === 'string') {
            try {
                header = JSON.parse(header);
            } catch {
                header = null;
            }
        }
        if (result.header != null && result.header !== '' && (!object(header) || Object.values(header).some(item => !['string', 'number', 'boolean'].includes(typeof item)))) add('/header', 'object');
    } else if (method === 'proxy') {
        if (!Number.isInteger(result.status) || result.status < 100 || result.status > 599) add('/status', 'status');
        if (typeof result.mime !== 'string') add('/mime', 'string');
        if (typeof result.body !== 'string' || result.bodyEncoding !== 'base64') add('/body', 'string');
    } else {
        if (method === 'home' && !('class' in result) && !('list' in result)) add('/class', 'array');
        if ('class' in result) {
            if (!Array.isArray(result.class)) {
                add('/class', 'array');
            } else {
                const ids = new Set();
                result.class.forEach((item, index) => validateClassItem(item, index, ids));
            }
        }
        if (method !== 'home' || 'list' in result) {
            if (!Array.isArray(result.list)) add('/list', 'array');
            else result.list.forEach(validateListItem);
        }
        if (method === 'category' || method === 'search') {
            for (const key of ['page', 'pagecount', 'limit', 'total']) {
                if (result[key] !== undefined && (!/^[0-9]+$/.test(String(result[key])) || !Number.isSafeInteger(Number(result[key])) || Number(result[key]) < (key === 'page' ? 1 : 0))) add('/' + key, 'number');
            }
        }
    }
    function validateClassItem(item, index, ids) {
        if (!object(item)) {
            add('/class/' + index, 'object');
            return;
        }
        for (const key of ['type_id', 'type_name']) if (!value(item[key])) add('/class/' + index + '/' + key, 'required');
        if (ids.has(String(item.type_id))) add('/class/' + index + '/type_id', 'duplicate');
        ids.add(String(item.type_id));
    }

    function validateListItem(item, index) {
        const path = '/list/' + index;
        if (!object(item)) {
            add(path, 'object');
            return;
        }
        if (!value(item.vod_id) && !value(item.action)) add(path + '/vod_id', 'required');
        if (!value(item.vod_name)) add(path + '/vod_name', 'required');
        if (method === 'detail' && ('vod_play_from' in item || 'vod_play_url' in item)) {
            if (typeof item.vod_play_from !== 'string' || typeof item.vod_play_url !== 'string') add(path + '/vod_play_url', 'string');
            else if (item.vod_play_from.split('$$$').length !== item.vod_play_url.split('$$$').length) add(path + '/vod_play_url', 'lines');
        }
    }

    return {checked: true, issues, truncated};
}

async function spiderSuite(call, changed, active = () => true) {
    const steps = ['home', 'category', 'detail', 'play'].map(method => ({method, state: 'pending'}));
    let args = [true], proceed = true;
    for (const step of steps) {
        if (!proceed || !active()) {
            step.state = active() ? 'skipped' : 'cancelled';
            changed(steps);
            continue;
        }
        step.state = 'running';
        step.args = args;
        changed(steps);
        try {
            const result = await call(step.method, args);
            if (!active() || !result) {
                step.state = 'cancelled';
                proceed = false;
                changed(steps);
                continue;
            }
            applySuiteResult(step, result);
            if (step.state !== 'passed') {
                proceed = false;
            } else if (step.method !== 'play') {
                const next = {home: 'category', category: 'detail', detail: 'play'}[step.method];
                const choice = spiderChoices(step.text).find(value => value.method === next);
                if (choice) args = choice.args;
                else proceed = false;
            }
        } catch (error) {
            step.state = active() ? 'failed' : 'cancelled';
            step.text = error.message || String(error);
            proceed = false;
        }
        changed(steps);
    }
    return steps;
}

function applySuiteResult(step, result) {
    Object.assign(step, {text: result.result, elapsed: result.elapsed});
    const validation = spiderValidate(step.method, step.text);
    step.state = result.state === 'timedout' ? 'timedout' : result.state === 'done' && !validation.issues.length ? 'passed' : 'failed';
    step.issues = validation.issues;
    step.truncated = validation.truncated;
}

class SpiderAutoSave {
    constructor(write, status) {
        this.write = write;
        this.status = status;
        this.revision = this.saved = 0;
        this.pending = null;
        this.timer = null;
    }

    reset(source) {
        clearTimeout(this.timer);
        this.text = source.text;
        this.version = source.version;
        this.revision = this.saved = 0;
        this.status('saved');
    }

    update(text) {
        if (text === this.text) return;
        this.text = text;
        this.revision++;
        this.status('saving');
        clearTimeout(this.timer);
        this.timer = setTimeout(() => this.flush().catch(() => {}), 1000);
    }

    async flush() {
        clearTimeout(this.timer);
        if (this.pending) {
            await this.pending;
            return this.flush();
        }
        if (this.revision === this.saved) return;
        const revision = this.revision;
        this.pending = this.write({version: this.version, text: this.text}).then(result => {
            if (!result || typeof result.version !== 'string') throw new Error('Invalid save response');
            this.version = result.version;
            this.saved = revision;
            this.status(this.saved === this.revision ? 'saved' : 'saving');
        }).catch(error => {
            this.status('error', error);
            throw error;
        }).finally(() => {this.pending = null;});
        await this.pending;
        if (this.saved !== this.revision) return this.flush();
    }

    get dirty() {return this.revision !== this.saved;}
}

function spiderChoices(text) {
    let result;
    try {
        result = JSON.parse(text);
    } catch {
        return [];
    }
    if (!result || typeof result !== 'object') return [];
    const classes = Array.isArray(result.class) && result.class.length ? result.class : null;
    const list = classes || result.list;
    if (!Array.isArray(list)) return [];
    const choices = [];
    const value = field => field === null || field === undefined ? '' : String(field);
    const add = (label, method, args) => choices.push({label, method, args});
    for (const item of list) addSpiderChoice(item, classes, value, add);
    return choices;
}

function addSpiderChoice(item, classes, value, add) {
    if (!item || typeof item !== 'object') return;
    const id = value(classes ? item.type_id : item.vod_id);
    const name = value(classes ? item.type_name : item.vod_name);
    if (classes) {
        if (id) add(name || id, 'category', [id, '1', true, {}]);
    } else if (value(item.action)) {
        add(name || value(item.action), 'action', [value(item.action)]);
    } else if (item.vod_tag === 'folder' || item.cate !== undefined && item.cate !== null) {
        if (id) add(name || id, 'category', [id, '1', true, {}]);
    } else if (value(item.vod_play_url)) {
        addSpiderPlaybackChoices(item, value, add);
    } else if (id) {
        add(name || id, 'detail', [[id]]);
    }
}

function addSpiderPlaybackChoices(item, value, add) {
    const flags = value(item.vod_play_from).split('$$$');
    const lines = value(item.vod_play_url).split('$$$');
    for (let line = 0; line < Math.min(flags.length, lines.length); line++) {
        const flag = flags[line].trim();
        if (!flag || !lines[line]) continue;
        addSpiderEpisodes(flag, lines[line], add);
    }
}

function addSpiderEpisodes(flag, line, add) {
    line.split('#').forEach((episode, index) => {
        const separator = episode.indexOf('$');
        const url = (separator < 0 ? episode : episode.slice(separator + 1)).trim();
        const label = separator < 0 ? String(index + 1).padStart(2, '0') : episode.slice(0, separator).trim();
        if (url) add(flag + ' · ' + (label || String(index + 1).padStart(2, '0')), 'play', [flag, url, []]);
    });
}

async function initSpider() {
    const el = name => document.getElementById('spider_' + name);
    const dialog = el('dialog');
    let token, rootPath, source, info = {loaded: false}, busy = false, generation = 0, choices = [], reload = false, cancelling = false, disconnected = false, conflict = false;
    let editor, resultMethod, suite, lastSuite, lastRun, lastBatch, dialogMode = '', caseName = '', timeout = 30, formatting = false, parameterInputs = [], fullscreen = false, editorScroll = 0;
    const records = [];
    const history = new SpiderHistory({getItem: key => window.localStorage.getItem(key), setItem: (key, value) => window.localStorage.setItem(key, value)});
    const cases = new SpiderCases(history.storage);
    const baselines = new SpiderBaselines(history.storage);
    try {
        const value = JSON.parse(history.storage.getItem('tv.spider.timeout.v1') || '30');
        if (Number.isInteger(value) && value >= 1 && value <= 300) timeout = value;
    } catch (error) {
        console.warn('Crawler timeout setting:', error);
    }
    const results = new SpiderResults();
    let historyTimer, searchTimer, inspectorText, operation;
    const config = () => ({api: el('api').value.trim(), jar: el('jar').value.trim(), ext: el('ext').value});
    const setStatus = (text, error = false) => {
        for (const name of ['status', 'editor_status']) {
            el(name).textContent = text;
            el(name).dataset.error = String(error);
            el(name).dataset.source = '';
        }
    };
    const showError = error => {
        const text = error.code === 'source_changed' ? t('spiderSourceChanged') : error.message || String(error);
        setStatus(text, true);
        if (error.code === 'source_changed') {
            conflict = true;
            refresh();
        }
        if (!dialog.open) return;
        let message = el('dialog_body').querySelector('[role="alert"]');
        if (!message) {
            message = document.createElement('p');
            message.className = 'spider-dialog-error';
            message.setAttribute('role', 'alert');
            el('dialog_body').prepend(message);
        }
        message.textContent = text;
        el('dialog_body').scrollTop = 0;
    };
    const api = async (path = '', body, method = body === undefined ? 'GET' : 'POST', signal) => {
        let response;
        try {
            response = await fetch('/spider-api' + path, {
                method, cache: 'no-store',
                headers: {'X-Spider-Session': token || '', ...(body instanceof FormData ? {} : {'Content-Type': 'application/json; charset=utf-8'})},
                body: body === undefined ? undefined : body instanceof FormData ? body : JSON.stringify(body), signal
            });
        } catch (error) {
            if (signal?.aborted) throw error;
            disconnected = true;
            refresh();
            throw new Error(t('spiderDisconnected'), {cause: error});
        }
        const value = await response.json();
        if (!response.ok) {
            if (response.status === 403) {
                disconnected = true;
                refresh();
            }
            const error = new Error(response.status === 403 ? t('spiderAccessDenied') + (value.error ? '\n' + value.error : '') : value.error || response.statusText);
            error.code = value.code;
            throw error;
        }
        return value;
    };
    const save = new SpiderAutoSave(async body => {
        const result = await api('/save', body);
        reload = true;
        info.loaded = false;
        refresh();
        return result;
    }, (state, error) => {
        if (state === 'saved') {
            conflict = false;
            if (el('editor_status').dataset.source === 'save') el('editor_status').dataset.error = 'false';
        } else if (error && error.code === 'source_changed') {
            conflict = true;
        }
        el('save_status').textContent = t({saved: 'spiderSaved', saving: 'spiderSaving', error: 'spiderUnsaved'}[state]);
        el('save_status').dataset.error = String(state === 'error');
        if (error) {
            showError(error);
            el('editor_status').dataset.source = 'save';
        }
        refresh();
    });

    function matches() {return info.loaded && info.api === el('api').value.trim() && info.ext === el('ext').value && info.jar === el('jar').value.trim();}
    function writeHistory() {
        clearTimeout(historyTimer);
        history.write();
        if (history.error) setStatus(t('spiderHistoryUnavailable'), true);
    }

    function rememberArguments() {
        history.update(config(), el('method').value, el('args').value);
        clearTimeout(historyTimer);
        historyTimer = setTimeout(writeHistory, 300);
    }

    function restoreArguments() {
        const entry = history.find(config());
        el('method').value = entry?.method || 'home';
        el('args').value = entry?.parameters[el('method').value] ?? SPIDER_METHODS.find(([name]) => name === el('method').value)[1];
        renderArguments();
    }

    function renderArguments() {
        updateChoice(el('method'));
        const fields = spiderParameters(el('method').value, el('args').value);
        el('arguments').hidden = fields !== null;
        parameterInputs = [];
        el('parameters').replaceChildren();
        if (!fields) return;
        for (const field of fields) appendParameter(field);
        refresh();
    }

    function appendParameter({name, type, text}) {
        const field = document.createElement('div'), label = document.createElement('label'), input = document.createElement(type === 'boolean' ? 'button' : type === 'string' && !/[\r\n]/.test(text) ? 'input' : 'textarea');
        field.className = 'md-field';
        input.className = 'md-input';
        input.id = 'spider_parameter_' + parameterInputs.length;
        label.htmlFor = input.id;
        label.textContent = t('spiderParam_' + name) + (type === 'object' || type === 'array' ? ' (JSON)' : '');
        if (type === 'array' || type === 'object') {
            input.rows = 2;
            input.classList.add('spider-json');
        }
        if (name === 'page') input.inputMode = 'numeric';
        input.spellcheck = false;
        input.autocomplete = 'off';
        input.value = text;
        if (type === 'boolean') {
            input.type = 'button';
            bindChoice(input, () => label.textContent, () => ['true', 'false'].map(value => ({value, label: value})));
        }
        const index = parameterInputs.length;
        input.addEventListener('input', () => updateParameter(input, type, index));
        field.append(label, input);
        el('parameters').append(field);
        parameterInputs.push(input);
    }

    function updateParameter(input, type, index) {
        try {
            const value = spiderParameterValue(input.value, type), args = JSON.parse(el('args').value);
            args[index] = value;
            el('args').value = JSON.stringify(args);
            input.setCustomValidity('');
            rememberArguments();
        } catch {
            input.setCustomValidity(t(type === 'object' ? 'spiderObjectInvalid' : 'spiderArrayInvalid'));
        }
    }

    function refresh() {
        const working = busy || !!suite?.running || formatting, java = el('api').value.trim().endsWith('.java');
        renderFeedback();
        for (const name of ['api', 'jar', 'ext', 'args', 'method', 'load', 'files', 'upload', 'more']) el(name).disabled = working || !token || disconnected;
        for (const input of parameterInputs) input.disabled = working || !token || disconnected;
        el('load').disabled ||= java;
        el('run').disabled = working || disconnected || !token || !el('api').value.trim() || java;
        el('run').hidden = working;
        el('cancel').hidden = !busy && !suite?.running;
        el('cancel').disabled = cancelling;
        el('status').dataset.busy = String(working);
        el('reconnect').hidden = !disconnected;
        el('reconnect').disabled = working;
        el('reopen').hidden = !conflict;
        el('reopen').disabled = working || disconnected;
        el('csp').hidden = !el('jar').value.trim();
        el('csp').disabled = working || !token || disconnected;
        el('edit_tab').disabled = working || !source || source.path !== el('api').value.trim();
        el('items').disabled = working || disconnected || !matches() || !choices.length;
        el('copy').disabled = !info.result;
        el('recent').disabled = working || !token || disconnected || !history.entries.length;
        el('compare').disabled = working || !matches() || info.state !== 'done' || !results.comparison && (!lastRun || !baselines.find(lastRun));
        el('versions').disabled = working || disconnected || !source || source.path !== config().api;
        el('save').disabled = working || disconnected || !source?.writable || conflict;
        el('format_args').disabled = working || !token || disconnected;
        el('format').disabled = working || disconnected || !source?.writable || conflict;
        for (const button of el('symbols').querySelectorAll('button')) button.disabled = el('format').disabled;
        el('fullscreen').disabled = !source;
        el('retest').disabled = working || disconnected || !source || source.path !== config().api || java || conflict;
        el('retest').hidden = java;
        el('java_note').hidden = !java;
        el('inspector').hidden = !info.result;
        if (inspectorText !== info.result) {
            inspectorText = info.result;
            el('result_search').value = '';
            renderInspection();
        }
        el('suite_cancel').hidden = !suite?.running;
        el('suite_cancel').disabled = cancelling;
        for (const name of ['location', 'check', 'playback']) el(name).disabled = working || disconnected;
        for (const button of [...el('dialog_body').querySelectorAll('button'), ...el('dialog_actions').querySelectorAll('.spider-dialog-action')]) button.disabled = working || disconnected || button.dataset.unavailable === 'true';
        el('load_label').textContent = t(info.loaded ? 'spiderReload' : 'spiderLoad');
    }

    function render(value) {
        info = value;
        busy = value.state === 'loading' || value.state === 'running';
        el('result').textContent = value.result || t('spiderEmpty');
        choices = spiderChoices(value.result);
        const key = {idle: 'spiderReady', loading: 'spiderLoading', loaded: 'spiderLoaded', running: 'spiderRunning', done: 'spiderDone', error: 'spiderFailed', cancelled: 'spiderCancelled', timedout: 'spiderTimedOut'}[value.state];
        setStatus(t(key) + (value.state === 'done' || value.state === 'loaded' ? ' · ' + value.elapsed + ' ms' : ''), value.state === 'error' || value.state === 'timedout');
        refresh();
    }

    async function work(path, body) {
        await save.flush();
        const request = operation = {generation: ++generation, controller: new AbortController()};
        busy = true;
        refresh();
        try {
            let value = await api(path, {...body, timeout}, 'POST', request.controller.signal);
            while (generation === request.generation) {
                render(value);
                if (!busy) return value;
                await new Promise(resolve => setTimeout(resolve, 150));
                value = await api('', undefined, 'GET', request.controller.signal);
            }
            if (request.cancellation) return await request.cancellation;
        } catch (error) {
            if (generation !== request.generation && request.cancellation) return await request.cancellation;
            throw error;
        } finally {
            if (operation === request) operation = null;
            if (generation === request.generation) {
                busy = false;
                refresh();
            }
        }
    }

    async function connect() {
        busy = true;
        setStatus(t('spiderConnecting'));
        refresh();
        try {
            if (save.pending) await save.pending.catch(() => {});
            const response = await api('', {});
            token = response.session;
            rootPath = response.root;
            if (source) await api('/file', {path: source.path});
            disconnected = false;
            resetResult();
            reload = !!source;
            await save.flush();
            setStatus(t('spiderReady'));
        } catch (error) {
            if (!token) disconnected = true;
            showError(error);
        } finally {
            busy = false;
            refresh();
        }
    }

    async function load() {
        resultMethod = null;
        if (!el('api').value.trim()) {
            el('api').focus();
            throw new Error(t('spiderSourceRequired'));
        }
        if (el('api').value.trim().endsWith('.java')) throw new Error(t('spiderJavaSource'));
        if (el('api').value.startsWith('csp_') && !el('jar').value.trim()) {
            el('jar').focus();
            throw new Error(t('spiderJarRequired'));
        }
        if (el('api').value.startsWith('/') && source?.path !== el('api').value.trim()) {
            const request = ++generation;
            busy = true;
            refresh();
            try {
                await save.flush();
                if (request !== generation) return;
                const method = el('method').value, args = el('args').value;
                const file = await api('/file', {path: el('api').value.trim()});
                if (request !== generation) return;
                setSource(file);
                el('method').value = method;
                el('args').value = args;
                renderArguments();
            } finally {
                if (request === generation) {
                    busy = false;
                    refresh();
                }
            }
        }
        const value = await work('/load', config());
        if (value?.state === 'loaded') {
            history.remember(config(), el('method').value, el('args').value);
            writeHistory();
            refresh();
        }
        reload = false;
    }

    function readArguments() {
        for (const input of parameterInputs) if (!input.reportValidity()) throw new Error(input.validationMessage || t('spiderArgsInvalid'));
        try {
            const args = JSON.parse(el('args').value);
            if (!Array.isArray(args)) throw new Error();
            return args;
        } catch {
            el('args').focus();
            throw new Error(t('spiderArgsInvalid'));
        }
    }

    async function run() {
        const args = readArguments();
        const entry = cases.list(config()).find(entry => entry.name === caseName && entry.method === el('method').value && !spiderDiff(entry.args, JSON.stringify(args)).changes.length);
        await execute(el('method').value, args, entry?.expected || {}, entry?.name || '');
        const card = document.querySelector('.spider-result-card'), bounds = card.getBoundingClientRect();
        if (bounds.top < 0 || bounds.bottom > window.innerHeight) card.scrollIntoView({behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'start'});
    }

    async function browseChoice(choice) {
        dialog.close();
        el('method').value = choice.method;
        el('args').value = JSON.stringify(choice.args);
        await run();
    }

    async function execute(method, args, expected = {}, name = '') {
        const current = config(), start = performance.now();
        resultMethod = method;
        el('method').value = method;
        el('args').value = JSON.stringify(args);
        renderArguments();
        history.update(current, method, el('args').value);
        writeHistory();
        let value;
        try {
            if (!matches()) {
                await load();
                resultMethod = method;
            }
            if (suite?.running && suite.cancelled) value = {state: 'cancelled', result: '', elapsed: 0};
            else if (!matches()) value = info;
            else value = await work('/run', {method, args});
        } catch (error) {
            value = {state: 'error', result: error.message || String(error), elapsed: Math.round(performance.now() - start)};
            showError(error);
        }
        value ||= {state: 'cancelled', result: '', elapsed: 0};
        if (value.state === 'cancelled') value.elapsed = Math.round(performance.now() - start);
        const validation = value.state === 'done' ? spiderChecks(method, value.result, expected) : {issues: []};
        const issues = validation.issues, version = source?.path === current.api ? save.version : undefined;
        lastRun = {time: new Date().toISOString(), source: current, sourceVersion: version, method, args: JSON.parse(JSON.stringify(args)), expected, name, status: value.state === 'done' ? issues.length ? 'failed' : 'passed' : value.state === 'timedout' ? 'timedout' : value.state === 'cancelled' ? 'cancelled' : 'failed', elapsed: value.elapsed, result: value.result || '', issues, truncated: validation.truncated, requests: value.requests || {records: [], total: 0, truncated: false}};
        records.push(lastRun);
        if (records.length > 32) records.shift();
        if (value.state === 'done') results.record(current, method, args, value.result);
        refresh();
        return lastRun;
    }

    async function testPanel() {
        await save.flush();
        exitFullscreen();
        el('test_panel').hidden = false;
        el('edit_panel').hidden = true;
        el('test_tab').setAttribute('aria-selected', 'true');
        el('edit_tab').setAttribute('aria-selected', 'false');
        if (source?.name.endsWith('.java')) setStatus(t('spiderJavaSource'));
        if (reload && source && !source.name.endsWith('.java') && source.path === el('api').value.trim()) await load();
    }

    function editPanel() {
        if (!source) return;
        el('test_panel').hidden = true;
        el('edit_panel').hidden = false;
        el('test_tab').setAttribute('aria-selected', 'false');
        el('edit_tab').setAttribute('aria-selected', 'true');
        if (!editor) {
            editor = SpiderEditor.create(el('editor'), text => save.update(text));
            editor.set(save.text, source.name.split('.').at(-1), source.writable);
        }
    }

    function resizeEditor() {
        if (!fullscreen) return;
        const viewport = window.visualViewport, height = viewport?.height ?? window.innerHeight, top = viewport?.offsetTop ?? 0;
        const panel = el('edit_panel'), resized = panel.style.getPropertyValue('--spider-editor-height') !== height + 'px';
        panel.style.setProperty('--spider-editor-height', height + 'px');
        panel.style.setProperty('--spider-editor-top', top + 'px');
        if (resized) editor?.revealSelection();
    }

    function setFullscreen(value) {
        if (fullscreen === value) return;
        if (value) editorScroll = window.scrollY;
        fullscreen = value;
        document.body.classList.toggle('spider-editor-expanded', value);
        for (const sibling of document.querySelectorAll('.spider-app > :not(#spider_edit_panel):not(dialog)')) sibling.inert = value;
        const button = el('fullscreen'), label = t(value ? 'spiderExitFullscreen' : 'spiderFullscreen');
        button.setAttribute('aria-pressed', String(value));
        button.setAttribute('aria-label', label);
        button.title = label;
        button.querySelector('use').setAttribute('href', 'icons.svg#' + (value ? 'fullscreen-exit' : 'fullscreen'));
        resizeEditor();
        if (!value) {
            window.scrollTo(0, editorScroll);
            button.focus({preventScroll: true});
        }
    }

    function exitFullscreen() {
        if (!fullscreen) return;
        setFullscreen(false);
        if (window.history.state?.spiderEditorFullscreen) window.history.back();
    }

    function renderSymbols() {
        const list = el('symbols');
        list.replaceChildren();
        const extension = source.name.split('.').at(-1).toLowerCase();
        const actions = {undo: 'spiderUndo', redo: 'spiderRedo', indent: 'spiderIndent', outdent: 'spiderOutdent'};
        for (const symbol of [...Object.keys(actions), ...(SPIDER_SYMBOLS[extension] || SPIDER_SYMBOLS.js)]) appendSymbol(symbol, actions, list);
    }

    function appendSymbol(symbol, actions, list) {
        const button = document.createElement('button'), action = actions[symbol];
        button.type = 'button';
        button.className = 'md-btn spider-symbol' + (action ? ' md-icon-button' : '');
        if (action) button.innerHTML = '<svg class="button-icon" viewBox="0 0 24 24" aria-hidden="true"><use href="icons.svg#' + symbol + '"/></svg>';
        else button.textContent = symbol;
        button.title = t(action || 'spiderInsertSymbol', symbol);
        button.setAttribute('aria-label', button.title);
        button.addEventListener('pointerdown', event => event.preventDefault());
        button.addEventListener('click', () => symbol === 'indent' || symbol === 'outdent' ? editor.indent(symbol === 'outdent') : action ? editor[symbol]() : editor.insert(symbol));
        list.append(button);
    }

    function setSource(file) {
        source = file.text === undefined ? null : file;
        if (source) {
            save.reset(source);
            if (editor) editor.set(source.text, source.name.split('.').at(-1), source.writable);
            el('api').value = file.path;
            el('filename').textContent = file.name;
            el('filename').title = file.name;
            renderSymbols();
            el('save').disabled = !file.writable;
            if (!file.writable) el('save_status').textContent = t('spiderReadOnly');
        } else {
            el('jar').value = file.path;
            if (!el('api').value.startsWith('csp_')) el('api').value = 'csp_';
        }
        if (source) restoreArguments();
        resetResult();
        reload = false;
        setStatus(t('spiderReady'));
        refresh();
    }

    function resetResult() {
        info = {loaded: false};
        resultMethod = null;
        choices = [];
        el('result').textContent = t('spiderEmpty');
        renderFeedback();
    }

    async function selected(file) {
        setSource(file);
        dialog.close();
        if (source?.name.endsWith('.java')) {
            editPanel();
            return;
        }
        await testPanel();
        if (source) await load();
        else await selectCsp();
    }

    async function formatPython(text) {
        const controller = new AbortController(), timer = setTimeout(() => controller.abort(), 60000);
        try {
            return (await api('/format', {text}, 'POST', controller.signal)).text;
        } catch (error) {
            const message = controller.signal.aborted ? t('spiderFormatTimeout') : error.code === 'python_format_unavailable' ? t('spiderFormatModuleUpdate') : t('spiderFormatFailed') + '\n' + error.message;
            throw new Error(message);
        } finally {
            clearTimeout(timer);
        }
    }

    async function formatSource() {
        const file = source, text = save.text;
        formatting = true;
        refresh();
        setStatus(t('spiderFormatting'));
        try {
            const formatted = await (file.name.endsWith('.py') ? formatPython(text) : spiderFormat(text, file.name.split('.').at(-1)));
            if (source !== file || save.text !== text) throw new Error(t('spiderFormatChanged'));
            editor.replace(formatted);
            await save.flush();
            setStatus(t('spiderFormatted'));
        } finally {
            formatting = false;
            refresh();
        }
    }

    async function selectCsp() {
        resultMethod = null;
        const value = await work('/spiders', {jar: el('jar').value.trim()});
        if (!value || value.state !== 'done') return;
        openDialog(t('spiderCsp'));
        for (const name of value.spiders) itemButton(name.substring(4), () => selectCspSource(name));
        if (!value.spiders.length) {
            const empty = document.createElement('p');
            empty.textContent = t('spiderNoCsp');
            el('dialog_body').append(empty);
        }
    }

    async function selectCspSource(name) {
        el('api').value = name;
        restoreArguments();
        dialog.close();
        await load();
    }

    function openDialog(title) {
        dialogMode = '';
        el('dialog_title').textContent = title;
        el('dialog_body').replaceChildren();
        for (const button of el('dialog_actions').querySelectorAll('.spider-dialog-action')) button.remove();
        el('dialog_body').scrollTop = 0;
        el('dialog_close').dataset.i18n = 'close';
        el('dialog_close').textContent = t('close');
        if (!dialog.open) dialog.showModal();
        el('dialog_close').focus({preventScroll: true});
    }

    function itemButton(label, action, parent = el('dialog_body')) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = parent === el('dialog_body') ? 'md-btn md-btn-outlined' : 'md-dialog-btn spider-dialog-action';
        button.textContent = label;
        button.addEventListener('click', () => action().catch(showError));
        if (parent !== el('dialog_body')) {
            el('dialog_close').dataset.i18n = 'cancel';
            el('dialog_close').textContent = t('cancel');
        }
        parent.append(button);
        return button;
    }

    async function restoreRecent(entry) {
        const request = ++generation;
        busy = true;
        refresh();
        try {
            await save.flush();
            if (request !== generation) return;
            const file = entry.api.startsWith('/') ? await api('/file', {path: entry.api}) : null;
            if (request !== generation) return;
            source = null;
            for (const name of ['api', 'jar', 'ext']) el(name).value = entry[name];
            if (file) setSource(file);
            restoreArguments();
            resetResult();
            dialog.close();
        } finally {
            if (request === generation) {
                busy = false;
                refresh();
            }
        }
        await testPanel();
        await load();
    }

    function recent() {
        openDialog(t('spiderRecent'));
        for (const entry of history.entries) appendRecent(entry);
        itemButton(t('spiderClearRecent'), async () => {
            history.entries = [];
            writeHistory();
            dialog.close();
            refresh();
        });
    }

    function appendRecent(entry) {
        const button = itemButton('', () => restoreRecent(entry));
        button.classList.add('spider-recent');
        const name = document.createElement('span'), path = document.createElement('span');
        name.textContent = entry.api.startsWith('csp_') ? entry.api.substring(4) : entry.api.split(/[?#]/, 1)[0].split('/').at(-1);
        path.className = 'spider-file-path';
        path.textContent = entry.jar || entry.api;
        button.append(name, path);
    }

    function compare() {
        const baseline = lastRun && baselines.find(lastRun);
        if (baseline && results.comparison) {
            openDialog(t('spiderCompare'));
            itemButton(t('spiderDiffBefore'), async () => showComparison(spiderDiff(results.comparison.before, results.comparison.after)));
            itemButton(t('spiderBaseline'), async () => showComparison(spiderBaselineDiff(baseline.text, lastRun.result, baseline.ignored)));
        } else showComparison(baseline ? spiderBaselineDiff(baseline.text, lastRun.result, baseline.ignored) : spiderDiff(results.comparison.before, results.comparison.after));
    }

    function showComparison(diff) {
        openDialog(t('spiderCompare'));
        const {changes, truncated} = diff;
        const summary = document.createElement('p');
        summary.textContent = truncated ? t('spiderDiffLimit') : changes.length ? t('spiderDiffCount', String(changes.length)) : t('spiderDiffSame');
        el('dialog_body').append(summary);
        for (const change of changes) appendComparison(change);
    }

    function appendComparison(change) {
        const card = document.createElement('div'), title = document.createElement('p');
        card.className = 'spider-diff';
        title.textContent = t({added: 'spiderDiffAdded', removed: 'spiderDiffRemoved', changed: 'spiderDiffChanged'}[change.kind]) + ' · ' + change.path;
        card.append(title);
        for (const side of ['before', 'after']) appendComparisonSide(card, change, side);
        el('dialog_body').append(card);
    }

    function appendComparisonSide(card, change, side) {
        if (!(side in change)) return;
        const label = document.createElement('p'), value = document.createElement('pre');
        label.className = 'spider-file-path';
        label.textContent = t(side === 'before' ? 'spiderDiffBefore' : 'spiderDiffAfter');
        value.textContent = JSON.stringify(change[side], null, 2);
        card.append(label, value);
    }

    function resultValidation() {
        if (info.state !== 'done' || !resultMethod) return null;
        const expected = lastRun?.method === resultMethod && lastRun.result === info.result && spiderSourceKey(lastRun.source) === spiderSourceKey(config()) ? lastRun.expected : {};
        return spiderChecks(resultMethod, info.result, expected);
    }

    function renderFeedback() {
        const validation = resultValidation();
        el('validation').textContent = validation?.checked ? t(validation.truncated ? 'spiderCheckLimit' : validation.issues.length ? 'spiderCheckIssues' : 'spiderCheckPassed', String(validation.issues.length)) : '';
        el('check').hidden = !validation?.issues.length;
        el('location').hidden = info.state !== 'error' || !source || source.path !== info.api || !spiderErrorLocation(info.result, source.path, rootPath);
        el('playback').hidden = resultMethod !== 'play' || !validation?.checked || spiderValidate(resultMethod, info.result).issues.length > 0 || !matches();
        el('feedback').hidden = !el('validation').textContent && el('location').hidden && el('playback').hidden;
    }

    function showValidation() {
        openDialog(t('spiderCheck'));
        for (const issue of resultValidation().issues) {
            const text = document.createElement('p');
            text.className = 'spider-diff';
            text.textContent = issue.path + ' · ' + t('spiderIssue' + issue.code);
            el('dialog_body').append(text);
        }
    }

    function locateError() {
        const location = source && spiderErrorLocation(info.result, source.path, rootPath);
        if (!location) return;
        dialog.close();
        editPanel();
        editor.goTo(location.line, location.column);
    }

    async function copyText(text) {
        if (navigator.clipboard) return navigator.clipboard.writeText(text);
        const input = document.createElement('textarea'), focused = document.activeElement;
        input.value = text;
        input.style.position = 'fixed';
        input.style.opacity = '0';
        (dialog.open ? el('dialog_body') : document.body).append(input);
        input.select();
        try {
            if (!document.execCommand('copy')) throw new Error(t('spiderCopyFailed'));
        } finally {
            input.remove();
            focused?.focus({preventScroll: true});
        }
    }

    function renderInspection() {
        clearTimeout(searchTimer);
        if (!info.result) return;
        const query = el('result_search').value.trim(), result = spiderSearch(info.result, query), children = [];
        el('search_count').hidden = !query;
        el('search_count').textContent = query ? t(result.truncated ? 'spiderSearchLimit' : result.matches.length ? 'spiderSearchCount' : 'spiderSearchEmpty', String(result.matches.length)) : '';
        if (result.json && !query) {
            children.push(treeItem(result.value, '$', t('spiderResult'), true));
        } else if (result.json) {
            for (const entry of result.matches) children.push(treeItem(entry.value, entry.path, entry.path));
        } else {
            const output = document.createElement('pre');
            output.className = 'spider-step-output';
            output.textContent = query ? result.matches.map(entry => t('spiderLine', String(entry.line)) + ' · ' + entry.value).join('\n\n') : info.result;
            children.push(output);
        }
        el('result_tree').replaceChildren(...children);
    }

    function treeItem(value, path, label, expanded = false) {
        if (value === null || typeof value !== 'object') {
            const button = document.createElement('button'), text = JSON.stringify(value);
            button.type = 'button';
            button.className = 'spider-tree-value';
            button.dataset.path = path;
            button.textContent = label + ': ' + (text.length > 200 ? text.slice(0, 200) + '…' : text);
            button.addEventListener('click', () => {
                openDialog(t('spiderResult'));
                const location = document.createElement('p');
                location.className = 'spider-file-path';
                location.textContent = path;
                el('dialog_body').append(location);
                const output = document.createElement('pre');
                output.className = 'spider-step-output';
                output.textContent = text;
                el('dialog_body').append(output);
                itemButton(t('spiderCopyPath'), async () => {
                    await copyText(path);
                    setStatus(t('spiderPathCopied'));
                });
            });
            return button;
        }
        const details = document.createElement('details'), summary = document.createElement('summary'), body = document.createElement('div'), keys = Object.keys(value);
        details.className = 'spider-tree-branch';
        body.className = 'spider-tree-children';
        summary.textContent = label + ' · ' + t(Array.isArray(value) ? 'spiderArrayCount' : 'spiderObjectCount', String(keys.length));
        details.append(summary, body);
        let index = 0, more;
        const appendChunk = () => {
            more?.remove();
            const end = Math.min(index + 100, keys.length);
            for (; index < end; index++) {
                const key = keys[index], next = (path === '$' ? '' : path) + '/' + key.replace(/~/g, '~0').replace(/\//g, '~1');
                body.append(treeItem(value[key], next, Array.isArray(value) ? '[' + key + ']' : key));
            }
            if (index < keys.length) {
                more = document.createElement('button');
                more.type = 'button';
                more.className = 'md-btn md-btn-outlined';
                more.textContent = t('spiderShowMore');
                more.addEventListener('click', appendChunk);
                body.append(more);
            }
        };
        details.addEventListener('toggle', () => {if (details.open && !body.childElementCount) appendChunk();});
        if (expanded) {
            details.open = true;
            appendChunk();
        }
        return details;
    }

    function showRequests(trace) {
        openDialog(t('spiderRequests'));
        const records = trace.records || [];
        if (!records.length) {
            const text = document.createElement('p');
            text.textContent = t('spiderNoRequests');
            el('dialog_body').append(text);
        }
        if (trace.truncated) {
            const text = document.createElement('p');
            text.className = 'spider-file-path';
            text.textContent = t('spiderRequestsLimit', String(trace.total));
            el('dialog_body').append(text);
        }
        for (const record of records) appendRequest(trace, record);
        const scope = document.createElement('p');
        scope.className = 'spider-file-path';
        scope.textContent = t('spiderRequestsScope');
        el('dialog_body').append(scope);
    }

    function appendRequest(trace, record) {
        const label = record.method + ' · ' + (record.code || t(record.error ? 'spiderFailed' : 'spiderRequestPending')) + ' · ' + record.elapsed + ' ms' + (record.code && !record.complete ? ' · ' + t('spiderRequestPending') : '');
        const button = itemButton('', async () => showRequest(trace, record, label));
        button.classList.add('spider-recent', 'spider-request');
        button.dataset.error = String(!!record.error || record.code >= 400);
        const name = document.createElement('span'), url = document.createElement('span');
        name.textContent = label;
        url.className = 'spider-file-path';
        url.textContent = record.url;
        button.append(name, url);
    }

    function showRequest(trace, record, label) {
        openDialog(t('spiderRequests'));
        for (const value of [label, record.url, record.error, record.urlTruncated ? t('spiderUrlTruncated') : '']) appendRequestDetail(value);
        itemButton(t('spiderCopyUrl'), async () => {
            await copyText(record.url);
            setStatus(t('spiderCopied'));
        });
        itemButton(t('spiderRequests'), async () => showRequests(trace));
    }

    function appendRequestDetail(value) {
        if (!value) return;
        const text = document.createElement('p');
        text.className = 'spider-file-path';
        text.textContent = value;
        el('dialog_body').append(text);
    }

    function listCases() {
        openDialog(t('spiderCases'));
        const entries = cases.list(config());
        if (!entries.length) {
            const text = document.createElement('p');
            text.textContent = t('spiderNoCases');
            el('dialog_body').append(text);
        }
        for (const entry of entries) appendCase(entry);
        itemButton(t('spiderSaveCase'), async () => saveCase());
        if (entries.length) itemButton(t('spiderRunAll'), () => runBatch(entries));
        if (lastBatch) itemButton(t('spiderBatchResults'), async () => {
            openDialog(t('spiderBatchResults'));
            dialogMode = 'suite';
            showSuite(lastBatch);
        });
        if (cases.error) showError(new Error(t('spiderCasesUnavailable')));
    }

    function appendCase(entry) {
        const row = document.createElement('div');
        row.className = 'spider-case-row';
        const button = itemButton(entry.name + ' · ' + entry.method, async () => selectCase(entry));
        const remove = itemButton(t('spiderDeleteCase'), async () => {
            cases.remove(entry);
            if (cases.error) throw cases.error;
            listCases();
        });
        row.append(button, remove);
        el('dialog_body').append(row);
    }

    function selectCase(entry) {
        caseName = entry.name;
        el('method').value = entry.method;
        el('args').value = entry.args;
        renderArguments();
        rememberArguments();
        dialog.close();
    }

    function saveCase() {
        openDialog(t('spiderSaveCase'));
        const field = document.createElement('div'), label = document.createElement('label'), input = document.createElement('input');
        field.className = 'md-field';
        label.textContent = t('spiderCaseName');
        label.htmlFor = input.id = 'spider_case_name';
        input.className = 'md-input';
        input.maxLength = 80;
        input.value = caseName;
        input.autocomplete = 'off';
        field.append(label, input);
        el('dialog_body').append(field);
        const method = document.createElement('p');
        method.className = 'spider-file-path';
        method.textContent = el('method').value + '(' + el('args').value + ')';
        el('dialog_body').append(method);
        const expected = cases.list(config()).find(entry => entry.name === caseName)?.expected || {};
        const details = document.createElement('details'), summary = document.createElement('summary'), form = document.createElement('div');
        summary.textContent = t('spiderExpected');
        form.className = 'spider-form';
        details.append(summary, form);
        el('dialog_body').append(details);
        const minimum = dialogField('minimum', 'spiderMinItems', expected.minItems ?? '', false, form);
        minimum.type = 'number';
        minimum.min = '0';
        minimum.step = '1';
        const required = dialogField('required', 'spiderRequiredFields', (expected.required || []).join('\n'), true, form);
        required.placeholder = '/list/0/vod_id';
        const equals = dialogField('equals', 'spiderExpectedValues', JSON.stringify(expected.equals || {}, null, 2), true, form);
        equals.placeholder = '{"/parse": 0}';
        itemButton(t('spiderSave'), async () => {
            if (!input.value.trim()) {
                input.focus();
                throw new Error(t('spiderCaseNameRequired'));
            }
            validateCaseArguments();
            if (cases.entries.length >= 32 && !cases.list(config()).some(entry => entry.name === input.value.trim())) throw new Error(t('spiderCaseLimit'));
            const conditions = readCaseExpected(minimum, required, equals);
            cases.save(config(), input.value, el('method').value, el('args').value, conditions);
            caseName = input.value.trim();
            if (cases.error) throw new Error(t('spiderCasesUnavailable'));
            listCases();
        }, el('dialog_actions'));
    }

    function validateCaseArguments() {
        try {
            if (!Array.isArray(JSON.parse(el('args').value))) throw new Error();
        } catch {
            throw new Error(t('spiderArgsInvalid'));
        }
    }

    function readCaseExpected(minimum, required, equals) {
        try {
            return spiderExpected({
                ...(minimum.value === '' ? {} : {minItems: Number(minimum.value)}),
                required: required.value.split('\n').map(value => value.trim()).filter(Boolean),
                equals: JSON.parse(equals.value)
            });
        } catch {
            throw new Error(t('spiderExpectedInvalid'));
        }
    }

    function showSuite(steps) {
        if (dialogMode !== 'suite') return;
        el('dialog_body').replaceChildren();
        for (const step of steps) appendSuiteStep(step, steps);
        const failed = steps === lastBatch ? spiderRetryCases(steps, config()) : [];
        if (failed.length) itemButton(t('spiderRetryFailed', String(failed.length)), () => runBatch(failed));
    }

    function appendSuiteStep(step, steps) {
        const button = itemButton((step.name || step.method) + ' · ' + t('spiderStep' + step.state) + (step.elapsed === undefined ? '' : ' · ' + step.elapsed + ' ms'), async () => showSuiteStep(step, steps));
        button.disabled = !!suite?.running || !step.text;
        button.dataset.unavailable = String(!step.text);
    }

    function showSuiteStep(step, steps) {
        openDialog(t('spiderResult'));
        const name = document.createElement('p');
        name.className = 'spider-file-path';
        name.textContent = step.name || step.method;
        el('dialog_body').append(name);
        appendSuiteIssues(step);
        const output = document.createElement('pre');
        output.className = 'spider-step-output';
        output.textContent = step.text;
        el('dialog_body').append(output);
        if (step.requests) itemButton(t('spiderRequests'), async () => showRequests(step.requests));
        const title = t(steps === lastBatch ? 'spiderBatchResults' : 'spiderSuiteResults');
        itemButton(title, async () => {
            openDialog(title);
            dialogMode = 'suite';
            showSuite(steps);
        });
    }

    function appendSuiteIssues(step) {
        if (step.truncated) {
            const text = document.createElement('p');
            text.textContent = t('spiderCheckLimit');
            el('dialog_body').append(text);
        }
        for (const issue of step.issues || []) appendSuiteIssue(issue);
    }

    function appendSuiteIssue(issue) {
        const text = document.createElement('p');
        text.className = 'spider-dialog-error';
        text.textContent = issue.path + ' · ' + t('spiderIssue' + issue.code) + ('expected' in issue ? ' · ' + JSON.stringify(issue.expected) : '');
        el('dialog_body').append(text);
    }

    async function runSuite() {
        const owner = suite = {running: true, cancelled: false};
        openDialog(t('spiderSuite'));
        dialogMode = 'suite';
        refresh();
        try {
            lastSuite = await spiderSuite(async (method, args) => {
                const run = await execute(method, args);
                return owner.cancelled ? null : {state: run.status === 'passed' ? 'done' : run.status, result: run.result, elapsed: run.elapsed};
            }, showSuite, () => suite === owner && !owner.cancelled && !disconnected);
        } finally {
            owner.running = false;
            if (lastSuite) showSuite(lastSuite);
            refresh();
        }
    }

    async function runBatch(entries) {
        const owner = suite = {running: true, cancelled: false};
        openDialog(t('spiderBatchResults'));
        dialogMode = 'suite';
        refresh();
        try {
            lastBatch = await spiderBatch(entries, execute, showSuite, () => suite === owner && !owner.cancelled && !disconnected);
        } finally {
            owner.running = false;
            if (lastBatch) showSuite(lastBatch);
            refresh();
        }
    }

    function dialogField(name, key, value, multiline = false, parent = el('dialog_body')) {
        const field = document.createElement('div'), label = document.createElement('label'), input = document.createElement(multiline ? 'textarea' : 'input');
        field.className = 'md-field';
        label.textContent = t(key);
        label.htmlFor = input.id = 'spider_' + name;
        input.className = 'md-input';
        input.value = value;
        input.autocomplete = 'off';
        input.spellcheck = false;
        if (multiline) input.rows = 3;
        field.append(label, input);
        parent.append(field);
        return input;
    }

    function baseline() {
        openDialog(t('spiderBaseline'));
        const entry = baselines.find(lastRun), status = document.createElement('p');
        status.className = 'spider-file-path';
        status.textContent = entry ? new Date(entry.time).toLocaleString() : t('spiderNoBaseline');
        el('dialog_body').append(status);
        const ignored = dialogField('ignored', 'spiderIgnoreFields', (entry?.ignored || []).join('\n'), true);
        ignored.placeholder = '/timestamp';
        const paths = () => {
            const values = ignored.value.split('\n').map(value => value.trim()).filter(Boolean);
            if (!values.every(spiderPointer)) throw new Error(t('spiderExpectedInvalid'));
            return values;
        };
        const compare = itemButton(t('spiderCompare'), async () => {
            entry.ignored = paths();
            baselines.write();
            if (baselines.error) throw new Error(t('spiderBaselineUnavailable'));
            showComparison(spiderBaselineDiff(entry.text, lastRun.result, entry.ignored));
        });
        compare.disabled = !entry;
        compare.dataset.unavailable = String(!entry);
        itemButton(t('spiderSetBaseline'), async () => {
            if (!entry && baselines.entries.length >= 16) throw new Error(t('spiderBaselineLimit'));
            baselines.save(lastRun, paths());
            if (baselines.error) throw new Error(t('spiderBaselineUnavailable'));
            baseline();
            refresh();
        }, el('dialog_actions'));
        if (entry) itemButton(t('spiderRemoveBaseline'), async () => {
            baselines.remove(entry);
            if (baselines.error) throw new Error(t('spiderBaselineUnavailable'));
            baseline();
            refresh();
        });
        if (baselines.error) showError(new Error(t('spiderBaselineUnavailable')));
    }

    function report(archive = records) {
        openDialog(t('spiderReport'));
        const text = document.createElement('p');
        text.className = 'spider-file-path';
        text.textContent = t('spiderRecordCount', String(archive.length));
        el('dialog_body').append(text);
        for (const format of ['json', 'html']) itemButton(format.toUpperCase(), async () => downloadReport(archive, format));
        if (archive.length > 1) itemButton(t('spiderReportLatest'), async () => report([lastRun]));
    }

    function downloadReport(archive, format) {
        const blob = new Blob([spiderReport(archive, format, t('spiderReport'))], {type: (format === 'json' ? 'application/json' : 'text/html') + ';charset=utf-8'});
        if (blob.size > (new URL(location.href).searchParams.has('app') ? 512 * 1024 : 8 * 1024 * 1024)) throw new Error(t('spiderReportTooLarge'));
        const link = document.createElement('a'), url = URL.createObjectURL(blob);
        link.href = url;
        link.download = 'spider-report-' + new Date().toISOString().replace(/[:.]/g, '-') + '.' + format;
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 5000);
    }

    function timeoutSetting() {
        openDialog(t('spiderTimeout'));
        const input = dialogField('timeout', 'spiderTimeoutSeconds', timeout);
        input.type = 'number';
        input.min = '1';
        input.max = '300';
        input.step = '1';
        itemButton(t('spiderSave'), async () => {
            const value = Number(input.value);
            if (!Number.isInteger(value) || value < 1 || value > 300) throw new Error(t('spiderTimeoutInvalid'));
            history.storage.setItem('tv.spider.timeout.v1', JSON.stringify(value));
            timeout = value;
            dialog.close();
        }, el('dialog_actions'));
    }

    async function versions() {
        const current = source;
        await save.flush();
        if (current !== source) return;
        const value = await api('/versions');
        if (current !== source) return;
        openDialog(t('spiderVersions'));
        if (!value.versions.length) {
            const text = document.createElement('p');
            text.textContent = t('spiderNoVersions');
            el('dialog_body').append(text);
        }
        for (const version of value.versions) itemButton(new Date(version.time).toLocaleString() + ' · ' + Math.ceil(version.size / 1024) + ' KB' + (version.current ? ' · ' + t('spiderVersionCurrent') : ''), () => showVersion(current, version));
        const checkpoint = itemButton(t('spiderCheckpoint'), async () => {
            await api('/checkpoint', {});
            await versions();
        }, el('dialog_actions'));
        checkpoint.disabled = !source.writable;
        checkpoint.dataset.unavailable = String(checkpoint.disabled);
    }

    async function showVersion(current, version) {
        const previous = await api('/version', {id: version.id});
        if (current !== source) return;
        openDialog(t('spiderVersions'));
        const text = document.createElement('pre');
        text.className = 'spider-step-output';
        text.textContent = previous.text;
        el('dialog_body').append(text);
        const restore = itemButton(t('spiderRestoreVersion'), () => restoreVersion(current, version), el('dialog_actions'));
        restore.disabled = !source.writable || version.current;
        restore.dataset.unavailable = String(restore.disabled);
        itemButton(t('spiderVersions'), versions);
    }

    async function restoreVersion(current, version) {
        await save.flush();
        if (current !== source) return;
        const restored = await api('/restore', {path: current.path, version: save.version, id: version.id});
        if (current !== source) return;
        setSource(restored);
        reload = true;
        dialog.close();
        editPanel();
    }

    function more() {
        openDialog(t('spiderMore'));
        itemButton(t('spiderCases'), async () => listCases());
        const saveButton = itemButton(t('spiderSaveCase'), async () => saveCase());
        saveButton.dataset.unavailable = String(saveButton.disabled = !el('api').value.trim());
        const runButton = itemButton(t('spiderSuite'), runSuite);
        runButton.dataset.unavailable = String(runButton.disabled = !matches());
        if (lastSuite) itemButton(t('spiderSuiteResults'), async () => {
            openDialog(t('spiderSuite'));
            dialogMode = 'suite';
            showSuite(lastSuite);
        });
        if (lastRun && info.state === 'done' && matches()) itemButton(t('spiderBaseline'), async () => baseline());
        if (records.length) itemButton(t('spiderReport'), async () => report());
        if (['done', 'error', 'timedout', 'loaded', 'cancelled'].includes(info.state)) itemButton(t('spiderRequests'), async () => showRequests(info.requests || {}));
        itemButton(t('spiderTimeout'), async () => timeoutSetting());
        const logs = document.createElement('a');
        logs.className = 'md-btn md-btn-outlined';
        logs.textContent = t('logTitle');
        logs.href = '/logs.html';
        logs.target = new URL(location.href).searchParams.has('app') ? '_self' : '_blank';
        logs.rel = 'noopener';
        el('dialog_body').append(logs);
    }

    async function files(path = rootPath) {
        await save.flush();
        const listing = await api('/files?path=' + encodeURIComponent(path));
        openDialog(t('spiderFiles'));
        const label = document.createElement('p');
        label.className = 'spider-file-path';
        label.textContent = listing.path;
        el('dialog_body').append(label);
        if (listing.parent) itemButton('..', () => files(listing.parent));
        for (const entry of listing.entries) itemButton((entry.directory ? '▸ ' : '') + entry.name, () => entry.directory ? files(entry.path) : api('/file', {path: entry.path}).then(selected));
        if (!listing.entries.length) {
            const empty = document.createElement('p');
            empty.textContent = t('spiderNoFiles');
            el('dialog_body').append(empty);
        }
    }

    function reopen() {
        openDialog(t('spiderReopen'));
        const message = document.createElement('p');
        message.textContent = t('spiderDiscardDraft');
        el('dialog_body').append(message);
        itemButton(t('confirm'), async () => {
            clearTimeout(save.timer);
            if (save.pending) await save.pending.catch(() => {});
            setSource(await api('/file', {path: source.path}));
            reload = true;
            dialog.close();
            editPanel();
            el('edit_tab').focus({preventScroll: true});
        }, el('dialog_actions'));
    }

    async function back() {
        if (closeChoice()) return;
        if (dialog.open) {
            dialog.close();
            return;
        }
        if (fullscreen) {
            exitFullscreen();
            return;
        }
        if (!el('edit_panel').hidden) {
            await testPanel();
            el('edit_tab').focus();
            return;
        }
        await leavePage();
    }

    async function leavePage() {
        await save.flush();
        await api('', undefined, 'DELETE').catch(error => console.warn('Crawler session cleanup:', error));
        if (new URL(location.href).searchParams.has('app')) location.href = 'tv-spider://close';
        else returnWebPage('settings');
    }

    async function formatArguments() {
        el('args').value = JSON.stringify(readArguments(), null, 2);
        el('args').rows = Math.min(12, Math.max(3, el('args').value.split('\n').length));
        renderArguments();
        rememberArguments();
    }

    async function cancel() {
        if (cancelling) return;
        cancelling = true;
        if (suite?.running) suite.cancelled = true;
        const stopped = generation, request = ++generation;
        refresh();
        const pending = api('/cancel', {});
        const current = operation?.generation === stopped ? operation : null;
        if (current) current.cancellation = pending;
        try {
            const value = await pending;
            if (request === generation) render(value);
        } catch (error) {
            current?.controller.abort();
            if (request === generation) {
                disconnected = true;
                showError(error);
            }
        } finally {
            cancelling = false;
            if (request === generation) busy = false;
            refresh();
        }
    }

    async function uploadSource() {
        const file = el('uploader').files[0];
        if (!file) return;
        try {
            if (!/\.(py|js|java|jar)$/.test(file.name)) throw new Error(t('spiderFileFormat'));
            await save.flush();
            const body = new FormData();
            body.append('source', file);
            await selected(await api('/upload', body));
        } catch (error) {
            showError(error);
        } finally {
            el('uploader').value = '';
        }
    }

    window.spiderBack = () => back().catch(showError);
    const action = (name, call) => el(name).addEventListener('click', () => call().catch(showError));
    bindChoice(el('method'), () => t('spiderMethod'), () => SPIDER_METHODS.map(([value]) => ({value, label: value})));
    el('method').addEventListener('change', () => {
        el('args').value = history.find(config())?.parameters[el('method').value] ?? SPIDER_METHODS.find(method => method[0] === el('method').value)[1];
        renderArguments();
        rememberArguments();
    });
    el('args').addEventListener('input', () => {
        renderArguments();
        rememberArguments();
    });
    for (const name of ['api', 'ext', 'jar']) el(name).addEventListener('input', refresh);
    action('load', load);
    action('run', run);
    action('test_tab', testPanel);
    action('files', () => files());
    action('back', leavePage);
    action('save', async () => {
        await save.flush();
        await api('/checkpoint', {});
    });
    action('reconnect', connect);
    action('csp', selectCsp);
    action('versions', versions);
    action('format', formatSource);
    el('fullscreen').addEventListener('click', () => {
        if (fullscreen) {
            exitFullscreen();
        } else {
            window.history.pushState({...window.history.state, spiderEditorFullscreen: true}, '');
            setFullscreen(true);
        }
    });
    window.addEventListener('popstate', () => setFullscreen(!!window.history.state?.spiderEditorFullscreen && !el('edit_panel').hidden));
    window.addEventListener('resize', resizeEditor);
    window.visualViewport?.addEventListener('resize', resizeEditor);
    window.visualViewport?.addEventListener('scroll', resizeEditor);
    document.addEventListener('keydown', event => {
        if (event.key !== 'Escape' || event.defaultPrevented || !fullscreen || document.querySelector('dialog[open]')) return;
        event.preventDefault();
        exitFullscreen();
    });
    action('format_args', formatArguments);
    action('retest', async () => {
        await testPanel();
        await run();
    });
    el('edit_tab').addEventListener('click', editPanel);
    el('recent').addEventListener('click', recent);
    el('compare').addEventListener('click', compare);
    el('more').addEventListener('click', more);
    el('check').addEventListener('click', showValidation);
    el('location').addEventListener('click', locateError);
    action('playback', async () => {
        await api('/play', {result: info.result});
        setStatus(t('spiderPlaybackSent'));
    });
    el('reopen').addEventListener('click', reopen);
    el('cancel').addEventListener('click', cancel);
    el('suite_cancel').addEventListener('click', cancel);
    el('upload').addEventListener('click', () => el('uploader').click());
    el('uploader').addEventListener('change', uploadSource);
    el('help').addEventListener('click', () => {
        openDialog(t('spiderArgs'));
        const text = document.createElement('p');
        text.textContent = SPIDER_METHODS.find(method => method[0] === el('method').value)[2];
        el('dialog_body').append(text);
    });
    el('items').addEventListener('click', () => {
        openDialog(t('spiderBrowse'));
        for (const choice of choices) itemButton(choice.label, () => browseChoice(choice));
    });
    action('copy', async () => {
        await copyText(info.result);
        setStatus(t('spiderCopied'));
    });
    el('result_search').addEventListener('input', () => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(renderInspection, 150);
    });
    el('dialog_close').addEventListener('click', () => dialog.close());
    window.addEventListener('beforeunload', event => {
        if (save.dirty) {
            event.preventDefault();
            event.returnValue = '';
        }
    });
    window.addEventListener('pagehide', () => {
        writeHistory();
        if (token && !save.dirty) fetch('/spider-api', {method: 'DELETE', keepalive: true, headers: {'X-Spider-Session': token}}).catch(() => {});
    });
    el('editor').dataset.label = t('spiderEdit');
    el('help').setAttribute('aria-label', t('spiderArgsHelp'));
    document.querySelector('.spider-tabs').setAttribute('aria-label', t('spiderTitle'));
    document.querySelector('.spider-controls').setAttribute('aria-label', t('spiderSource'));
    if (history.entries.length) {
        for (const name of ['api', 'jar', 'ext']) el(name).value = history.entries[0][name];
        restoreArguments();
    }
    renderArguments();
    refresh();
    await connect();
    if (history.error) setStatus(t('spiderHistoryUnavailable'), true);
}

if (typeof module !== 'undefined') module.exports = {SpiderAutoSave, spiderChoices, SpiderHistory, SpiderResults, spiderDiff, SpiderCases, spiderErrorLocation, spiderPlaybackUrls, spiderValidate, spiderSuite, spiderPointer, spiderField, spiderExpected, spiderAssert, spiderChecks, SpiderBaselines, spiderBaselineDiff, spiderBatch, spiderReport, spiderRetryCases, spiderSearch, spiderParameters, spiderParameterValue, spiderFormat};
if (typeof document !== 'undefined') document.addEventListener('DOMContentLoaded', initSpider);
