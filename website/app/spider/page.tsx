import type { Metadata } from "next";
import { DocsNav } from "../DocsNav";
import Link from "next/link";
import { CodeTabs, type CodeSample } from "../CodeTabs";
import { CodeBlock, DocsLayout } from "../ui";
import { FieldTable } from "../FieldTable";
import { resultFields, objectGroups, methods, networkMethods, liveDanmakuFields } from "../spider-fields";

export const metadata: Metadata = {
  title: "爬蟲介接",
  description: "Java、Python、QuickJS 爬蟲範例、共用接口與回傳資料格式。",
};

const java = `// 最小點播範例；example.com 為示意網址。
package com.github.catvod.spider;

import com.github.catvod.crawler.Spider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class Demo extends Spider {

    // 回傳分類清單。
    @Override
    public String homeContent(boolean filter) throws Exception {
        JSONObject item = new JSONObject().put("type_id", "movie").put("type_name", "電影");
        return new JSONObject().put("class", new JSONArray().put(item)).toString();
    }

    // 範例只有一頁；第二頁起回傳空清單。
    @Override
    public String categoryContent(String tid, String pg, boolean filter,
            HashMap<String, String> extend) throws Exception {
        int page = Math.max(1, Integer.parseInt(pg));
        JSONArray items = new JSONArray();
        if (page == 1) items.put(card("demo-1", "範例項目"));
        return new JSONObject().put("list", items).put("page", page)
                .put("pagecount", 1).put("limit", 20).put("total", 1).toString();
    }

    // 集數先使用穩定 ID，播放時再取得網址。
    @Override
    public String detailContent(List<String> ids) throws Exception {
        JSONObject item = card(ids.get(0), "範例項目").put("vod_play_from", "Demo")
                .put("vod_play_url", "第 1 集$demo://episode/1");
        return new JSONObject().put("list", new JSONArray().put(item)).toString();
    }

    // 將播放 ID 換成實際網址。
    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        String url = id.replace("demo://", "https://example.com/");
        return new JSONObject().put("parse", 0).put("url", url).toString();
    }

    private JSONObject card(String id, String name) throws Exception {
        return new JSONObject().put("vod_id", id).put("vod_name", name)
                .put("vod_pic", "https://example.com/poster.jpg");
    }
}`;

const python = `# 最小點播範例；example.com 為示意網址。
from base.spider import Spider as BaseSpider


class Spider(BaseSpider):
    def init(self, extend=""):
        pass

    # 回傳分類清單。
    def homeContent(self, filter):
        return {
            "class": [{"type_id": "movie", "type_name": "電影"}]
        }

    # 完整清單交給共用方法分頁。
    def categoryContent(self, tid, pg, filter, extend):
        items = [self.card("demo-1", "範例項目")]
        return self.page(items, pg, 20)

    # 集數先使用穩定 ID，播放時再取得網址。
    def detailContent(self, ids):
        item = self.card(ids[0], "範例項目")
        item.update({
            "vod_play_from": "Demo",
            "vod_play_url": "第 1 集$demo://episode/1"
        })
        return {"list": [item]}

    # 將播放 ID 換成實際網址。
    def playerContent(self, flag, id, vipFlags):
        url = id.replace("demo://", "https://example.com/")
        return {"parse": 0, "url": url}

    def card(self, id, name):
        return {
            "vod_id": id,
            "vod_name": name,
            "vod_pic": "https://example.com/poster.jpg"
        }`;

const javascript = `// 最小點播範例；example.com 為示意網址。
const card = (id, name) => ({
  vod_id: id,
  vod_name: name,
  vod_pic: "https://example.com/poster.jpg"
});

export default {
  init(ext) {},

  // 回傳分類清單。
  home(filter) {
    return {
      class: [{ type_id: "movie", type_name: "電影" }]
    };
  },

  // 完整清單交給共用方法分頁。
  category(tid, pg, filter, extend) {
    const items = [card("demo-1", "範例項目")];
    return page(items, pg, 20);
  },

  // 集數先使用穩定 ID，播放時再取得網址。
  detail(id) {
    const item = {
      ...card(id, "範例項目"),
      vod_play_from: "Demo",
      vod_play_url: "第 1 集$demo://episode/1"
    };
    return { list: [item] };
  },

  // 將播放 ID 換成實際網址。
  play(flag, id, vipFlags) {
    const url = id.replace("demo://", "https://example.com/");
    return { parse: 0, url };
  }
};`;

const resultExplore = `{
  "class": [
    { "type_id": "movie", "type_name": "電影" }
  ],
  "filters": {
    "movie": [
      {
        "key": "area",
        "name": "地區",
        "init": "",
        "value": [
          { "n": "全部", "v": "" },
          { "n": "台灣", "v": "tw" }
        ]
      }
    ]
  },
  "list": [
    {
      "vod_id": "demo-1",
      "vod_name": "範例項目",
      "vod_pic": "https://example.com/poster.jpg",
      "vod_remarks": "示意"
    }
  ],
  "pagecount": 1,
  "msg": "選填提示"
}`;

const resultPlay = `{
  "url": "https://example.com/video/demo-1.m3u8",
  "parse": 0,
  "header": {
    "User-Agent": "ExampleClient/1.0",
    "Referer": "https://example.com/"
  },
  "format": "application/x-mpegURL",
  "subs": [
    {
      "url": "https://example.com/subs/demo-1.vtt",
      "name": "繁體中文",
      "lang": "zh-TW",
      "format": "text/vtt",
      "flag": 0
    }
  ],
  "danmaku": [
    { "url": "https://example.com/danmaku/demo-1.xml", "name": "示意彈幕" }
  ],
  "artwork": "https://example.com/poster.jpg",
  "desc": "示意播放資訊",
  "position": 120000
}`;

const samples: CodeSample[] = [
  {
    id: "java",
    label: "Java",
    file: "com/github/catvod/spider/Demo.java",
    code: java,
  },
  { id: "python", label: "Python", file: "demo.py", code: python },
  { id: "javascript", label: "QuickJS", file: "demo.js", code: javascript },
];

const streamSamples: CodeSample[] = [
  {
    id: "java",
    label: "Java",
    file: "Demo.java / proxy",
    code: String.raw`NetOptions options = NetOptions.from("{\"timeout\":15000,\"callTimeout\":15000}");
Net.StreamResult response = net.requestStreamAsync("https://example.com/data.bin", options).get();
return new Object[]{response.code, response.contentType, response, response.headers};`,
  },
  {
    id: "python",
    label: "Python",
    file: "spider.py / localProxy",
    code: `response = self.net.stream("https://example.com/data.bin", {
    "timeout": 15000, "callTimeout": 15000
})
return [response["code"], response["contentType"], response["stream"], response["headers"]]`,
  },
  {
    id: "javascript",
    label: "QuickJS",
    file: "spider.js / proxy",
    code: `const response = await net.stream("https://example.com/data.bin", {
  timeout: 15000, callTimeout: 15000
});
return [response.code, response.contentType, response.stream, response.headers];`,
  },
];

export default function SpiderPage() {
  return (
    <DocsLayout
      title="爬蟲介接指南"
      lead="從 Java、Python、QuickJS 範例開始，了解載入、共用接口與回傳格式。"
    >
      <div className="docs-layout wrap">
        <DocsNav items={[
          ["start", "載入爬蟲"],
          ["modules", "安裝執行模組"],
          ["testing", "測試爬蟲"],
          ["demo", "三語言 Demo"],
          ["lifecycle", "方法與呼叫時機"],
          ["playback-details", "播放詳情更新"],
          ["network", "共用網路"],
          ["streams", "串流讀取與代理"],
          ["websocket", "WebSocket"],
          ["cache", "快取與儲存"],
          ["scripts", "執行 JavaScript"],
          ["runtime", "Android UI"],
          ["result", "Result 回傳格式"],
          ["objects", "資料物件"],
          ["episodes", "播放分組與集數"],
          ["proxy", "Proxy 回傳"],
          ["live-danmaku", "即時彈幕"],
          ["timeshift", "直播回看"],
          ["skips", "片段跳過"],
          ["web", "開網頁"],
          ["node", "Node.js 載入"],
        ]} />
        <div className="docs-content">
          <section id="start">
            <h2>載入爬蟲</h2>
            <h3>Site 最小配置</h3>
            <CodeBlock label="JSON / Site" path="sites[] 物件片段">{`{
  "key": "demo",
  "name": "Demo",
  "type": 3,
  "api": "./demo.js",
  "ext": {}
}`}</CodeBlock>
            <div className="loader-list">
              <article>
                <strong>Java JAR</strong>
                <code>{'api: "csp_Demo"'}</code>
                <p>
                  類別放在 <code>com.github.catvod.spider.Demo</code>
                  ，並繼承抽象 Spider；主配置必須提供 <code>spider</code>{" "}
                  或站點的 <code>jar</code>。使用 <code>catvod-api.jar</code>{" "}
                  作為 <code>compileOnly</code> 編譯依賴，由 TV 根任務{" "}
                  <code>:apiJar</code> 產生於{" "}
                  <code>build/outputs/api/catvod-api.jar</code>。
                </p>
              </article>
              <article>
                <strong>Python</strong>
                <code>{'api: "./demo.py"'}</code>
                <p>
                  相對路徑以配置檔所在位置為基準；輸出 <code>Spider</code>{" "}
                  類別並繼承 <code>base.spider.Spider</code>。
                </p>
              </article>
              <article>
                <strong>QuickJS</strong>
                <code>{'api: "./demo.js"'}</code>
                <p>
                  JavaScript 由 QuickJS 執行；相對路徑以配置檔所在位置為基準。
                  使用 ES module，預設匯出物件或建立物件的函式。
                </p>
              </article>
            </div>
          </section>
          <section id="modules">
            <h2>安裝執行模組</h2>
            <p>
              在 App 的模組頁安裝 Python、QuickJS 或 Node.js 的獨立 APK。
              Thunder、mpv 與 BD-J 也在此管理，按需要安裝。
            </p>
            <div className="loader-list">
              <article>
                <strong>安裝與更新</strong>
                <p>
                  選擇模組、下載並完成系統安裝。
                  版本需與 App 的模組接口及裝置 ABI 相容。
                </p>
              </article>
              <article>
                <strong>測試與重啟</strong>
                <p>
                  安裝後執行模組測試，確認能初始化。
                  若提示重新啟動，重開 App 後再載入爬蟲。
                </p>
              </article>
            </div>
            <p>
              QuickJS 使用 QuickJS-NG 引擎，不能直接執行 Node.js 套件；
              Node.js 請依 <Link className="inline-link" href="/spider#node">獨立載入方式</Link> 設定。
              使用新接口時，請一併更新 App 與對應模組。
            </p>
          </section>
          <section id="testing">
            <h2>測試爬蟲</h2>
            <p>
              連線 App 的網頁，從「設定 → 爬蟲測試」開啟 <code>/spider.html</code>。
              Python 與 QuickJS 需先安裝對應模組。
            </p>
            <ol>
              <li>載入 Java JAR 並選擇類別，或載入 .py／.js；ext 為選填。</li>
              <li>選擇方法，以 JSON 陣列填入參數，執行並查看結果與網路紀錄。</li>
              <li>修改後儲存、重新載入並重測；可格式化來源或還原版本。</li>
              <li>play 回傳有效結果後，送到 App 確認播放。</li>
            </ol>
            <p>
              App 不編譯 Java，修改 .java 後需重新產生並載入 JAR。
              測試逾時預設 30 秒，可設為 1–300 秒；重新載入、取消或離開會清理測試工作。
              來源若已被其他頁面修改，先重新讀取再儲存。
            </p>
            <div className="callout">
              <b>先確認回傳，再確認播放</b>
              模組測試與正確的 URL 不代表播放成功，仍需在 App 確認畫面、聲音、字幕與彈幕。
            </div>
          </section>
          <section id="demo">
            <h2>三語言 Demo</h2>
            <p>
              範例流程為「分類 → 列表 → 詳情 → 播放」。請將 example.com 換成自己的來源，
              並補上搜尋與錯誤處理。
            </p>
            <div className="callout">
              <b>回傳格式</b>Java 用 Android 內建的 <code>JSONObject</code>／
              <code>JSONArray</code> 組裝 JSON 字串；Python 回傳
              dict／list，QuickJS 回傳物件／陣列，由 App 序列化。
              直播可回傳 Group 陣列或 TXT／M3U 原始文字。
            </div>
            <CodeTabs samples={samples} />
            <p>
              完整範例見 CatVodSpider 的{" "}
              <a href="https://github.com/FongMi/CatVodSpider/blob/main/app/src/main/java/com/github/catvod/spider/Demo.java">Java</a>、
              <a href="https://github.com/FongMi/CatVodSpider/blob/main/examples/python/spider.py">Python</a> 與{" "}
              <a href="https://github.com/FongMi/CatVodSpider/blob/main/examples/javascript/spider.js">QuickJS</a>，
              涵蓋篩選、搜尋分頁、直播、快取與網頁操作。
            </p>
          </section>
          <section id="lifecycle">
            <h2>方法與呼叫時機</h2>
            <p>
              本表適用 Java、Python 與 QuickJS；Node.js 使用 <Link className="inline-link" href="/spider#node">獨立 HTTP 端點</Link>。
            </p>
            <p>
              在 <code>destroy</code> 釋放自建 Session、WebSocket 與背景工作。
              QuickJS 最多等待 5 秒（含 Promise），逾時仍會繼續清理引擎。
            </p>
            <div className="table-scroll">
              <table
                className="method-table"
                aria-label="Spider 方法與呼叫時機"
              >
                <thead>
                  <tr>
                    <th scope="col">方法</th>
                    <th scope="col">何時呼叫</th>
                    <th scope="col">責任</th>
                    <th scope="col">主要回傳</th>
                  </tr>
                </thead>
                <tbody>
                  {methods.map((row) => (
                    <tr key={row[0]}>
                      {row.map((cell, index) => (
                        <td
                          key={index}
                          data-label={
                            ["方法", "何時呼叫", "責任", "主要回傳"][index]
                          }
                        >
                          {index === 0 ? <code>{cell}</code> : cell}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
          <section id="playback-details">
            <h2>播放詳情更新</h2>
            <p>
              <code>playerContent</code>／<code>play</code> 可在播放結果的 <code>list[0]</code> 帶回 VOD 詳情。
              App 採用結果時更新頁面；預載結果在切集時套用，取消或過期的結果會丟棄。
            </p>
            <CodeBlock label="JSON / Result" path="播放結果附帶詳情">{`{
  "parse": 0,
  "url": "https://example.com/video.mp4",
  "list": [
    {
      "vod_id": "demo-1",
      "vod_name": "範例項目",
      "vod_pic": "https://example.com/cover.jpg",
      "type_name": "動畫",
      "vod_year": "2026",
      "vod_area": "台灣",
      "vod_director": "示範導演",
      "vod_actor": "演員甲、演員乙",
      "vod_content": "完整簡介",
      "vod_remarks": "更新至第 2 集",
      "vod_play_from": "主要線",
      "vod_play_url": "第1集$episode-1#第2集$episode-2"
    }
  ]
}`}</CodeBlock>
            <p>
              Java 使用 <code>Response.vod(vod)</code>，Python／QuickJS 使用 <code>list: [vod]</code>；
              Node.js 的 <code>/play</code> 與 type=4 API 使用相同的 JSON 格式。
              範例見 <a className="inline-link" href="https://github.com/FongMi/CatVodSpider">CatVodSpider Demo</a>。
            </p>
            <p>
              同名播放分組合併 episode，相同名稱與播放值去重，其餘追加，保留目前選集位置。
              <code>vod_id</code> 可省略，提供時會連動歷史與收藏識別。
            </p>
          </section>
          <section id="network">
            <h2>共用網路</h2>
            <p>
              App 在初始化前提供網路接口：Java 使用 <code>net</code>、Python 使用 <code>self.net</code>、QuickJS 使用 <code>net</code>。
              時間單位為毫秒。完整參數與範例見 <a href="https://github.com/FongMi/CatVodSpider/blob/main/docs/development.md#網路">CatVodSpider 開發說明</a>。
            </p>
            <div className="table-scroll">
              <table className="method-table" aria-label="共用網路方法">
                <thead>
                  <tr><th scope="col">方法</th><th scope="col">用途</th></tr>
                </thead>
                <tbody>
                  {networkMethods.map(([name, description]) => (
                    <tr key={name}>
                      <td data-label="方法"><code>{name}</code></td>
                      <td data-label="用途">{description}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <h3>HTTP</h3>
            <p>
              只取 JSON 用 <code>net.json(url, options)</code>；連線失敗、HTTP 非 2xx 或無效 JSON 會拋出例外。
              需要狀態碼、錯誤正文或 bytes 時用 <code>net.req</code>，QuickJS 用 <code>await net.http</code>。
              Java 的 options 除特別標示外使用 JSON 字串，json／req 也回傳 JSON 字串。
            </p>
            <p>
              QuickJS 的 req 同步執行；http／json 以 await 取得結果。Python／QuickJS 使用物件傳 options，
              二進位回應用 <code>buffer: 3</code>，分別取得 bytes／ArrayBuffer。
              <code>net.getUserAgent()</code> 取得 App 的 UA，需要時自行放入 headers。
            </p>
            <h3>Cookie 與 Session</h3>
            <p>
              <code>cookie: true</code> 使用並更新 WebView Cookie；以 <code>getCookie(url)</code> 讀取、
              <code>setCookie(url, cookie)</code> 寫入單筆 Set-Cookie。
              <code>net.session()</code> 自動管理隔離 Cookie，不共用站點的 Cookie 或 TTL 快取，也不支援 persist。
            </p>
            <p>
              Session 用完須關閉：Java 用 try-with-resources、Python 用 with、QuickJS 用 finally。
              關閉會取消其請求、串流與連線，之後不可繼續使用。
            </p>
            <h3>批次與下載</h3>
            <p>
              <code>net.batch(requests, limit)</code> 每筆傳入 <code>&#123;url, options&#125;</code>，預設最多 4 筆並行，按輸入順序回傳。
              每項還需快取、簽名或解析時用 <code>net.map(items, mapper, limit)</code>，回呼收 item／index，各項自行檢查 HTTP 狀態。
              拋錯後停止派送，已開始的工作繼續完成；Java／Python 在背景執行緒回呼，QuickJS 可用 async。
            </p>
            <p>
              Java 下載用 <code>net.download(url, optionsJson, file)</code>，Python／QuickJS 用 <code>net.download(url, path, options)</code>。
              <code>threads</code> 為 0 自動、1 單連線、2–4 並行上限；<code>size</code> 指定完整 bytes，<code>maxBytes</code> 限制寫入大小。
              續傳與分段依來源的 Range 與驗證資訊決定，cookie:true／Session 不續傳；完成後仍須確認檔案內容。
            </p>
          </section>
          <section id="streams">
            <h2>串流讀取與代理</h2>
            <p>
              Java 用 <code>net.requestStreamAsync(url, NetOptions)</code>，Future 回傳繼承 InputStream 的 <code>Net.StreamResult</code>。
              Python 用 <code>self.net.openStream(url, options)</code>，QuickJS 用 <code>await net.openStream(url, options)</code>。
              用完以 try-with-resources、with 或 finally 關閉。
            </p>
            <h3>原樣轉送</h3>
            <p>
              將下列片段放入對應的 proxy 方法。成功交付後由 App 關閉 stream，不交付時自行 close。
              QuickJS handle 只供當次 proxy 使用一次，不存入 Local／快取；Java 需引入 Net 與 NetOptions。
            </p>
            <CodeTabs samples={streamSamples} />
            <h3>自訂串流正文</h3>
            <p>
              Python 用 <code>self.net.stream(generator())</code>，yield bytes／bytearray／連續 memoryview；
              QuickJS 用 <code>await net.stream(asyncGenerator())</code>，yield ArrayBuffer／TypedArray。
              將回應的 stream 放入 proxy 第三格；Java 直接回傳自訂 InputStream。
            </p>
            <p>
              產生器在播放器讀取時才執行，不接受 HTTP options。reader 在產生器內開啟，以 with／finally 關閉；
              App 關閉 body 時取消其等待中的串流請求與讀取，再關閉產生器。
            </p>
            <p>
              直播可用 <code>callTimeout: 0</code> 關閉整次 HTTP 期限，保留 timeout 防止讀取卡住；Demo 使用 15 秒期限。
              讀取大小、EOF 與取消限制見 <a href="https://github.com/FongMi/CatVodSpider/blob/main/docs/development.md#串流讀取與代理">開發說明</a>。
            </p>
          </section>
          <section id="websocket">
            <h2>WebSocket</h2>
            <p>
              一次交換用 <code>net.ws(url, options)</code>，取得首個完整訊息後關閉。
              持續收訊用 <code>net.connect(url, options, callback)</code>，回呼接收 <code>(socket, event)</code>，
              事件為 open／message／retry／error／close。
            </p>
            <p>
              content 為文字，二進位則為 Java byte[]、Python bytes 或 QuickJS Uint8Array。
              使用回呼傳入的 <code>socket.send()</code> 發送、<code>socket.close()</code> 關閉，需立即中斷時用 cancel。
              站點或 Session 釋放時也會取消連線。
            </p>
            <p>
              timeout 只限制建立連線；ping 是協定 Ping 的毫秒間隔。
              idle 是未收到文字／二進位訊息的逾時毫秒數，預設 0 關閉；送出訊息與 Ping／Pong 不會重設期限。
              平台訂閱與應用層心跳由爬蟲處理，只在來源保證持續回訊時設定 idle。
            </p>
            <p>
              <code>reconnect: true</code> 啟用斷線恢復，預設關閉，等待從 1 秒倍增至 30 秒。
              每次 open 重新訂閱，retry 時停止心跳並清除殘包。
              主動關閉、釋放或認證／憑證／協定／回呼錯誤後不再重連。
            </p>
          </section>
          <section id="cache">
            <h2>快取與儲存</h2>
            <h3>TTL 快取</h3>
            <p>
              <code>net.cached(key, options, loader)</code> 按站點快取 JSON，同 key 合併載入。
              ttl 必填；stale 預設 true，過期先回舊值並背景更新；persist 預設 false，啟用時持久化公開資料。
              播放網址使用短 TTL、<code>stale: false</code>，不持久化。
            </p>
            <p>
              <code>peek(key)</code> 只讀已有值，包括過期值；<code>refresh(key, options, loader)</code> 立即返回，依 TTL 背景更新。
              <code>clearCache(key)</code> 清一筆，省略 key 清除目前站點全部 TTL 快取並取消載入。
            </p>
            <p>
              Python 的 <code>self.net.cachedText(key, options, loader)</code> 取得 JSON 原文；cached 則解碼為 Python 值，兩者共用 TTL 與清除規則。
              Java 的 cached 回傳 JSON 字串。
            </p>
            <h3>Local</h3>
            <p>
              小型設定用 Java 的 <code>local</code>、Python 的 <code>self.local</code> 或 QuickJS 的 <code>local</code>。
              以 get(key)／set(key, value)／delete(key) 讀寫，App 按站點隔離；Java 使用 JSON 字串，Python／QuickJS 使用 JSON 值。
              清 TTL 快取不會刪除 Local。
            </p>
          </section>
          <section id="scripts">
            <h2>執行 JavaScript</h2>
            <p>
              用 <code>js.eval(source, options)</code> 在獨立環境執行短段 JavaScript。
              Java 的 options 與回傳值使用 JSON 字串；Python 用 <code>self.js.eval</code>，QuickJS 用 <code>await js.eval</code>。
            </p>
            <p>
              <code>globals</code> 傳 JSON 變數，<code>timeout</code> 為正整數毫秒，預設 5000；內建 <code>md5X</code>。
              回傳最後一個運算式的 JSON 值，不接受 Promise。環境不提供網路、DOM 或模組匯入，完成、逾時或取消後即關閉。
              三種語言都需要 QuickJS 模組。
            </p>
          </section>
          <section id="runtime">
            <h2>Android UI</h2>
            <p>
              共用 SDK 的 <code>com.github.catvod.Runtime</code> 以 <code>Runtime.context()</code>
              取得 Application Context、<code>Runtime.activity()</code> 取得目前 Activity，
              <code>Runtime.toast()</code> 顯示文字提示。Java 需明確匯入這個類別；Python
              使用 <code>jclass</code> 取得。這些接口由宿主提供，爬蟲 JAR 使用
              <code>compileOnly</code> 依賴 SDK。
            </p>
            <p>
              繼承 Spider 的 Java／Python 爬蟲也可使用 <code>getActivity()</code>／
              <code>self.getActivity()</code>。Activity 可能為 null／None；自訂 Dialog
              在主執行緒顯示時才取得 Activity，不保存頁面引用。
              <code>Runtime.toast()</code> 由 App 切到主執行緒顯示。
              QuickJS 的文字提示可從 action 回傳 <code>msg</code> 欄位。
            </p>
            <p>
              JAR 自己的 <code>com.github.catvod.spider.Init.init(Context)</code>
              是可選的初始化入口，與 <code>Runtime</code> 分開；每個爬蟲仍由 App
              呼叫 <code>Spider.init(Context, ext)</code>。
            </p>
          </section>
          <section id="result">
            <h2>Result 回傳格式</h2>
            <div className="schema-grid">
              <article>
                <h3>探索類</h3>
                <CodeBlock label="JSON / Result" path="home · category">
                  {resultExplore}
                </CodeBlock>
                <p>
                  <code>homeContent</code> 使用 class /
                  filters；列表、搜尋、詳情使用 list。filters
                  第一層鍵必須等於分類的 type_id 值，本例是 movie。
                </p>
              </article>
              <article>
                <h3>播放類</h3>
                <CodeBlock label="JSON / Result" path="playerContent">
                  {resultPlay}
                </CodeBlock>
                <p>
                  <code>url</code> 是核心欄位；<code>parse=0</code>{" "}
                  表示此欄位未要求解析，但 <code>jx=1</code>{" "}
                  或配置旗標比對仍可能觸發解析。
                </p>
              </article>
            </div>
            <h3>完整 Result 欄位</h3>
            <FieldTable fields={resultFields} label="Result" />
            <h3>url 的多畫質寫法</h3>
            <p>
              單一 URL
              見上方播放範例。陣列必須以「名稱、URL」成對排列，不是單純的 URL
              清單；也可改用 values 物件。頂層 position 是毫秒，Url.position
              則是從 0 起算的畫質索引，實際選取仍會由 App 調整。
            </p>
            <div className="schema-grid">
              <article>
                <h3>名稱與 URL 成對陣列</h3>
                <CodeBlock label="JSON / Result" path="playerContent">{`{
  "parse": 0,
  "url": [
    "主畫質", "https://example.com/video/main.m3u8",
    "低畫質", "https://example.com/video/low.m3u8"
  ]
}`}</CodeBlock>
              </article>
              <article>
                <h3>Url 物件</h3>
                <CodeBlock label="JSON / Result" path="playerContent">{`{
  "parse": 0,
  "url": {
    "values": [
      { "n": "主畫質", "v": "https://example.com/video/main.m3u8" },
      { "n": "低畫質", "v": "https://example.com/video/low.m3u8" }
    ],
    "position": 0
  }
}`}</CodeBlock>
              </article>
            </div>
            <p>
              <code>drm</code> 的子欄位請參考
              <Link className="inline-link" href="/config#common">
                共用物件字典
              </Link>
              ；詳情的播放分組見 <Link className="inline-link" href="/spider#episodes">集數格式</Link>。
            </p>
          </section>
          <section id="objects">
            <h2>資料物件</h2>
            <p>
              演員或分類超連結使用 Java 的 <code>Json.link(name, target)</code>、
              Python 的 <code>self.link(name, target)</code> 或 QuickJS 的 <code>link(name, target)</code>。
              target 包含目標分類及篩選，可用 id、name、type_flag、filter。
            </p>
            <div className="object-grid">
              {objectGroups.map((group) => (
                <article key={group.name}>
                  <h3>{group.name}</h3>
                  <dl>
                    {group.fields.map(([name, description]) => (
                      <div key={name}>
                        <dt>{name}</dt>
                        <dd>{description}</dd>
                      </div>
                    ))}
                  </dl>
                </article>
              ))}
            </div>
          </section>
          <section id="episodes">
            <h2>播放分組與集數</h2>
            <div className="delimiter-grid">
              <div>
                <b>$$$</b>
                <span>分隔播放分組</span>
              </div>
              <div>
                <b>#</b>
                <span>分隔同組集數</span>
              </div>
              <div>
                <b>$</b>
                <span>分隔集數名稱與 id</span>
              </div>
            </div>
            <CodeBlock
              label="JSON / Vod fields"
              path="list[] 中的 Vod 欄位片段"
            >{`{
  "vod_play_from": "主線路$$$備用線路",
  "vod_play_url": "第 01 集$demo://ep/1#第 02 集$demo://ep/2$$$第 01 集$backup://ep/1"
}`}</CodeBlock>
            <p>
              使用者點擊某集後，<code>$</code> 右側的 value 會成為{" "}
              <code>playerContent(flag, id, …)</code> 的 <code>id</code>。
            </p>
          </section>
          <section id="proxy">
            <h2>Proxy 回傳</h2>
            <CodeBlock label="JSON / Proxy 陣列" path="proxy / localProxy">{`[
  200,
  "text/plain; charset=utf-8",
  "demo response",
  { "Cache-Control": "no-cache" }
]`}</CodeBlock>
            <p>
              上例是 Python／QuickJS
              一般陣列模式：狀態碼、Content-Type、內容、選填標頭；第 5 格{" "}
              <code>1</code> 表示 Base64 內容。Python 方法名稱是{" "}
              <code>localProxy</code>，第三格也可直接回傳 bytes。
            </p>
            <p>
              Java 的一般 <code>Object[]</code> 回傳格式中，第三格必須是{" "}
              <code>InputStream</code>，第四格標頭選填，不能直接放字串或
              bytes；也可只回傳一格 <code>NanoHTTPD.Response</code>。QuickJS 的{" "}
              <code>from=catvod</code> 另有 JSON response 模式；Node Proxy
              則直接轉接 HTTP 回應。
            </p>
            <p>
              使用 <code>getProxyUrl(params)</code> 產生網址，App 自動加入{" "}
              <code>do</code>、<code>siteKey</code> 並編碼參數。JAR 模式沒有
              siteKey 時會找{" "}
              <code>com.github.catvod.spider.Proxy.proxy(Map)</code> 靜態入口。
            </p>
          </section>
          <section id="live-danmaku">
            <h2>即時彈幕</h2>
            <p>
              Java／Python／QuickJS 的播放回應沿用 <code>danmaku: [&#123;name, url&#125;]</code>。
              即時來源的 url 用 <code>getDanmakuUrl(id)</code> 產生，Python 使用{" "}
              <code>self.getDanmakuUrl(id)</code>；App 會呼叫該站點的 <code>danmaku(id, emit)</code>。
              HTTP(S) 來源仍按彈幕檔案載入。
            </p>
            <CodeBlock label="JSON / 即時彈幕" path="emit(item)">{`{
  "text": "即時彈幕",
  "color": 6737151,
  "size": 25,
  "type": 1,
  "colors": [6737151, 13408767]
}`}</CodeBlock>
            <FieldTable fields={liveDanmakuFields} label="即時彈幕" />
            <p>
              狀態也使用 <code>emit</code>，不可同時包含 text。
              state 為 ready（訂閱可用）、retrying（恢復中）或 error（終止失敗）；
              msg 選填，最多 512 字元，不放 Cookie、token 或簽名網址。
              ready 須確認平台訂閱可用，不能僅以 WebSocket open 判斷。
            </p>
            <CodeBlock label="JSON / 訂閱狀態" path="emit(item)">{`{
  "state": "retrying",
  "msg": "連線中斷，等待恢復"
}`}</CodeBlock>
            <p>
              App 自行設定 connecting、合併相同 state／msg；初始化例外轉成 error。
              error 後停止訂閱並忽略後續回呼，主動停止則清除狀態，不需回報 closed。
              狀態記錄在日誌，不會當成彈幕或反覆跳出提示。
            </p>
            <p>
              <code>emit</code> 接收 Java JSONObject、Python dict 或 QuickJS 物件；Java 用{" "}
              <code>emit.accept(item)</code> 並回傳 AutoCloseable，Python／QuickJS 回傳停止函式。
              切台或關閉時，App 呼叫停止；爬蟲須取消自己的連線與心跳。
            </p>
            <p>
              爬蟲負責協定、解碼與樣式轉換，App 負責顯示限制，超量或過期訊息可能丟棄。
              完整範例見 CatVodSpider 的三語言 Demo。
            </p>
          </section>
          <section id="timeshift">
            <h2>直播回看</h2>
            <p>
              來源支援依時間取源時，在播放回應或直播列表的 <code>Channel</code> 加入 <code>timeshift</code>。
              App 會顯示可拖曳的時間軸，最右側為直播。
            </p>
            <CodeBlock label="JSON / Result" path="playerContent">{`{
  "parse": 0,
  "url": "https://example.com/live.flv",
  "timeshift": {
    "url": "https://example.com/replay?time={time}",
    "start": 1792700000000,
    "window": 7200000
  }
}`}</CodeBlock>
            <p>
              <code>start</code> 是本場直播開始的 Unix 毫秒，<code>window</code> 是最多可回看的毫秒數。
              時間軸從本場開始累加，最長不超過 window；可用範圍由來源決定。
            </p>
            <p>
              App 將網址的 <code>{"{time}"}</code> 換成目標 Unix 毫秒後 GET；0 表示重新取得直播來源。
              端點回傳直接播放的 <code>url</code>，並可附上 <code>header</code> 與 <code>format</code>。
              使用本地代理時，以 <code>getProxyUrl</code> 產生網址。
            </p>
            <p>
              連續拖曳或切台會取消舊請求；失敗時提示並保留目前播放。
              回看期間暫停爬蟲即時彈幕，回到直播後恢復。
              從節目表選取回看則使用 <Link className="inline-link" href="/config#catchup"><code>catchup</code></Link>，時間軸以該節目為範圍。
            </p>
          </section>
          <section id="skips">
            <h2>片段跳過</h2>
            <p>
              在每集的 <code>playerContent</code> 回應頂層加入 <code>skips</code>：
            </p>
            <CodeBlock label="JSON / Result" path="playerContent">{`{
  "parse": 0,
  "url": "https://example.com/video.m3u8",
  "skips": [
    {"type": "opening", "start": 0, "end": 90000},
    {"type": "middle", "start": 600000, "end": 630000},
    {"type": "ending", "start": 1200000}
  ]
}`}</CodeBlock>
            <p>
              <code>opening</code> 為片頭、<code>middle</code> 為片中、<code>ending</code> 為片尾。
              時間單位是毫秒；<code>start</code> 是開始跳過的時間，<code>end</code> 是要跳到的時間。
              上例片中會從 10:00 跳到 10:30。
            </p>
            <p>
              片頭與片中必填 end，且須晚於 start；片中可提供多段。
              片尾可在 start 大於 0 時省略 end，表示換下一集，沒有下一集則跳到片尾。
            </p>
            <p>
              使用者手動設定的片頭或片尾會優先套用；片中跳過不受影響。
            </p>
          </section>
          <section id="web">
            <h2>開網頁</h2>
            <p>
              卡片的 <code>action</code> 觸發後，在回應頂層提供 <code>web</code>。
            </p>
            <div className="schema-grid">
              <article>
                <h3>一般網頁</h3>
                <CodeBlock label="JSON / Result" path="action">{`{
  "web": {
    "url": "https://example.com/"
  }
}`}</CodeBlock>
              </article>
              <article>
                <h3>獨立內容視窗</h3>
                <CodeBlock label="JSON / Result" path="action">{`{
  "web": {
    "url": "https://example.com/game",
    "mode": "view"
  }
}`}</CodeBlock>
              </article>
            </div>
            <p>
              <code>url</code> 必填，只接受 HTTP 或 HTTPS。<code>mode</code> 省略或填{" "}
              <code>browser</code> 為一般網頁；<code>view</code> 為獨立內容視窗，
              不啟動外部 App，也不會自動全螢幕。兩者返回都先退出全螢幕，
              再依網頁歷史回上一頁；沒有歷史才關閉。
            </p>
            <h3>登入後自動關閉</h3>
            <CodeBlock label="JSON / Result" path="action">{`{
  "web": {
    "url": "https://example.com/login",
    "cookie": [
      {"keys": ["sid", "token"]},
      {"url": "https://login.example.com/", "keys": ["auth"]}
    ]
  }
}`}</CodeBlock>
            <p>
              <code>cookie</code> 為選填陣列，每組 url 省略時使用開啟網址。同組 keys 必須全部非空，
              不同組任一組完成即可；至少一個指定 Cookie 比開啟前改變，且目前頁面與該組的
              scheme、host、port 相同，才自動關閉。此條件不代表站點已驗證帳號授權。
            </p>
          </section>
          <section id="node">
            <h2>Node.js 載入</h2>
            <p>
              一般 <code>./demo.js</code> 仍由 QuickJS 執行。Node.js 必須以{" "}
              <code>index.js.md5</code> 作為配置入口，同目錄提供{" "}
              <code>index.js</code>、<code>index.config.js</code> 與{" "}
              <code>index.config.js.md5</code>。
            </p>
            <p>
              App 啟動 bundle 後，讀取 <code>/config</code> 回應的{" "}
              <code>video</code> 或 <code>data.video</code>，把{" "}
              <code>video.sites</code> 映射為 <code>node:</code>{" "}
              站點路由。爬蟲以 HTTP JSON 處理接在各站點路由後的{" "}
              <code>/init</code>、<code>/home</code>、<code>/category</code>、
              <code>/detail</code>、<code>/search</code> 與 <code>/play</code>。
            </p>
            <p>
              <code>/play</code> 的詳情回傳格式見 <Link className="inline-link" href="#playback-details">播放詳情更新</Link>。
            </p>
            <div className="callout">
              <b>不要只替換 api 前綴</b>Node.js 不是 QuickJS 的 export default
              物件模式；單獨把普通配置的 api 改成 node:，不會完成 Node bundle
              初始化。
            </div>
          </section>
        </div>
      </div>
    </DocsLayout>
  );
}
