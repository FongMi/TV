import json
from com.github.catvod import Proxy
from com.github.catvod.crawler import Spider as JavaSpider
from abc import abstractmethod, ABCMeta
from urllib.parse import urlencode


class Spider(metaclass=ABCMeta):
    _instance = None

    def __init__(self):
        self.extend = ''

    def __new__(cls, *args, **kwargs):
        if not cls._instance:
            cls._instance = super().__new__(cls)
        return cls._instance

    @abstractmethod
    def init(self, extend=""):
        pass

    def homeContent(self, filter):
        pass

    def homeVideoContent(self):
        pass

    def categoryContent(self, tid, pg, filter, extend):
        pass

    def detailContent(self, ids):
        pass

    def searchContent(self, key, quick, pg="1"):
        pass

    def playerContent(self, flag, id, vipFlags):
        pass

    def liveContent(self, url):
        pass

    def localProxy(self, param):
        pass

    def isVideoFormat(self, url):
        pass

    def manualVideoCheck(self):
        pass

    def action(self, action):
        pass

    def danmaku(self, id, emit):
        raise NotImplementedError("Spider does not support live danmaku")

    def destroy(self):
        pass

    def getName(self):
        pass

    def getActivity(self):
        from com.github.catvod import Runtime
        return Runtime.activity()

    def dialog(self, options, onAction=None):
        dialog = getattr(self, '_dialog', None)
        if dialog is None:
            raise RuntimeError("Dialog bridge is unavailable")
        return dialog.show(options, onAction)

    def getDependence(self):
        return []

    def loadSpider(self, name):
        spider = self._tv_runtime.spider(name)
        if hasattr(self, 'net'):
            spider.net = self.net
            spider.local = self.local
            spider.js = self.js
        if hasattr(self, '_dialog'):
            spider._dialog = self._dialog
        return spider

    def loadModule(self, name):
        return self._tv_runtime.load_reference(name)

    def getProxyUrl(self, params=None, local=True):
        params = {} if params is None else dict(params)
        if 'do' in params or 'siteKey' in params:
            raise ValueError('Reserved proxy parameter')
        url = str(Proxy.getUrl(local))
        query = urlencode({'do': 'py', 'siteKey': self.siteKey, **params})
        return url + '?' + query

    def getDanmakuUrl(self, id):
        return str(JavaSpider.getDanmakuUrl(self.siteKey, id))

    def log(self, msg):
        if isinstance(msg, (dict, list)):
            print(json.dumps(msg, ensure_ascii=False))
        else:
            print(f'{msg}')

    @staticmethod
    def link(name, target):
        if not isinstance(target, dict):
            raise TypeError("link requires a target dict")
        target = json.dumps(target, ensure_ascii=False, separators=(',', ':'), allow_nan=False)
        return '[a=cr:%s/]%s[/a]' % (target, name)

    @staticmethod
    def page(items, pg, limit, total=None):
        pg = max(1, int(pg))
        if isinstance(limit, bool) or not isinstance(limit, int) or limit < 1:
            raise ValueError("page requires a positive limit")
        if total is None:
            total = len(items)
            items = items[(pg - 1) * limit:pg * limit]
        if isinstance(total, bool) or not isinstance(total, int) or total < 0:
            raise ValueError("page requires a nonnegative total")
        return {
            'list': items,
            'page': pg,
            'pagecount': max(1, (total + limit - 1) // limit),
            'limit': limit,
            'total': total,
        }
