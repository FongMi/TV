import os
import requests
from importlib.machinery import SourceFileLoader
from urllib import parse
import json


def createFile(file_path):
    if os.path.exists(file_path) is False:
        os.makedirs(file_path)


def redirectResponse(tUrl):
    rsp = requests.get(tUrl, allow_redirects=False, verify=False)
    if 'Location' in rsp.headers:
        return redirectResponse(rsp.headers['Location'])
    else:
        return rsp


def downloadFile(name, url):
    try:
        rsp = redirectResponse(url)
        with open(name, 'wb') as f:
            f.write(rsp.content)
        print(url)
    except:
        print(name + ' =======================================> error')
        print(url)


def downloadPlugin(basePath, url):
    createFile(basePath)
    name = url.split('/')[-1].split('.')[0]
    if url.startswith('file://'):
        pyName = url.replace('file://', '')
    else:
        pyName = basePath + name + '.py'
        downloadFile(pyName, url)
    sPath = gParam['SpiderPath']
    sPath[name] = pyName
    sParam = gParam['SpiderParam']
    paramList = parse.parse_qs(parse.urlparse(url).query).get('extend')
    if paramList == None:
        paramList = ['']
    sParam[name] = paramList[0]
    return pyName


def loadFromDisk(fileName):
    name = fileName.split('/')[-1].split('.')[0]
    spList = gParam['SpiderList']
    if name not in spList:
        sp = SourceFileLoader(name, fileName).load_module().Spider()
        spList[name] = sp
    return spList[name]


def str2json(content):
    return json.loads(content)


gParam = {
    "SpiderList": {},
    "SpiderPath": {},
    "SpiderParam": {}
}


def getDependence(ru):
    result = ru.getDependence()
    return result


def getName(ru):
    result = ru.getName()
    return result


def init(ru, extend):
    spoList = []
    spList = gParam['SpiderList']
    sPath = gParam['SpiderPath']
    sParam = gParam['SpiderParam']
    for key in ru.getDependence():
        sp = None
        if key in spList.keys():
            sp = spList[key]
        elif key in sPath.keys():
            sp = loadFromDisk(sPath[key])
        if sp != None:
            sp.setExtendInfo(sParam[key])
            spoList.append(sp)
    ru.setExtendInfo(extend)
    ru.init(spoList)


def homeContent(ru, filter):
    result = ru.homeContent(filter)
    formatJo = json.dumps(result, ensure_ascii=False)
    return formatJo


def homeVideoContent(ru):
    result = ru.homeVideoContent()
    formatJo = json.dumps(result, ensure_ascii=False)
    return formatJo


def categoryContent(ru, tid, pg, filter, extend):
    result = ru.categoryContent(tid, pg, filter, str2json(extend))
    formatJo = json.dumps(result, ensure_ascii=False)
    return formatJo


def detailContent(ru, array):
    result = ru.detailContent(str2json(array))
    formatJo = json.dumps(result, ensure_ascii=False)
    return formatJo


def playerContent(ru, flag, id, vipFlags):
    result = ru.playerContent(flag, id, str2json(vipFlags))
    formatJo = json.dumps(result, ensure_ascii=False)
    return formatJo


def searchContent(ru, key, quick):
    result = ru.searchContent(key, quick)
    formatJo = json.dumps(result, ensure_ascii=False)
    return formatJo


def localProxy(ru, param):
    result = ru.localProxy(str2json(param))
    return result


def run():
    pass


if __name__ == '__main__':
    run()
