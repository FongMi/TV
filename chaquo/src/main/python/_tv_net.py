import json
from threading import Lock


_loader_type = None
_loader_lock = Lock()
_listener_type = None
_listener_lock = Lock()
_mapper_type = None
_mapper_lock = Lock()


def _mapper_class():
    global _mapper_type
    with _mapper_lock:
        if _mapper_type is None:
            from java import dynamic_proxy, jclass

            class Mapper(dynamic_proxy(jclass("com.github.catvod.net.Net$Mapper"))):
                def __init__(self, items, mapper, results):
                    super().__init__()
                    self._items = items
                    self._mapper = mapper
                    self._results = results

                def apply(self, item, index):
                    self._results[index] = self._mapper(self._items[index], index)
                    return None

            _mapper_type = Mapper
        return _mapper_type


class JsAdapter:
    def __init__(self, native):
        self._native = native

    def eval(self, source, options=None):
        if not isinstance(source, str) or options is not None and not isinstance(options, dict):
            raise TypeError("Script requires source and an options dict")
        if self._native is None:
            raise RuntimeError("Script bridge is unavailable")
        settings = json.dumps({} if options is None else options, ensure_ascii=False, allow_nan=False)
        return json.loads(self._native.eval(source, settings))


def _invalid_constant(value):
    raise ValueError("Invalid JSON constant: " + value)


def _milliseconds(value, name):
    if isinstance(value, bool) or not isinstance(value, int):
        raise TypeError("%s requires integer milliseconds" % name)
    if value < 0 or value > 9223372036854775807:
        raise ValueError("%s milliseconds must be between 0 and Long.MAX_VALUE" % name)
    return value


def _cache_options(options, name):
    _milliseconds(options.get("ttl"), name)
    for flag in ("stale", "persist"):
        if flag in options and not isinstance(options[flag], bool):
            raise TypeError("%s %s requires a boolean" % (name, flag))


def _loader_class():
    global _loader_type
    with _loader_lock:
        if _loader_type is None:
            from java import dynamic_proxy
            from java.util.concurrent import Callable

            class Loader(dynamic_proxy(Callable)):
                def __init__(self, loader):
                    super().__init__()
                    self._loader = loader

                def call(self):
                    return json.dumps(self._loader(), ensure_ascii=False, separators=(",", ":"), allow_nan=False)

            _loader_type = Loader
        return _loader_type


def _listener_class():
    global _listener_type
    with _listener_lock:
        if _listener_type is None:
            from java import dynamic_proxy, jclass

            class Listener(dynamic_proxy(jclass("com.github.catvod.net.NetSocket$Listener"))):
                def __init__(self, callback):
                    super().__init__()
                    self._callback = callback

                def accept(self, socket, event):
                    metadata, content = event.parts()
                    value = json.loads(metadata)
                    if value["type"] == "message":
                        value["content"] = bytes(content) if value["binary"] else content
                    self._callback(SocketAdapter(socket), value)

            _listener_type = Listener
        return _listener_type


class SocketAdapter:
    def __init__(self, native):
        self._native = native

    def send(self, data):
        if not isinstance(data, (str, bytes, bytearray)):
            raise TypeError("send requires text or bytes")
        return self._native.send(data)

    def close(self):
        self._native.close()

    def cancel(self):
        self._native.cancel()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self.close()


class StreamAdapter:
    def __init__(self, native):
        self._native = native
        metadata = json.loads(native.metadata())
        self.code = metadata["code"]
        self.headers = metadata["headers"]
        self.contentType = metadata["contentType"]

    def read(self, size=65536):
        if isinstance(size, bool) or not isinstance(size, int) or not 1 <= size <= 1048576:
            raise ValueError("Chunk size must be 1..1048576 bytes")
        chunk = self._native.readChunk(size)
        return None if chunk is None else bytes(chunk)

    def close(self):
        self._native.close()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self.close()


class _StreamBody:
    def __init__(self, source):
        self._iterator = iter(source)
        self._chunk = None

    def pull(self, size):
        if self._chunk is None or not self._chunk:
            try:
                value = next(self._iterator)
            except StopIteration:
                return None
            if not isinstance(value, (bytes, bytearray, memoryview)):
                raise TypeError("Stream body must yield bytes")
            self._chunk = memoryview(value).cast("B")
        result = self._chunk[:size].tobytes()
        self._chunk = self._chunk[size:]
        return result

    def close(self):
        self._chunk = None
        close = getattr(self._iterator, "close", None)
        if close is not None:
            close()


class NetAdapter:
    def __init__(self, native, user_agent=None, streams=None):
        self._native = native
        self._user_agent = user_agent
        self._streams = streams

    def session(self):
        return NetAdapter(self._native.session(), self._user_agent, self._streams)

    def openStream(self, url, options=None):
        return StreamAdapter(self._open_stream(url, options))

    def stream(self, source, options=None):
        if isinstance(source, str):
            body = self._open_stream(source, options)
            result = json.loads(body.metadata())
        else:
            if options is not None and (not isinstance(options, dict) or options):
                raise TypeError("Stream body does not accept request options")
            if isinstance(source, (bytes, bytearray, memoryview, dict)):
                raise TypeError("Stream body requires a byte iterable")
            if self._streams is None:
                raise RuntimeError("Stream bridge is unavailable")
            body = self._streams.body(_StreamBody(source))
            result = {"code": 200, "headers": {}, "contentType": "application/octet-stream"}
        result["stream"] = body
        return result

    def _open_stream(self, url, options):
        if not isinstance(url, str) or options is not None and not isinstance(options, dict):
            raise TypeError("Stream requires a URL and options dict")
        if self._streams is None:
            raise RuntimeError("Stream bridge is unavailable")
        settings = json.dumps({} if options is None else options, ensure_ascii=False, allow_nan=False)
        return self._streams.open(self._native, url, settings)

    def getUserAgent(self):
        return "" if self._user_agent is None else self._user_agent.getUserAgent()

    def sleep(self, ms):
        self._native.sleep(_milliseconds(ms, "sleep"))

    def cached(self, key, options, loader):
        return json.loads(self.cachedText(key, options, loader))

    def cachedText(self, key, options, loader):
        if not isinstance(key, str) or not key:
            raise ValueError("cached requires a nonempty string key")
        if not isinstance(options, dict):
            raise TypeError("cached requires an options dict")
        _cache_options(options, "cached")
        if not callable(loader):
            raise TypeError("cached requires a callable loader")
        options = json.dumps(options)
        value = self._native.cachedValue(key, options)
        if value is None:
            value = self._native.cached(key, options, _loader_class()(loader))
        return value

    def clearCache(self, key=None):
        self._native.clearCache(key)

    def refresh(self, key, options, loader):
        if not isinstance(key, str) or not key or not isinstance(options, dict) or not callable(loader):
            raise ValueError("refresh requires a key, options and callable loader")
        _cache_options(options, "refresh")
        self._native.refresh(key, json.dumps(options), _loader_class()(loader))

    def peek(self, key):
        value = self._native.peek(key)
        return None if value is None else json.loads(value)

    def json(self, url, options=None):
        options = {} if options is None else options
        text = self._native.jsonText(url, json.dumps(options, ensure_ascii=False, allow_nan=False))
        return json.loads(text, parse_constant=_invalid_constant)

    def getCookie(self, url):
        return self._native.getCookie(url)

    def setCookie(self, url, cookie):
        return self._native.setCookie(url, cookie)

    def close(self):
        self._native.close()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self.close()

    def req(self, url, options=None):
        return self._request(self._native.reqParts, url, options)

    def batch(self, requests, limit=4):
        if (not isinstance(requests, list) or isinstance(limit, bool)
                or not isinstance(limit, int) or limit < 1):
            raise ValueError("batch requires a request list and positive limit")
        for item in requests:
            if (not isinstance(item, dict) or not isinstance(item.get("url"), str)
                    or item.get("options") is not None and not isinstance(item["options"], dict)):
                raise ValueError("Invalid batch request")
        values = self._native.batchParts(json.dumps(requests, ensure_ascii=False, allow_nan=False), limit)
        return [self._parts(value, item.get("options") or {}) for value, item in zip(values, requests)]

    def map(self, items, mapper, limit=4):
        if not isinstance(items, list) or not callable(mapper) or type(limit) is not int or not 1 <= limit <= 2147483647:
            raise ValueError("map requires a list, callable mapper and positive limit")
        from java.util import Collections

        items = list(items)
        results = [None] * len(items)
        self._native.map(Collections.nCopies(len(items), None), _mapper_class()(items, mapper, results), limit)
        return results

    def download(self, url, path, options=None):
        from java.io import File
        settings = json.dumps({} if options is None else options, ensure_ascii=False, allow_nan=False)
        self._native.download(url, settings, File(str(path)))

    def ws(self, url, options=None):
        return self._request(self._native.wsParts, url, options)

    def connect(self, url, options, callback):
        if not callable(callback):
            raise TypeError("connect requires a callable listener")
        options = {} if options is None else options
        settings = json.dumps(options, ensure_ascii=False, separators=(",", ":"), allow_nan=False)
        return SocketAdapter(self._native.connect(url, settings, _listener_class()(callback)))

    @staticmethod
    def _request(method, url, options):
        options = {} if options is None else options
        settings = json.dumps(options, ensure_ascii=False, separators=(",", ":"))
        return NetAdapter._parts(method(url, settings), options)

    @staticmethod
    def _parts(parts, options):
        metadata, content = parts
        result = json.loads(metadata)
        if "error" not in result:
            if options.get("buffer") == 3:
                content = bytes(content)
            elif not isinstance(content, str):
                content = list(content)
        result["content"] = content
        return result


class LocalAdapter:
    def __init__(self, native):
        self._native = native

    def get(self, key, default=None):
        value = self._native.get(key)
        return default if value is None else json.loads(value)

    def set(self, key, value):
        value = json.dumps(value, ensure_ascii=False, separators=(",", ":"), allow_nan=False)
        self._native.set(key, value)

    def delete(self, key):
        self._native.delete(key)
