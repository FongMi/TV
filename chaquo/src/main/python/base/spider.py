import re
import os
import json
import requests
from lxml import etree
from com.github.catvod import Proxy
from com.github.catvod.crawler import Spider as JavaSpider
from com.chaquo.python import Python
from abc import abstractmethod, ABCMeta
from importlib.machinery import SourceFileLoader
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

    def getDependence(self):
        return []

    def loadSpider(self, name):
        runtime = getattr(self, '_tv_runtime', None)
        if runtime is not None:
            spider = runtime.spider(name)
            if hasattr(self, 'net'):
                spider.net = self.net
                spider.local = self.local
            return spider
        return self.loadModule(name).Spider()

    def loadModule(self, name):
        runtime = getattr(self, '_tv_runtime', None)
        if runtime is not None:
            return runtime.load_reference(name)
        cache_dir = Python.getPlatform().getApplication().getCacheDir().getAbsolutePath()
        path = os.path.join(cache_dir, 'py', f'{name}.py')
        return SourceFileLoader(name, path).load_module()

    def regStr(self, reg, src, group=1):
        match = re.search(reg, src)
        return match.group(group) if match else ''

    def removeHtmlTags(self, src):
        return re.sub('<.*?>', '', src)

    def cleanText(self, src):
        return re.sub('[\U0001F600-\U0001F64F\U0001F300-\U0001F5FF\U0001F680-\U0001F6FF\U0001F1E0-\U0001F1FF]', '', src)

    def fetch(self, url, params=None, cookies=None, headers=None, timeout=5, verify=True, stream=False, allow_redirects = True):
        rsp = requests.get(url, params=params, cookies=cookies, headers=headers, timeout=timeout, verify=verify, stream=stream, allow_redirects=allow_redirects)
        rsp.encoding = 'utf-8'
        return rsp

    def post(self, url, params=None, data=None, json=None, cookies=None, headers=None, timeout=5, verify=True, stream=False, allow_redirects = True):
        rsp = requests.post(url, params=params, data=data, json=json, cookies=cookies, headers=headers, timeout=timeout, verify=verify, stream=stream, allow_redirects=allow_redirects)
        rsp.encoding = 'utf-8'
        return rsp

    def html(self, content):
        return etree.HTML(content)

    def str2json(str):
        return json.loads(str)

    def json2str(str):
        return json.dumps(str, ensure_ascii=False)

    def getProxyUrl(self, params=None, local=True):
        if isinstance(params, bool):
            local, params = params, None
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
