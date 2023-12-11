import os
import requests
from importlib.machinery import SourceFileLoader
import json


def create_file(file_path):
    if os.path.exists(file_path) is False:
        os.makedirs(file_path)


def write_file(name, content):
    with open(name, 'wb') as f:
        f.write(content)


def redirect(url):
    rsp = requests.get(url, allow_redirects=False, verify=False)
    if 'Location' in rsp.headers:
        return redirect(rsp.headers['Location'])
    else:
        return rsp


def download_file(name, api):
    if api.startswith('http'):
        write_file(name, redirect(api).content)
    else:
        write_file(name, str.encode(api))


def init_py(cache, key, api):
    name = os.path.basename(api)
    path = cache + '/' + name
    download_file(path, api)
    return SourceFileLoader(name, path).load_module().Spider()


def str2json(content):
    return json.loads(content)


def init(ru, extend):
    ru.init(extend)


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
