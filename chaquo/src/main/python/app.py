import json
import os
from urllib.parse import urlsplit
from urllib.request import url2pathname

from _tv_loader import Runtime, SourceNotFoundError
from _tv_net import JsAdapter, LocalAdapter, NetAdapter


def spider(cache, api, site_key="", net=None):
    return Runtime(cache, api, site_key, lambda url: readSource(url, net, inline=url == api)).create()


def readSource(api, net=None, inline=True):
    parsed = urlsplit(api)
    if parsed.scheme in ('http', 'https'):
        return readRemote(api, net)
    if parsed.scheme == 'file':
        api = url2pathname(parsed.path)
        if parsed.netloc:
            api = '//%s%s' % (parsed.netloc, api)
    if not inline or parsed.scheme == 'file' or os.path.isfile(api):
        try:
            with open(api, 'rb') as stream:
                return stream.read()
        except FileNotFoundError as error:
            raise SourceNotFoundError(error.errno, error.strerror, error.filename) from error
    return api.encode()


def readRemote(api, net):
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


def str2json(content):
    return json.loads(content)


def json2str(content):
    return json.dumps(content, ensure_ascii=False, allow_nan=False)


def init(ru, extend):
    try:
        runtime = getattr(ru, '_tv_runtime', None)
        if runtime is not None:
            runtime.prepare(ru)
        ru.init(extend)
    except BaseException:
        release(ru)
        raise


def bindNet(ru, net, user_agent=None, local=None, streams=None, js=None):
    ru.net = NetAdapter(net, user_agent, streams)
    ru.local = LocalAdapter(local)
    ru.js = JsAdapter(js)


def homeContent(ru, filter):
    return json2str(ru.homeContent(filter))


def homeVideoContent(ru):
    return json2str(ru.homeVideoContent())


def categoryContent(ru, tid, pg, filter, extend):
    return json2str(ru.categoryContent(tid, pg, filter, str2json(extend)))


def detailContent(ru, array):
    return json2str(ru.detailContent(str2json(array)))


def searchContent(ru, key, quick, pg="1"):
    return json2str(ru.searchContent(key, quick, pg))


def playerContent(ru, flag, id, vipFlags):
    return json2str(ru.playerContent(flag, id, str2json(vipFlags)))


def isVideoFormat(ru, url):
    return bool(ru.isVideoFormat(url))


def manualVideoCheck(ru):
    return bool(ru.manualVideoCheck())


def liveContent(ru, url):
    value = ru.liveContent(url)
    return value if isinstance(value, str) else json2str(value)


def localProxy(ru, param):
    return ru.localProxy(str2json(param))


def action(ru, action):
    return json2str(ru.action(action))


def danmaku(ru, id, emitter):
    stop = ru.danmaku(id, lambda item: emitter.emit(json.dumps(item, ensure_ascii=False, allow_nan=False)))
    if not callable(stop):
        raise TypeError("danmaku must return a stop function")
    return stop


def destroy(ru):
    try:
        ru.destroy()
    finally:
        release(ru)


def release(ru):
    runtime = getattr(ru, '_tv_runtime', None)
    if runtime is not None:
        runtime.close()
