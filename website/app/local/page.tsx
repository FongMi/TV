import type { Metadata } from "next";
import Link from "next/link";
import { CodeBlock, DocsLayout } from "../ui";
import { FieldTable } from "../FieldTable";
import {
  localSections,
  localActions,
  localControls,
  localRefresh,
  localFileEndpoints,
  localCacheActions,
  localMediaFields,
  localDeviceFields,
  localExamples,
} from "../local-api";

export const metadata: Metadata = {
  title: "本地 API",
  description:
    "影視TV 本地 HTTP API：播放控制、裝置資訊、檔案管理、快取與同步的參數及範例。",
};

function ReferenceTable({
  label,
  columns,
  rows,
}: {
  label: string;
  columns: readonly string[];
  rows: readonly (readonly string[])[];
}) {
  return (
    <div className="table-scroll">
      <table className="method-table" aria-label={label}>
        <thead>
          <tr>
            {columns.map((column) => (
              <th scope="col" key={column}>
                {column}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row[0]}>
              {row.map((value, index) => (
                <td key={columns[index]} data-label={columns[index]}>
                  {index === 0 ? <code>{value}</code> : value}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function LocalPage() {
  return (
    <DocsLayout
      title="本地 HTTP API"
      lead="透過 App 內建的 HTTP 服務，查詢播放狀態、推送內容、管理檔案與交換裝置資料。以下按目前程式碼核對，不是 GitHub Pages 網站提供的 API。"
    >
      <div className="docs-layout wrap">
        <aside className="docs-nav">
          <b>本頁導覽</b>
          {localSections.map(([id, title]) => (
            <a href={`#${id}`} key={id}>
              {title}
            </a>
          ))}
        </aside>
        <div className="docs-content">
          <section id="connection">
            <h2>先確認連線位址</h2>
            <p>
              服務從埠號 <code>9978</code> 依序嘗試到 <code>9998</code>
              ，使用第一個成功啟動的埠。請先啟動 App，依 App
              顯示的實際位址連線。
            </p>
            <div className="callout">
              <b>127.0.0.1 指的是發出請求的裝置</b>
              <span>
                下方範例以 App 裝置本機為例。若從電腦或另一台手機操作，請把{" "}
                <code>127.0.0.1:9978</code> 換成 App 的區域網路 IP
                與埠號。範例中的 example.com 與 192.0.2.10 僅作結構示意。
              </span>
            </div>
            <CodeBlock
              label="HTTP"
              path="連線測試"
            >{`curl "http://127.0.0.1:9978/device"`}</CodeBlock>
            <p>
              一般參數可放在 Query String；POST 表單使用{" "}
              <code>application/x-www-form-urlencoded</code>。中文、URL 與 JSON
              字串需 URL 編碼，範例使用 curl 的 <code>--data-urlencode</code>{" "}
              處理。不要把整包 JSON body 當作一般表單參數。
            </p>
            <p>
              多數文字回應使用 <code>text/plain</code>，包括內容為 JSON 的{" "}
              <code>/media</code>、<code>/device</code>{" "}
              與目錄列表。檔案、圖片、解析頁及代理各有自己的
              Content-Type；不能統一假設全部為 application/json。
            </p>
            <div className="callout">
              <b>OK 不等於動作已完成</b>
              <span>
                <code>/action</code> 通常先派送事件或排入主執行緒再回傳
                OK；未知指令、缺少必要內容或沒有播放服務，也可能回傳 OK
                而未執行。這些端點沒有通用 token
                驗證，請只在可信任的區域網路使用，不要直接開放到公網。
              </span>
            </div>
          </section>

          <section id="action">
            <h2>/action：動作指令</h2>
            <p>
              使用 GET 或表單 POST，透過 <code>do</code>{" "}
              選擇操作。下列參數都屬於同一次請求。
            </p>
            <ReferenceTable
              label="動作參數"
              columns={["do", "參數", "用途"]}
              rows={localActions}
            />
            <h3>播放控制與即時彈幕</h3>
            <ReferenceTable
              label="播放控制指令"
              columns={["type", "用途"]}
              rows={localControls}
            />
            <CodeBlock
              label="HTTP"
              path="播放控制"
            >{`curl -G "http://127.0.0.1:9978/action" --data-urlencode "do=control" --data-urlencode "type=pause"

curl -G "http://127.0.0.1:9978/action" --data-urlencode "do=danmaku" --data-urlencode "text=範例彈幕"`}</CodeBlock>
            <h3>重新整理與推送資料</h3>
            <ReferenceTable
              label="刷新類型"
              columns={["type", "額外參數", "用途"]}
              rows={localRefresh}
            />
            <CodeBlock
              label="HTTP"
              path="刷新範例"
            >{`curl -G "http://127.0.0.1:9978/action" --data-urlencode "do=refresh" --data-urlencode "type=category"

curl -G "http://127.0.0.1:9978/action" --data-urlencode "do=refresh" --data-urlencode "type=subtitle" --data-urlencode "path=https://example.com/subtitle.srt"

curl "http://127.0.0.1:9978/action?do=refresh&type=vod" --data-urlencode 'json={"vod_id":"demo-1","vod_name":"範例項目"}'`}</CodeBlock>
            <h3>播放、搜尋、配置與檔案</h3>
            <CodeBlock
              label="HTTP"
              path="推送與設定"
            >{`curl -G "http://127.0.0.1:9978/action" --data-urlencode "do=push" --data-urlencode "url=https://example.com/video.m3u8"

curl -G "http://127.0.0.1:9978/action" --data-urlencode "do=search" --data-urlencode "word=範例"

curl -G "http://127.0.0.1:9978/action" --data-urlencode "do=setting" --data-urlencode "text=https://example.com/vod.json" --data-urlencode "name=示意配置"

curl -G "http://127.0.0.1:9978/action" --data-urlencode "do=file" --data-urlencode "path=/TV/subtitle.srt"`}</CodeBlock>
            <p>
              <code>do=file</code> 依小寫副檔名分派：<code>.apk</code>{" "}
              交給安裝流程；<code>.srt</code>、<code>.ssa</code>、
              <code>.ass</code>{" "}
              交給字幕流程；其他交給設定流程。它使用裝置檔案位置，不會把電腦上的檔案上傳。
            </p>
            <h3>投放既有觀看紀錄</h3>
            <p>
              <code>config</code> 是 App 儲存的 Config 記錄，不是含 sites /
              lives 的完整配置；<code>device</code> 可由目標裝置的 /device
              取得；<code>history</code> 應沿用既有 History
              記錄。以下只示範表單編碼，key、設定位置與裝置位址需換成實際資料。
            </p>
            <CodeBlock
              label="HTTP"
              path="投放表單"
            >{`curl "http://127.0.0.1:9978/action?do=cast" --data-urlencode 'config={"type":0,"url":"https://example.com/vod.json","name":"示意配置"}' --data-urlencode 'device={"uuid":"demo-android-id","name":"示意電視","ip":"http://192.0.2.10:9978","type":0}' --data-urlencode 'history={"key":"demo@@@demo-1@@@7","vodName":"範例項目","vodFlag":"Demo","vodRemarks":"第 1 集","episodeUrl":"demo://episode/1","position":120000}'`}</CodeBlock>
          </section>

          <section id="sync">
            <h2>/action?do=sync：裝置同步</h2>
            <p>
              建議以表單 POST 傳送 JSON 字串。mode 的方向以「收到本次請求的
              App」為準，HTTP OK 並不是配置載入或資料合併完成的確認。
            </p>
            <ReferenceTable
              label="同步參數"
              columns={["參數", "用途"]}
              rows={[
                ["type", "history＝觀看紀錄；keep＝收藏。"],
                ["mode", "0＝傳送並接收（預設）；1＝只接收；2＝只傳送。"],
                [
                  "device",
                  "mode 0 / 2 傳送時使用的目標 Device 物件字串；沒有 device 就不主動傳送。",
                ],
                ["force", "字串 true 才啟用先刪後合併；未填或其他值不先刪除。"],
                [
                  "config",
                  "history 使用的 Config 物件字串；接收時需要有效 url，可能先載入該配置。",
                ],
                [
                  "targets",
                  "History[] 或 Keep[] 的 JSON 字串；型別依 type 決定。",
                ],
                [
                  "configs",
                  "keep 使用的 Config[] 物件陣列字串，不是 URL 字串陣列；以來源 id 對應 Keep.cid。",
                ],
              ]}
            />
            <div className="callout">
              <b>force=true 會刪除既有資料</b>
              <span>
                history 先刪除對應配置的觀看紀錄；keep 呼叫
                Keep.deleteAll()。一般同步請省略 force 或傳 false，不要使用空
                targets 測試清除流程。
              </span>
            </div>
            <h3>收藏同步的資料關係</h3>
            <CodeBlock label="JSON / Sync" path="兩個表單欄位的解碼內容">
              {JSON.stringify(localExamples.sync, null, 2)}
            </CodeBlock>
            <p>
              上例不是可直接提交的 JSON body。需要將 <code>configs</code> 與{" "}
              <code>targets</code> 各自序列化成表單字串；來源 Config.id 為
              7，因此 Keep.cid 也為 7。App 合併時會重新對應本機配置 ID。
            </p>
            <CodeBlock
              label="HTTP"
              path="只接收收藏，不先刪除"
            >{`curl "http://127.0.0.1:9978/action?do=sync&type=keep&mode=1&force=false" --data-urlencode 'configs=${JSON.stringify(localExamples.sync.configs)}' --data-urlencode 'targets=${JSON.stringify(localExamples.sync.targets)}'`}</CodeBlock>
            <p>
              觀看紀錄使用 <code>type=history</code>、單一 <code>config</code>{" "}
              與 History 陣列；合併會比較同名項目的
              createTime，不是盲目覆寫全部紀錄。同步可能載入另一份配置，請先備份個人資料。
            </p>
          </section>

          <section id="media">
            <h2>/media：播放狀態</h2>
            <CodeBlock
              label="HTTP"
              path="查詢播放狀態"
            >{`curl "http://127.0.0.1:9978/media"`}</CodeBlock>
            <p>
              沒有播放服務、播放器已釋放，或讀取失敗時回傳 <code>{"{}"}</code>
              。有資料時回傳以下結構；Content-Type 是
              text/plain，客戶端需自行解析 JSON。
            </p>
            <CodeBlock label="JSON / Media" path="/media 回應">
              {JSON.stringify(localExamples.media, null, 2)}
            </CodeBlock>
            <FieldTable label="Media" fields={localMediaFields} />
          </section>

          <section id="device">
            <h2>/device：裝置資訊</h2>
            <CodeBlock label="JSON / Device" path="/device 回應">
              {JSON.stringify(localExamples.device, null, 2)}
            </CodeBlock>
            <p>
              內容為 JSON 字串，Content-Type 是 text/plain。不要把裝置識別碼或
              MAC 位址填入公開範例；下方只列欄位用途。
            </p>
            <FieldTable label="Device" fields={localDeviceFields} />
          </section>

          <section id="files">
            <h2>檔案與目錄管理</h2>
            <p>
              一般相對路徑以 Android 共用外部儲存根目錄 <code>Path.root()</code>{" "}
              為基準，不是 App 的私有 data 目錄；實際存取仍受系統儲存權限限制。
            </p>
            <ReferenceTable
              label="檔案端點"
              columns={["端點", "使用方式", "參數與行為"]}
              rows={localFileEndpoints}
            />
            <CodeBlock
              label="HTTP"
              path="列目錄與建立資料夾"
            >{`curl "http://127.0.0.1:9978/file/TV"

curl -G "http://127.0.0.1:9978/newFolder" --data-urlencode "path=TV" --data-urlencode "name=demo-empty"`}</CodeBlock>
            <CodeBlock label="JSON / Folder" path="瀏覽 /file/TV 的示意回應">
              {JSON.stringify(localExamples.folder, null, 2)}
            </CodeBlock>
            <ReferenceTable
              label="目錄回應欄位"
              columns={["欄位", "用途"]}
              rows={[
                [
                  "parent",
                  "位於根目錄時為 .；上一層是根目錄時為空字串；其他回傳上一層相對路徑。",
                ],
                ["files[].name", "檔案或目錄名稱。"],
                [
                  "files[].path",
                  "根目錄內的路徑通常帶前導 /，例如 /TV/demo.mp4。",
                ],
                [
                  "files[].time",
                  "最後修改時間，以裝置時區格式化為 yyyy/MM/dd HH:mm:ss。",
                ],
                ["files[].dir", "1＝目錄；0＝一般檔案。"],
              ]}
            />
            <h3>下載、Range 與快取</h3>
            <p>
              一般下載回傳 200；單一有效 Range 回傳
              206，支援起迄、開放結尾及尾端位元組範圍。多段或無效範圍回傳
              416；If-None-Match 命中 ETag 時回傳 304，If-Range
              不符時改回完整檔案。
            </p>
            <CodeBlock
              label="HTTP"
              path="讀取前 1 KiB"
            >{`curl "http://127.0.0.1:9978/file/TV/demo.mp4" -H "Range: bytes=0-1023" --output demo-part.bin`}</CodeBlock>
            <h3>上傳與刪除</h3>
            <CodeBlock
              label="HTTP"
              path="上傳至已存在的 TV 目錄"
            >{`curl "http://127.0.0.1:9978/upload?path=TV" -F "file=@./subtitle.srt"`}</CodeBlock>
            <p>
              multipart 欄位名稱可自訂，上例使用
              file；檔名由上傳欄位提供，不能是空字串、.、..
              或包含路徑分隔符。ZIP
              會解壓到指定目錄，其他檔案使用原子複製；缺少檔案、目錄不存在或操作失敗會回傳
              500 與文字訊息。
            </p>
            <div className="callout">
              <b>刪除端點會直接刪除資料</b>
              <span>
                /delFile 與 /delFolder
                目前使用同一段遞迴刪除程式；不要依端點名稱假設目錄不會被刪除。下例只針對先前建立的示意目錄，執行前務必確認內容。
              </span>
            </div>
            <CodeBlock
              label="HTTP"
              path="刪除示意目錄"
            >{`curl -G "http://127.0.0.1:9978/delFolder" --data-urlencode "path=TV/demo-empty"`}</CodeBlock>
            <p>
              上傳、新建與刪除都限制在共用儲存根目錄內，禁止刪除根目錄本身。/file
              的區網請求也限制於此範圍；本機 loopback 請求可由 Path.local
              解析其他可存取路徑。
            </p>
          </section>

          <section id="cache">
            <h2>/cache：鍵值快取</h2>
            <p>
              使用 GET 或表單 POST。實際儲存鍵為{" "}
              <code>{'"cache_" + (rule 為空 ? "" : rule + "_") + key'}</code>
              ；rule 可用來隔離不同爬蟲的名稱空間。
            </p>
            <ReferenceTable
              label="快取操作"
              columns={["do", "參數", "回應"]}
              rows={localCacheActions}
            />
            <CodeBlock
              label="HTTP"
              path="寫入、讀取與移除"
            >{`curl -G "http://127.0.0.1:9978/cache" --data-urlencode "do=set" --data-urlencode "rule=demo" --data-urlencode "key=greeting" --data-urlencode "value=Hello"

curl "http://127.0.0.1:9978/cache?do=get&rule=demo&key=greeting"

curl "http://127.0.0.1:9978/cache?do=del&rule=demo&key=greeting"`}</CodeBlock>
          </section>

          <section id="internal">
            <h2>代理與 App 內部端點</h2>
            <ReferenceTable
              label="內部端點"
              columns={["端點", "參數與行為"]}
              rows={[
                [
                  "/proxy",
                  "把 Query / 表單參數、請求標頭、parseBody 產生的 files 依序合併後交給 BaseLoader.proxy；後加入的同名鍵會覆蓋前者。",
                ],
                [
                  "/parse",
                  "jxs 為以分號分隔的解析前綴；url 為目標網址。parse.html 會以每個前綴 + url 建立 iframe，回傳 text/html。",
                ],
                [
                  "/image/{key}",
                  "讀取 ImgUtil 已快取的圖片資料，使用圖片本身的 MIME；找不到 key 時回傳 404。不是任意網址的圖片下載 API。",
                ],
                [
                  "/tvbus",
                  "回傳目前直播配置 Core 的 RESP 字串，不是頻道列表。",
                ],
                [
                  "/",
                  "回傳 App 內建的 index.html 控制頁；其他未匹配路徑會嘗試讀取內建資源，不存在則回傳 404。",
                ],
              ]}
            />
            <p>
              /proxy 的 POST body 不是一律直接攤平成參數：一般表單進入
              parms，multipart 會提供暫存檔資訊，原始 body 依 NanoHTTPD 的
              parseBody
              結果傳遞。爬蟲回應決定狀態、Content-Type、串流與標頭；無效回應或例外會回傳
              500。
            </p>
            <CodeBlock
              label="HTTP"
              path="解析頁範例"
            >{`curl -G "http://127.0.0.1:9978/parse" --data-urlencode "jxs=https://example.com/parse?url=" --data-urlencode "url=https://example.com/watch/demo-1"`}</CodeBlock>
            <p>
              代理回傳格式與各語言差異請見{" "}
              <Link className="inline-link" href="/spider#proxy">
                Spider Proxy 回傳
              </Link>
              ；RESP 欄位請見{" "}
              <Link className="inline-link" href="/config#core">
                Core 配置
              </Link>
              。
            </p>
          </section>
        </div>
      </div>
    </DocsLayout>
  );
}
