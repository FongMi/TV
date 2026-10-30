import host from 'catvod:host';
import {net, local, js, proxyUrl, link, page} from './net.js';
import {dialog} from './dialog.js';

export {net, local, js, dialog};
export {proxyUrl, danmakuUrl, getPort, openWeb, s2t, t2s, joinUrl, htmlQuery, md5X, sha256X, randomX, aesX, desX, rsaX, link, page} from './net.js';

function bindMethods(spider) {
    const names = new Set(['constructor', ...Object.getOwnPropertyNames(spider)]);
    for (let prototype = Object.getPrototypeOf(spider); prototype !== Object.prototype; prototype = Object.getPrototypeOf(prototype)) {
        for (const name of Object.getOwnPropertyNames(prototype)) {
            if (names.has(name)) continue;
            names.add(name);
            const method = Object.getOwnPropertyDescriptor(prototype, name).value;
            if (typeof method === 'function') Object.defineProperty(spider, name, {value: method.bind(spider), writable: true, configurable: true});
        }
    }
}

export class Spider {
    constructor() {
        this.siteKey = host.siteKey;
        this.net = net;
        this.local = local;
        this.js = js;
        this.dialog = dialog;
        bindMethods(this);
    }

    init(extend) {
    }

    homeContent(filter) {
        return {class: []};
    }

    homeVideoContent() {
        return {list: []};
    }

    categoryContent(tid, pg, filter, extend) {
        return {list: []};
    }

    detailContent(ids) {
        return {list: []};
    }

    searchContent(key, quick, pg = '1') {
        return {list: []};
    }

    playerContent(flag, id, vipFlags) {
        return {};
    }

    liveContent(url) {
        return '';
    }

    manualVideoCheck() {
        return false;
    }

    isVideoFormat(url) {
        return false;
    }

    proxy(params) {
        return [404, 'text/plain', ''];
    }

    action(action) {
        return {};
    }

    danmaku(id, emit) {
        throw new Error('Spider does not support live danmaku');
    }

    destroy() {
    }

    proxyUrl(params = {}, local = true) {
        return proxyUrl(params, local);
    }

    danmakuUrl(id) {
        return host.danmakuUrl(id);
    }

    link(name, target) {
        return link(name, target);
    }

    page(items, pg, limit, total) {
        return page(items, pg, limit, total);
    }

    log(message) {
        console.log(message);
    }
}
