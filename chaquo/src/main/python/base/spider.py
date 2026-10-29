import json
from urllib.parse import urlencode

from com.github.catvod import Proxy
from com.github.catvod.crawler import Spider as JavaSpider


class Spider:
    _instance = None

    def __init__(self):
        self.extend = ""

    def __new__(cls, *args, **kwargs):
        if cls.__dict__.get("_instance") is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def init(self, extend=""):
        pass

    def homeContent(self, filter):
        return {"class": []}

    def homeVideoContent(self):
        return {"list": []}

    def categoryContent(self, tid, pg, filter, extend):
        return {"list": []}

    def detailContent(self, ids):
        return {"list": []}

    def searchContent(self, key, quick, pg="1"):
        return {"list": []}

    def playerContent(self, flag, id, vipFlags):
        return {}

    def liveContent(self, url):
        return ""

    def proxy(self, params):
        return [404, "text/plain", ""]

    def isVideoFormat(self, url):
        return False

    def manualVideoCheck(self):
        return False

    def action(self, action):
        return {}

    def danmaku(self, id, emit):
        raise NotImplementedError("Spider does not support live danmaku")

    def destroy(self):
        pass

    def getActivity(self):
        from com.github.catvod import Runtime

        return Runtime.activity()

    def getDependence(self):
        return []

    def loadSpider(self, name):
        spider = self.runtime.spider(name)
        if hasattr(self, "net"):
            spider.net = self.net
            spider.local = self.local
            spider.js = self.js
        if hasattr(self, "dialog"):
            spider.dialog = self.dialog
        return spider

    def loadModule(self, name):
        return self.runtime.load_reference(name)

    def getProxyUrl(self, params=None, local=True):
        params = {} if params is None else dict(params)
        if not isinstance(local, bool):
            raise TypeError("Proxy local flag requires a boolean")
        if "do" in params or "siteKey" in params:
            raise ValueError("Reserved proxy parameter")
        url = str(Proxy.getUrl(local))
        query = urlencode({"do": "py", "siteKey": self.siteKey, **params})
        return url + "?" + query

    def getDanmakuUrl(self, id):
        return str(JavaSpider.getDanmakuUrl(self.siteKey, id))

    def log(self, msg):
        if isinstance(msg, (dict, list)):
            print(json.dumps(msg, ensure_ascii=False))
        else:
            print(f"{msg}")

    @staticmethod
    def link(name, target):
        if not isinstance(target, dict):
            raise TypeError("link requires a target dict")
        target = json.dumps(target, ensure_ascii=False, separators=(",", ":"), allow_nan=False)
        return f"[a=cr:{target}/]{name!s}[/a]"

    @staticmethod
    def page(items, pg, limit, total=None):
        pg = max(1, int(pg))
        if isinstance(limit, bool) or not isinstance(limit, int) or limit < 1:
            raise ValueError("page requires a positive limit")
        if total is None:
            total = len(items)
            items = items[(pg - 1) * limit : pg * limit]
        if isinstance(total, bool) or not isinstance(total, int) or total < 0:
            raise ValueError("page requires a nonnegative total")
        return {"list": items, "page": pg, "pagecount": max(1, (total + limit - 1) // limit), "limit": limit, "total": total}
