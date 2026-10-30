import json
from threading import Lock


def _callback(callback):
    from java import dynamic_proxy, jclass

    json_object = jclass("org.json.JSONObject")

    class Callback(dynamic_proxy(jclass("com.github.catvod.utils.Dialog$Callback"))):
        def onAction(self, event):
            update = callback(json.loads(str(event)))
            if update is None:
                return None
            if not isinstance(update, dict):
                raise TypeError("Dialog action result must be a dict")
            return json_object(json.dumps(update, ensure_ascii=False, allow_nan=False))

    return Callback()


class DialogAdapter:
    def __init__(self):
        self._lock = Lock()
        self._native = None
        self._closed = False

    def __call__(self, options, callback=None):
        if not isinstance(options, dict):
            raise TypeError("Dialog options must be a dict")
        if callback is not None and not callable(callback):
            raise TypeError("Dialog callback must be callable")
        from java import jclass

        with self._lock:
            if self._closed:
                raise RuntimeError("Dialog owner is closed")
            if self._native is None:
                self._native = jclass("com.github.catvod.utils.Dialog")()
            native = self._native
        json_object = jclass("org.json.JSONObject")
        callback = None if callback is None else _callback(callback)
        result = native.show(json_object(json.dumps(options, ensure_ascii=False, allow_nan=False)), callback)
        return None if result is None else json.loads(str(result))

    def close(self):
        with self._lock:
            if self._closed:
                return
            self._closed = True
            native = self._native
        if native is not None:
            native.close()
