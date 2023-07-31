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


def download_file(name, ext):
    if ext.startswith('http'):
        write_file(name, redirect(ext).content)
    else:
        write_file(name, str.encode(ext))


def init_py(path, name, ext):
    py_name = path + '/' + name + '.py'
    download_file(py_name, ext)
    return SourceFileLoader(name, py_name).load_module().Spider()


def str2json(content):
    return json.loads(content)


def init(ru, extend):
    ru.init([""])


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
