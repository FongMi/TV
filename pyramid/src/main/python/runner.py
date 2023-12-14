class Runner():
    def __init__(self, spider):
        self.spider = spider

    def getDependence(self):
        return self.spider.getDependence()

    def getName(self):
        return self.spider.getName()

    def init(self, extend=""):
        self.spider.init(extend)

    def homeContent(self, filter):
        return self.spider.homeContent(filter)

    def homeVideoContent(self):
        return self.spider.homeVideoContent()

    def categoryContent(self, tid, pg, filter, extend):
        return self.spider.categoryContent(tid, pg, filter, extend)

    def detailContent(self, ids):
        return self.spider.detailContent(ids)

    def searchContent(self, key, quick):
        return self.spider.searchContent(key, quick)

    def searchContentPage(self, key, quick, pg):
        return self.spider.searchContentPage(key, quick, pg)

    def playerContent(self, flag, id, vipFlags):
        return self.spider.playerContent(flag, id, vipFlags)

    def localProxy(self, param):
        return self.spider.localProxy(param)

    def isVideoFormat(self, url):
        return self.spider.isVideoFormat(url)

    def manualVideoCheck(self):
        return self.spider.manualVideoCheck()
