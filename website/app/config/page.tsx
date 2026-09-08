import type { Metadata } from "next";
import { FieldTable } from "../FieldTable";
import {
  vodTop,
  siteFields,
  parseFields,
  liveFields,
  channelFields,
  commonFields,
  coreFields,
} from "../config-fields";
import { CodeBlock, DocsLayout } from "../ui";

export const metadata: Metadata = {
  title: "配置字典",
  description: "VodConfig、LiveConfig 與相關物件的完整欄位說明。",
};

const examples = {
  vod: `{
  "spider": "./spider.jar",
  "sites": [
    {
      "key": "java_demo",
      "name": "Java Demo",
      "type": 3,
      "api": "csp_Demo"
    },
    {
      "key": "js_demo",
      "name": "JavaScript Demo",
      "type": 3,
      "api": "./demo.js",
      "ext": { "region": "tw" }
    },
    {
      "key": "py_demo",
      "name": "Python Demo",
      "type": 3,
      "api": "./demo.py"
    }
  ],
  "parses": [
    {
      "name": "JSON 解析範例",
      "type": 1,
      "url": "https://example.com/parse?url=",
      "ext": {
        "flag": ["demo"],
        "header": { "User-Agent": "ExampleClient/1.0" }
      }
    }
  ]
}`,
  depot: `{
  "urls": [
    { "name": "同目錄配置", "url": "./vod.json" },
    { "name": "遠端配置", "url": "https://example.com/vod.json" }
  ]
}`,
  externalArrays: `{
  "doh": "./doh.json",
  "headers": [
    "./headers.json",
    {
      "host": "media.example.com",
      "header": { "Referer": "https://example.com/" }
    }
  ],
  "proxy": "./proxy.json",
  "rules": [
    "./rules.json",
    {
      "name": "媒體請求範例",
      "hosts": ["player.example.com"],
      "regex": [".*m3u8.*"],
      "exclude": [".*preview.*"]
    }
  ]
}`,
  site: `{
  "key": "js_demo",
  "name": "JavaScript Demo",
  "type": 3,
  "api": "./demo.js",
  "ext": { "region": "tw" },
  "searchable": 1,
  "changeable": 1,
  "categories": ["電影", "戲劇"],
  "header": { "User-Agent": "ExampleClient/1.0" },
  "style": { "type": "rect", "ratio": 1.33 }
}`,
  parse: `[
  {
    "name": "WebView 解析範例",
    "type": 0,
    "url": "https://example.com/web?url="
  },
  {
    "name": "JSON API 解析範例",
    "type": 1,
    "url": "https://example.com/api?url=",
    "ext": {
      "flag": ["demo"],
      "header": { "Referer": "https://example.com/" }
    }
  }
]`,
  liveExternal: `{
  "lives": [
    {
      "name": "M3U 範例",
      "url": "./live.m3u",
      "epg": "https://example.com/epg.xml",
      "logo": "https://example.com/logo/{id}.png",
      "ua": "ExampleClient/1.0",
      "timeZone": "Asia/Taipei",
      "boot": false
    }
  ]
}`,
  liveInline: `{
  "lives": [
    {
      "name": "內嵌 JSON 範例",
      "groups": [
        {
          "name": "新聞",
          "channel": [
            {
              "name": "範例一台",
              "number": "1",
              "tvgId": "demo-1",
              "urls": [
                "https://example.com/live/main.m3u8$主線",
                "https://example.com/live/backup.m3u8$備線"
              ],
              "header": { "Referer": "https://example.com/" }
            }
          ]
        }
      ]
    }
  ]
}`,
  jsonLive: `[
  {
    "name": "新聞",
    "channel": [
      {
        "name": "範例一台",
        "number": "1",
        "tvgId": "demo-1",
        "urls": [
          "https://example.com/live/main.m3u8$主線",
          "https://example.com/live/backup.m3u8$備線"
        ]
      }
    ]
  }
]`,
  m3u: `#EXTM3U url-tvg="https://example.com/epg.xml"
#EXTINF:-1 tvg-id="demo-1" tvg-name="範例一台" tvg-chno="1" tvg-logo="https://example.com/logo/demo-1.png" group-title="新聞",範例一台
#EXTVLCOPT:http-user-agent=ExampleClient/1.0
#EXTVLCOPT:http-referrer=https://example.com/
https://example.com/live/demo.m3u8`,
  txt: `新聞,#genre#
ua=ExampleClient/1.0
referer=https://example.com/
format=hls
範例一台,https://example.com/live/main.m3u8$主線#https://example.com/live/backup.m3u8$備線`,
  core: `{
  "name": "特殊核心示意",
  "url": "./live.m3u",
  "core": {
    "auth": "https://example.com/core/auth",
    "name": "demo",
    "pass": "demo-pass",
    "broker": "https://example.com/core/broker",
    "domain": "example.com",
    "so": "https://example.com/core/libcore.so",
    "option": []
  }
}`,
  shared: `{
  "doh": [
    {
      "name": "Example DoH",
      "url": "https://dns.example.com/dns-query",
      "ips": ["203.0.113.53"]
    }
  ],
  "proxy": [
    {
      "name": "本機代理範例",
      "hosts": ["media.example.com"],
      "urls": ["http://127.0.0.1:8080", "socks5://127.0.0.1:1080"]
    }
  ],
  "hosts": ["media.example.com=203.0.113.10"],
  "ads": ["ads.example.com"]
}`,
  catchup: `{
  "name": "時移示意",
  "url": "./live.m3u",
  "catchup": {
    "type": "append",
    "days": "7",
    "regex": "example.com/live/",
    "source": "?playseek=\${(b)yyyyMMddHHmmss}-\${(e)yyyyMMddHHmmss}",
    "replace": "live/,archive/"
  }
}`,
};

export default function ConfigPage() {
  return (
    <DocsLayout
      title="配置指南與欄位字典"
      lead="配置載入由 BaseConfig 統一處理；VodConfig 管理站點與解析器，LiveConfig 管理直播清單與頻道。以下依目前本地程式碼整理，並區分完整配置與物件片段。"
    >
      <div className="docs-layout wrap">
        <aside className="docs-nav">
          <b>本頁導覽</b>
          <a href="#examples">完整範例</a>
          <a href="#vod">Vod 頂層</a>
          <a href="#site">Site</a>
          <a href="#parse">Parse</a>
          <a href="#live">Live</a>
          <a href="#channel">Group / Channel</a>
          <a href="#formats">直播格式</a>
          <a href="#core">Core</a>
          <a href="#common">共用物件</a>
        </aside>
        <div className="docs-content">
          <section id="examples">
            <h2>先從可直接修改的配置開始</h2>
            <p>
              先選擇點播或直播的完整配置，再替換名稱、路徑與需要的欄位。後續各節另有標明層級的物件片段，需放入對應欄位。
            </p>
            <div className="callout">
              <b>範例位置說明</b>
              <span>
                主配置中的 <code>./</code> 與 <code>../</code>{" "}
                會以該配置檔的位置展開；外部陣列檔或直播列表內的路徑不會再次展開，請使用完整網址。
                <code>example.com</code> 與保留測試 IP
                只用來展示結構，並不是可用來源。
              </span>
            </div>
            <div className="schema-grid">
              <article>
                <h3>點播配置：Java、JS、Python</h3>
                <CodeBlock label="JSON / VodConfig" path="vod.json">
                  {examples.vod}
                </CodeBlock>
                <p>
                  Java 使用 <code>csp_</code> 類別名稱；JS 與 Python 檔案使用{" "}
                  <code>./</code> 相對路徑。
                </p>
              </article>
              <article>
                <h3>直播配置：外部 M3U</h3>
                <CodeBlock label="JSON / LiveConfig" path="live-config.json">
                  {examples.liveExternal}
                </CodeBlock>
                <p>
                  <code>url</code> 也可改成 <code>./live.txt</code> 或{" "}
                  <code>./live.json</code>；內容格式會由 LiveParser 判斷。
                </p>
              </article>
            </div>
          </section>
          <section id="vod">
            <h2>VodConfig 頂層欄位</h2>
            <p>
              主要配置入口。只有 <code>headers</code>、<code>proxy</code>、
              <code>rules</code>、<code>doh</code> 等經過 fetchArray
              的欄位，才支援物件陣列、外部位置或兩者混用。
            </p>
            <FieldTable fields={vodTop} label="VodConfig" />
            <div className="schema-grid">
              <article>
                <h3>配置倉庫 Depot</h3>
                <CodeBlock label="JSON / Depot" path="depot.json">
                  {examples.depot}
                </CodeBlock>
                <p>
                  頂層出現 <code>urls</code> 時，App
                  登記清單後會載入第一項配置，清單不可為空；每一項包含{" "}
                  <code>name</code> 與 <code>url</code>。
                </p>
              </article>
              <article>
                <h3>可引用外部陣列的欄位</h3>
                <CodeBlock label="JSON / arrays" path="VodConfig 頂層欄位片段">
                  {examples.externalArrays}
                </CodeBlock>
                <p>
                  整個欄位可以是一個位置，也可以在陣列中混用外部 JSON
                  位置與內嵌物件。外部檔案本身必須回傳 JSON 陣列。
                </p>
              </article>
            </div>
          </section>
          <section id="site">
            <h2>Site 點播站點</h2>
            <div className="type-strip">
              <span>
                <b>0</b>XML HTTP
              </span>
              <span>
                <b>1</b>JSON HTTP
              </span>
              <span>
                <b>3</b>JAR / JS / PY
              </span>
              <span>
                <b>4</b>HTTP 擴充
              </span>
            </div>
            <FieldTable fields={siteFields} label="Site" />
            <h3>Type 3 站點範例</h3>
            <CodeBlock label="JSON / Site" path="sites[] 物件片段">
              {examples.site}
            </CodeBlock>
            <p>
              改用 Python 時只需把 <code>api</code> 換成 <code>./demo.py</code>
              ；Java 類別則填 <code>csp_Demo</code>。相對路徑也適用於{" "}
              <code>ext</code> 與 <code>jar</code>。
            </p>
          </section>
          <section id="parse">
            <h2>Parse URL 處理規則</h2>
            <FieldTable fields={parseFields} label="Parse" />
            <h3>WebView 與 JSON API 範例</h3>
            <CodeBlock label="JSON / Parse" path="parses 欄位值">
              {examples.parse}
            </CodeBlock>
            <p>
              <code>parses</code> 的值是陣列；<code>ext.flag</code> 是 type 4
              並行解析的優先比對條件，不是全域白名單。<code>ext.header</code>{" "}
              也可能作為解析結果的預設播放標頭，詳見上表。
            </p>
          </section>
          <section id="live">
            <h2>LiveConfig 與直播來源</h2>
            <p>
              獨立 LiveConfig 的 JSON 頂層使用 <code>lives</code>，並支援{" "}
              <code>spider</code>、<code>headers</code>、<code>proxy</code>、
              <code>rules</code>、<code>hosts</code>、<code>ads</code>、
              <code>urls</code> 與 <code>msg</code>；不讀取獨立的{" "}
              <code>doh</code>。下表說明 <code>lives[]</code> 中的 Live
              來源物件。
            </p>
            <p>
              <code>url</code> 與 <code>groups</code> 通常擇一；
              <code>groups</code> 為空且提供 <code>api</code> 時，會先呼叫
              Spider 的 <code>liveContent(url)</code> 取得文字，再交由
              LiveParser 判斷格式。
            </p>
            <p>
              同名配置已保存的 <code>boot</code>、<code>pass</code> 與{" "}
              <code>keep</code> 可能覆蓋配置值；請同時檢查 App 內的已保存設定。
            </p>
            <FieldTable fields={liveFields} label="Live" />
            <div className="schema-grid">
              <article>
                <h3>載入外部列表</h3>
                <CodeBlock label="JSON / LiveConfig" path="live-config.json">
                  {examples.liveExternal}
                </CodeBlock>
                <p>
                  這是含 <code>lives</code> 陣列的完整 LiveConfig；
                  <code>url</code> 指向可獨立維護的 M3U、TXT 或 JSON 檔案。
                </p>
              </article>
              <article>
                <h3>直接內嵌 Group / Channel</h3>
                <CodeBlock label="JSON / LiveConfig" path="live-config.json">
                  {examples.liveInline}
                </CodeBlock>
                <p>
                  <code>$主線</code>、<code>$備線</code>{" "}
                  是線路標籤；同一頻道可以提供多個位置。直接內嵌 groups
                  不經外部列表的自動編號與預設值繼承，請在 Channel
                  明確設定需要的欄位。
                </p>
              </article>
            </div>
          </section>
          <section id="channel">
            <h2>分組與頻道欄位</h2>
            <FieldTable fields={channelFields} label="Group / Channel" />
            <p>
              若 JSON 是由 <code>Live.url</code> 載入，檔案最外層直接放 Group
              陣列，如下一節的 JSON 範例；若寫在主配置內，則放進{" "}
              <code>lives[].groups</code>。
            </p>
          </section>
          <section id="formats">
            <h2>直播列表的三種完整輸入</h2>
            <div className="schema-grid">
              <article>
                <h3>JSON</h3>
                <CodeBlock label="JSON / Group[]" path="live.json">
                  {examples.jsonLive}
                </CodeBlock>
                <p>最外層是 Group 陣列，適合完整保存頻道欄位與多線路。</p>
              </article>
              <article>
                <h3>M3U</h3>
                <CodeBlock label="M3U" path="live.m3u">
                  {examples.m3u}
                </CodeBlock>
                <p>
                  支援節目表、分組、頻道 ID、號碼、Logo、User-Agent 與 Referer。
                </p>
              </article>
              <article>
                <h3>TXT</h3>
                <CodeBlock label="TXT" path="live.txt">
                  {examples.txt}
                </CodeBlock>
                <p>
                  <code>#genre#</code> 建立分組；同一頻道的多個位置使用{" "}
                  <code>#</code> 分隔。
                </p>
              </article>
            </div>
            <div className="callout">
              <b>TXT / M3U 設定行</b>
              <code>
                ua= · parse= · click= · header= · format= · origin= · referer= ·
                forceKey=
              </code>
            </div>
          </section>
          <section id="core">
            <h2>特殊播放核心欄位</h2>
            <p>
              <code>Live.core</code>{" "}
              只在對應播放核心初始化時使用；一般直播配置不需要填寫。以下示範它在
              Live 物件中的位置；<code>option</code>{" "}
              的名稱與值由該核心實作定義，不是通用配置參數。
            </p>
            <FieldTable fields={coreFields} label="Core" />
            <CodeBlock label="JSON / Live.core" path="lives[] 物件片段">
              {examples.core}
            </CodeBlock>
          </section>
          <section id="common">
            <h2>網路、時移與顯示物件</h2>
            <FieldTable fields={commonFields} label="共用物件" />
            <div className="schema-grid">
              <article>
                <h3>DoH、Proxy、Hosts 與 Ads</h3>
                <CodeBlock label="JSON / shared" path="VodConfig 頂層欄位片段">
                  {examples.shared}
                </CodeBlock>
                <p>
                  代理位置必須包含 scheme、host 與 port；<code>hosts</code> 使用{" "}
                  <code>原主機=目標主機或 IP</code>。
                </p>
              </article>
              <article>
                <h3>追看 / 時移 Catchup</h3>
                <CodeBlock label="JSON / Live.catchup" path="lives[] 物件片段">
                  {examples.catchup}
                </CodeBlock>
                <p>
                  <code>{"${(b)yyyyMMddHHmmss}"}</code> 與{" "}
                  <code>{"${(e)yyyyMMddHHmmss}"}</code>{" "}
                  會分別替換為節目開始與結束時間，格式化使用裝置系統時區，不使用{" "}
                  <code>Live.timeZone</code>。
                </p>
              </article>
            </div>
          </section>
        </div>
      </div>
    </DocsLayout>
  );
}
