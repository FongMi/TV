import type { Metadata } from "next";
import { DocsNav } from "../DocsNav";
import Link from "next/link";
import { CodeTabs, type CodeSample } from "../CodeTabs";
import { CodeBlock, DocsLayout } from "../ui";
import { FieldTable } from "../FieldTable";
import { resultFields, objectGroups, methods, networkMethods } from "../spider-fields";

export const metadata: Metadata = {
  title: "爬蟲介接",
  description: "Java、Python、JavaScript 爬蟲 Demo、生命週期與回傳資料格式。",
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
        return new JSONObject().put("parse", 0).put("jx", 0).put("url", url).toString();
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
  "skips": [
    { "type": "opening", "start": 0, "end": 90000 },
    { "type": "middle", "start": 600000, "end": 630000 },
    { "type": "middle", "start": 850000, "end": 880000 },
    { "type": "middle", "start": 1000000, "end": 1020000 },
    { "type": "ending", "start": 1200000, "end": 1260000 }
  ],
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
  { id: "javascript", label: "JavaScript", file: "demo.js", code: javascript },
];

export default function SpiderPage() {
  return (
    <DocsLayout
      title="爬蟲介接指南"
      lead="從 Java、Python、JavaScript 範例開始，了解載入方式與回傳格式。"
    >
      <div className="docs-layout wrap">
        <DocsNav items={[
          ["start", "載入方式"],
          ["modules", "安裝執行模組"],
          ["testing", "爬蟲測試"],
          ["demo", "三語言 Demo"],
          ["network", "共用網路"],
          ["runtime", "Runtime 與 Android UI"],
          ["node", "Node.js"],
          ["lifecycle", "方法生命週期"],
          ["result", "Result 結構"],
          ["skips", "片段跳過"],
          ["web", "開網頁"],
          ["objects", "資料物件"],
          ["episodes", "集數格式"],
          ["proxy", "Proxy 回傳"],
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
                <strong>JavaScript · QuickJS</strong>
                <code>{'api: "./demo.js"'}</code>
                <p>
                  相對路徑以配置檔所在位置為基準；ES module
                  預設匯出物件或建立物件的函式。
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
              Node.js 請依下方的獨立載入方式設定。
            </p>
          </section>
          <section id="testing">
            <h2>測試爬蟲</h2>
            <p>
              連線 App 的網頁，從「設定 → 爬蟲測試」開啟 <code>/spider.html</code>。
              Python 與 QuickJS 需先安裝對應模組。
            </p>
            <ol>
              <li>載入 .py／.js，ext 為選填；Java 載入編譯好的 JAR，再選擇類別。</li>
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
              CatVodSpider 自行維護 <code>com.github.catvod.bean</code> 資料模型及{" "}
              <code>Response</code> 組裝工具，隨爬蟲 JAR 打包，不在 SDK 內。
              完整清單分頁可用 <code>Response.page</code>、<code>self.page</code>{" "}
              或 <code>page</code>。
              直播可回傳 Group 陣列或 TXT／M3U 原始文字。
            </div>
            <CodeTabs samples={samples} />
            <p>
              CatVodSpider 的完整範例涵蓋分類樣式、篩選、搜尋分頁、直播、快取與網頁操作：
              Java 的 <code>app/src/main/java/com/github/catvod/spider/Demo.java</code>
              與同目錄 <code>DemoData.java</code>、
              Python 的 <code>examples/python/spider.py</code> 與 QuickJS 的{" "}
              <code>examples/javascript/spider.js</code>。
            </p>
          </section>
          <section id="network">
            <h2>共用網路</h2>
            <p>
              App 在初始化前提供網路接口：Java／QuickJS 使用 <code>net</code>，
              Python 使用 <code>self.net</code>。QuickJS 的 <code>net.req()</code> 同步執行；
              <code>net.http()</code> 與 <code>net.json()</code> 使用 <code>await</code> 取得結果。
            </p>
            <p>
              Java 的 options 與 <code>net.json()</code> 結果都是 JSON 字串。
              Python／QuickJS 的 options 是物件，<code>net.json()</code> 結果為 JSON 值。
            </p>
            <p>
              <code>net.connect(url, options, callback)</code> 的回呼接收{" "}
              <code>(socket, event)</code>，事件為 open／message／error／close。
              訊息的 <code>content</code> 是文字，或 Python bytes／QuickJS Uint8Array／Java byte[]。
              使用 <code>socket.send()</code> 發送訊息，<code>socket.close()</code> 關閉連線。
            </p>
            <p>
              站點或 Session 釋放時也會取消連線。<code>timeout</code> 只限制建立連線，
              <code>ping</code> 是協定 Ping 的毫秒間隔，平台訂閱與應用層心跳由爬蟲處理。
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
            <p>
              <code>cookie: true</code> 使用 WebView Cookie；隔離 Session 用完須關閉。
              快取 options 使用 <code>ttl</code>、<code>stale</code>、<code>persist</code>，
              播放網址使用 <code>stale: false</code>。時間單位為毫秒。
              完整參數與三語言範例見 CatVodSpider 的 <code>docs/development.md</code>。
            </p>
            <p>
              下載 options 可指定 <code>threads</code>：0 為自動，1 為單連線，2–4 限制並行數。
              已知總大小可用正整數 <code>size</code> 傳入，單位為位元組；
              下載器仍會按來源的 Range 支援與驗證資訊決定是否接續或分塊。
            </p>
            <p>
              Python 的 <code>self.net.cachedText(key, options, loader)</code> 直接取得快取的 JSON 字串，
              適合需要保留原文的流程；<code>self.net.cached()</code> 則解碼為 Python 值。
              兩者沿用同一套 TTL 與清除規則。
            </p>
            <p>
              小型設定用 Local：Java／QuickJS 使用 <code>local</code>，Python 使用{" "}
              <code>self.local</code>。以 <code>get(key)</code>、<code>set(key, value)</code> 與{" "}
              <code>delete(key)</code> 讀寫，由 App 按站點隔離。Java 使用 JSON 字串，
              Python／QuickJS 直接讀寫 JSON 值。
            </p>
          </section>
          <section id="runtime">
            <h2>Runtime 與 Android UI</h2>
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
          <section id="node">
            <h2>Node.js 使用獨立的載入契約</h2>
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
            <div className="callout">
              <b>不要只替換 api 前綴</b>Node.js 不是 QuickJS 的 export default
              物件模式；單獨把普通配置的 api 改成 node:，不會完成 Node bundle
              初始化。
            </div>
          </section>
          <section id="lifecycle">
            <h2>方法與呼叫時機</h2>
            <p>
              本表適用 Java、Python 與 QuickJS；Node.js 請使用上方的 HTTP 端點。
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
          <section id="result">
            <h2>Result 通用回傳格式</h2>
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
              ；詳情的播放分組見下方集數格式。
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
          <section id="objects">
            <h2>常用資料物件</h2>
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
            <h2>播放分組與集數字串</h2>
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
            <h2>Proxy 回傳：依執行環境區分</h2>
            <CodeBlock label="JSON / Proxy 陣列" path="proxy / localProxy">{`[
  200,
  "text/plain; charset=utf-8",
  "demo response",
  { "Cache-Control": "no-cache" }
]`}</CodeBlock>
            <p>
              上例是 QuickJS / Python
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
        </div>
      </div>
    </DocsLayout>
  );
}
