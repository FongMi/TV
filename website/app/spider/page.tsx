import type { Metadata } from "next";
import Link from "next/link";
import { CodeTabs, type CodeSample } from "../CodeTabs";
import { CodeBlock, DocsLayout } from "../ui";
import { FieldTable } from "../FieldTable";
import { resultFields, objectGroups, methods } from "../spider-fields";

export const metadata: Metadata = {
  title: "爬蟲介接",
  description: "Java、Python、JavaScript 爬蟲 Demo、生命週期與回傳資料格式。",
};

const java = `package com.github.catvod.spider;

import android.content.Context;
import com.github.catvod.crawler.Spider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.List;

public class Demo extends Spider {
    private String ext;

    @Override
    public void init(Context context, String extend) {
        this.ext = extend;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        JSONArray classes = new JSONArray()
            .put(new JSONObject().put("type_id", "movie")
            .put("type_name", "電影"));
        return new JSONObject().put("class", classes).toString();
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter,
            HashMap<String, String> extend) throws Exception {
        JSONArray list = new JSONArray().put(card("demo-1", "範例項目"));
        return new JSONObject().put("list", list)
            .put("pagecount", 1).toString();
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        JSONObject item = card(ids.get(0), "範例項目")
            .put("vod_play_from", "Demo")
            .put("vod_play_url", "第 1 集$demo://episode/1");
        return new JSONObject().put("list",
            new JSONArray().put(item)).toString();
    }

    @Override
    public String playerContent(String flag, String id,
            List<String> vipFlags) throws Exception {
        return new JSONObject().put("parse", 0)
            .put("url", resolve(id)).toString();
    }

    private JSONObject card(String id, String name) throws Exception {
        return new JSONObject().put("vod_id", id).put("vod_name", name)
            .put("vod_pic", "https://example.com/poster.jpg");
    }

    private String resolve(String id) {
        return id.replace("demo://", "https://example.com/");
    }
}`;

const python = `# demo.py
from base.spider import Spider as BaseSpider

class Spider(BaseSpider):
    def init(self, extend=""):
        self.extend = extend

    def homeContent(self, filter):
        return {"class": [
            {"type_id": "movie", "type_name": "電影"}
        ]}

    def categoryContent(self, tid, pg, filter, extend):
        return {
            "list": [self.card("demo-1", "範例項目")],
            "pagecount": 1
        }

    def detailContent(self, ids):
        item = self.card(ids[0], "範例項目")
        item.update({
            "vod_play_from": "Demo",
            "vod_play_url": "第 1 集$demo://episode/1"
        })
        return {"list": [item]}

    def playerContent(self, flag, id, vipFlags):
        return {"parse": 0, "url": self.resolve(id)}

    def card(self, id, name):
        return {"vod_id": id, "vod_name": name,
                "vod_pic": "https://example.com/poster.jpg"}

    def resolve(self, id):
        return id.replace("demo://", "https://example.com/")`;

const javascript = `// demo.js — QuickJS ES module
const card = (id, name) => ({
  vod_id: id,
  vod_name: name,
  vod_pic: "https://example.com/poster.jpg"
});

const resolve = (id) => id.replace("demo://", "https://example.com/");

export default {
  init(ext) { this.ext = ext; },

  home(filter) {
    return JSON.stringify({ class: [
      { type_id: "movie", type_name: "電影" }
    ]});
  },

  category(tid, pg, filter, extend) {
    return JSON.stringify({
      list: [card("demo-1", "範例項目")],
      pagecount: 1
    });
  },

  detail(id) {
    return JSON.stringify({ list: [{
      ...card(id, "範例項目"),
      vod_play_from: "Demo",
      vod_play_url: "第 1 集$demo://episode/1"
    }]});
  },

  play(flag, id, vipFlags) {
    return JSON.stringify({ parse: 0, url: resolve(id) });
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
  { id: "javascript", label: "JavaScript", file: "demo.js", code: javascript },
];

export default function SpiderPage() {
  return (
    <DocsLayout
      title="爬蟲介接指南"
      lead="從 Java、Python、JavaScript 範例開始，了解載入方式、方法呼叫與回傳資料。三種語言目前涵蓋 JAR、QuickJS、Chaquopy 與 Node.js 四種執行環境。"
    >
      <div className="docs-layout wrap">
        <aside className="docs-nav">
          <b>本頁導覽</b>
          <a href="#start">載入方式</a>
          <a href="#demo">三語言 Demo</a>
          <a href="#node">Node.js</a>
          <a href="#lifecycle">方法生命週期</a>
          <a href="#result">Result 結構</a>
          <a href="#objects">資料物件</a>
          <a href="#episodes">集數格式</a>
          <a href="#proxy">Proxy 回傳</a>
        </aside>
        <div className="docs-content">
          <section id="start">
            <h2>先讓 App 找到你的爬蟲</h2>
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
                  或站點的 <code>jar</code>。
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
          <section id="demo">
            <h2>可直接改寫的最小 Demo</h2>
            <p>
              三份範例示範「分類 → 列表 → 詳情 → 播放」資料流程。example.com
              是結構示意，並不是可用來源。正式實作時須接上自己的資料取得、錯誤處理與搜尋。
            </p>
            <div className="callout">
              <b>回傳型別依語言區分</b>Java 與 QuickJS 回傳 JSON 字串；Python
              直接回傳 dict，由 Chaquopy bridge 序列化，不要再呼叫
              json.dumps。直播方法回傳原始文字。
            </div>
            <CodeTabs samples={samples} />
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
              下表的方法名稱以 Java／Python 與 QuickJS 為主；Node.js 的 HTTP
              端點請依上方獨立段落，不是把所有方法名稱直接加在網址後。
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
          <section id="objects">
            <h2>常用資料物件</h2>
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
              代理請求帶 <code>siteKey</code> 時會交給對應 Spider；JAR 模式沒有
              siteKey 時，則找{" "}
              <code>com.github.catvod.spider.Proxy.proxy(Map)</code> 靜態入口。
            </p>
          </section>
        </div>
      </div>
    </DocsLayout>
  );
}
