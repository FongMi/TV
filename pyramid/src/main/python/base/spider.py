import re
import json
import requests
from lxml import etree
from abc import abstractmethod, ABCMeta
from importlib.machinery import SourceFileLoader


class Spider(metaclass=ABCMeta):
    _instance = None

    def __new__(cls, *args, **kwargs):
        if cls._instance:
            return cls._instance
        else:
            cls._instance = super().__new__(cls)
            return cls._instance

    @abstractmethod
    def init(self, extend=""):
        pass

    @abstractmethod
    def homeContent(self, filter):
        pass

    @abstractmethod
    def homeVideoContent(self):
        pass

    @abstractmethod
    def categoryContent(self, tid, pg, filter, extend):
        pass

    @abstractmethod
    def detailContent(self, ids):
        pass

    @abstractmethod
    def searchContent(self, key, quick):
        pass

    def searchContentPage(self, key, quick, pg):
        pass

    @abstractmethod
    def playerContent(self, flag, id, vipFlags):
        pass

    @abstractmethod
    def localProxy(self, param):
        pass

    @abstractmethod
    def isVideoFormat(self, url):
        pass

    @abstractmethod
    def manualVideoCheck(self):
        pass

    @abstractmethod
    def getName(self):
        pass

    def getDependence(self):
        return []

    def regStr(self, src, reg, group=1):
        m = re.search(reg, src)
        src = ''
        if m:
            src = m.group(group)
        return src

    def str2json(self, str):
        return json.loads(str)

    def cleanText(self, src):
        clean = re.sub('[\U0001F600-\U0001F64F\U0001F300-\U0001F5FF\U0001F680-\U0001F6FF\U0001F1E0-\U0001F1FF]', '', src)
        return clean

    def fetch(self, url, cookies=None, headers=None, timeout=5, verify=True, stream=False, allow_redirects = True):
        rsp = requests.get(url, cookies=cookies, headers=headers, timeout=timeout, verify=verify, stream=stream, allow_redirects=allow_redirects)
        rsp.encoding = 'utf-8'
        return rsp

    def post(self, url, data, cookies=None, headers=None, timeout=5, verify=True, stream=False, allow_redirects = True):
        rsp = requests.post(url, data=data, cookies=cookies, headers=headers, timeout=timeout, verify=verify, stream=stream, allow_redirects=allow_redirects)
        rsp.encoding = 'utf-8'
        return rsp

    def postJson(self, url, json, cookies=None, headers=None, timeout=5, verify=True, stream=False, allow_redirects = True):
        rsp = requests.post(url, json=json, cookies=cookies, headers=headers, timeout=timeout, verify=verify, stream=stream, allow_redirects=allow_redirects)
        rsp.encoding = 'utf-8'
        return rsp

    def html(self, content):
        return etree.HTML(content)

    def xpText(self, root, expr):
        ele = root.xpath(expr)
        if len(ele) == 0:
            return ''
        else:
            return ele[0]

    def loadModule(self, name, path):
        return SourceFileLoader(name, path).load_module()
