import json
import os
from inspect import isawaitable, iscoroutine
from urllib.parse import urlsplit
from urllib.request import url2pathname

from runtime.dialog import DialogAdapter
from runtime.loader import Runtime, SourceNotFoundError
from runtime.net import JsAdapter, LocalAdapter, NetAdapter


def spider(cache, api, site_key="", net=None):
    return Runtime(cache, api, site_key, lambda url: read_source(url, net, inline=url == api)).create()


def read_source(api, net=None, inline=True):
    parsed = urlsplit(api)
    if parsed.scheme in ("http", "https"):
        return read_remote(api, net)
    if parsed.scheme == "file":
        api = url2pathname(parsed.path)
        if parsed.netloc:
            api = f"//{parsed.netloc}{api}"
    if not inline or parsed.scheme == "file" or os.path.isfile(api):
        try:
            with open(api, "rb") as stream:
                return stream.read()
        except FileNotFoundError as error:
            raise SourceNotFoundError(error.errno, error.strerror, error.filename) from error
    return api.encode()


def read_remote(api, net):
    if net is None:
        raise RuntimeError("Remote Python source requires a network owner")
    response = NetAdapter(net).req(api, {"timeout": 10000, "callTimeout": 10000, "buffer": 3})
    if response.get("error"):
        raise RuntimeError(response["error"])
    if response["code"] == 404:
        raise SourceNotFoundError("HTTP 404")
    if not 200 <= response["code"] < 300:
        raise RuntimeError("HTTP " + str(response["code"]))
    return response["content"]


def encode_json(content):
    content = sync_result(content)
    if not isinstance(content, (dict, list)):
        raise TypeError("Spider data must be a dict or list")
    return json.dumps(content, ensure_ascii=False, separators=(",", ":"), allow_nan=False)


def sync_result(value):
    if isawaitable(value):
        if iscoroutine(value):
            value.close()
        raise TypeError("Python spider methods must be synchronous")
    return value


def boolean_result(value):
    value = sync_result(value)
    if not isinstance(value, bool):
        raise TypeError("Spider video checks must return bool")
    return value


def init(ru, extend):
    try:
        ru.runtime.prepare(ru)
        sync_result(ru.init(extend))
    except BaseException:
        release(ru)
        raise


def bindNet(ru, net, user_agent=None, local=None, streams=None, js=None):
    ru.net = NetAdapter(net, user_agent, streams)
    ru.local = LocalAdapter(local)
    ru.js = JsAdapter(js)
    ru.dialog = DialogAdapter()


def homeContent(ru, filter):
    return encode_json(ru.homeContent(filter))


def homeVideoContent(ru):
    return encode_json(ru.homeVideoContent())


def categoryContent(ru, tid, pg, filter, extend):
    return encode_json(ru.categoryContent(tid, pg, filter, json.loads(extend)))


def detailContent(ru, array):
    return encode_json(ru.detailContent(json.loads(array)))


def searchContent(ru, key, quick, pg="1"):
    return encode_json(ru.searchContent(key, quick, pg))


def playerContent(ru, flag, id, vipFlags):
    return encode_json(ru.playerContent(flag, id, json.loads(vipFlags)))


def isVideoFormat(ru, url):
    return boolean_result(ru.isVideoFormat(url))


def manualVideoCheck(ru):
    return boolean_result(ru.manualVideoCheck())


def liveContent(ru, url):
    value = ru.liveContent(url)
    return value if isinstance(value, str) else encode_json(value)


def proxy(ru, params):
    value = sync_result(ru.proxy(json.loads(params)))
    if isinstance(value, (list, tuple)) and len(value) > 2 and isinstance(value[2], (bytes, bytearray)):
        from java import jarray, jbyte

        value = list(value)
        value[2] = jarray(jbyte)(value[2])
    return value


def action(ru, action):
    return encode_json(ru.action(action))


def danmaku(ru, id, emitter):
    stop = sync_result(ru.danmaku(id, lambda item: emitter.emit(encode_json(item))))
    if not callable(stop):
        raise TypeError("danmaku must return a stop function")
    return lambda: sync_result(stop())


def destroy(ru):
    try:
        close_dialog(ru)
        sync_result(ru.destroy())
    finally:
        release(ru)


def release(ru):
    close_dialog(ru)
    ru.runtime.close()


def close_dialog(ru):
    dialog = getattr(ru, "dialog", None)
    if dialog is not None:
        dialog.close()
