import type { Metadata } from "next";
import Link from "next/link";
import { CodeBlock, DocsLayout } from "../ui";
import { FieldTable } from "../FieldTable";
import { DocsNav } from "../DocsNav";
import {
  localSections,
  localActions,
  localControls,
  localRemoteControls,
  localRemoteFields,
  localRefresh,
  localFileEndpoints,
  localTransferEndpoints,
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
      lead="透過 App 的區域網路服務，控制播放、管理檔案與同步資料。"
    >
      <div className="docs-layout wrap">
        <DocsNav items={localSections} />
        <div className="docs-content">
          <section id="connection">
            <h2>先確認連線位址</h2>
            <p>
              啟動 App，使用畫面顯示的服務位址。埠號從 <code>9978</code> 到 <code>9998</code>，
              取第一個可用的埠。
            </p>
            <div className="callout">
              <b>從其他裝置連線</b>
              <span>
                <code>127.0.0.1</code> 指目前裝置。從電腦或另一台手機操作時，
                請換成 App 裝置的區域網路 IP 與埠號。範例網址與 IP 僅供示意。
              </span>
            </div>
            <CodeBlock
              label="HTTP"
              path="連線測試"
            >{`curl "http://127.0.0.1:9978/device"`}</CodeBlock>
            <p>
              一般參數放在 Query String 或 <code>application/x-www-form-urlencoded</code> 表單，
              不使用整包 JSON body。中文、URL 與 JSON 字串以 <code>--data-urlencode</code> 編碼。
            </p>
            <p>
              <code>/media</code>、<code>/device</code> 與目錄列表以 <code>text/plain</code> 回傳 JSON。
              其他回應請依實際 Content-Type 處理。
            </p>
            <div className="callout">
              <b>OK 不等於動作已完成</b>
              <span>
                <code>/action</code> 回傳 OK 只表示已收到請求；未知指令、缺少參數或沒有播放服務時，
                可能不執行。端點沒有通用 token 驗證，僅限可信任的區域網路使用。
              </span>
            </div>
          </section>

          <section id="action">
            <h2>/action：動作指令</h2>
            <p>
              使用 GET 或表單 POST，以 <code>do</code> 選擇操作。
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
              <code>config</code> 使用 App 儲存的 Config 記錄，<code>device</code> 取自目標裝置的
              <code>/device</code>，<code>history</code> 沿用既有 History 記錄。
              請將範例替換為實際資料，config 不可改填整份站點配置。
            </p>
            <CodeBlock
              label="HTTP"
              path="投放表單"
            >{`curl "http://127.0.0.1:9978/action?do=cast" --data-urlencode 'config={"type":0,"url":"https://example.com/vod.json","name":"示意配置"}' --data-urlencode 'device={"uuid":"demo-android-id","name":"示意電視","ip":"http://192.0.2.10:9978","type":0}' --data-urlencode 'history={"key":"demo@@@demo-1@@@7","vodName":"範例項目","vodFlag":"Demo","vodRemarks":"第 1 集","episodeUrl":"demo://episode/1","position":120000}'`}</CodeBlock>
          </section>

          <section id="sync">
            <h2>/action?do=sync：裝置同步</h2>
            <p>
              以表單 POST 傳送 JSON 字串。mode 的方向以收到請求的 App 為準；
              HTTP OK 不代表資料已合併完成。
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
                history 刪除對應配置的觀看紀錄；keep 刪除全部收藏。
                一般同步請省略 force 或填 false。
              </span>
            </div>
            <h3>收藏同步的資料關係</h3>
            <CodeBlock label="JSON / Sync" path="兩個表單欄位的解碼內容">
              {JSON.stringify(localExamples.sync, null, 2)}
            </CodeBlock>
            <p>
              將 <code>configs</code> 與 <code>targets</code> 分別轉為 JSON 表單字串。
              每筆 Keep.cid 須對應所屬的 Config.id，合併時會轉為本機配置 ID。
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

          <section id="remote">
            <h2>網頁播放與遙控器</h2>
            <p>
              在瀏覽器開啟 App 的服務網址。「播放」頁顯示封面、標題與進度，
              可控制播放、切換集數或頻道、單集循環、跳轉與倍速。
              預設開啟「搜尋」頁，搜尋與網址推送各自保留輸入內容及瀏覽器紀錄。
            </p>
            <p>
              拖曳進度滑桿預覽時間，放手後跳轉；方向鍵每次調整五秒。
              直播隱藏進度、循環與倍速控制，不能跳轉的影片停用滑桿。
              網頁提供 0.5×、1.0×、2.0×、3.0× 四個倍速按鈕。
            </p>
            <h3 id="navigation">分頁網址</h3>
            <p>
              使用語意分頁參數；舊數字分頁不再對應原頁面，未知值會回到搜尋頁。
              檔案頁可另帶 <code>path</code>，例如 <code>/?tab=files&amp;path=/TV</code>。
            </p>
            <ReferenceTable
              label="網頁分頁網址"
              columns={["網址", "用途"]}
              rows={[
                ["/?tab=search", "搜尋；未指定 tab 時的預設頁。"],
                ["/?tab=push", "網址推送。"],
                ["/?tab=play", "播放控制。"],
                ["/?tab=subtitles", "字幕上傳與套用。"],
                ["/?tab=danmaku", "即時彈幕與彈幕來源。"],
                ["/?tab=settings", "配置、裝置操作、日誌與爬蟲測試入口。"],
                ["/?tab=files", "裝置檔案與傳輸。"],
              ]}
            />
            <h3>/remote：查詢與控制</h3>
            <p>
              GET 查詢狀態，POST 以表單欄位 <code>type</code> 送出指令。
              成功回傳 JSON（application/json），禁止快取；沒有本機播放時回傳
              <code>{'{"active":false}'}</code>。
              回應不包含播放網址；完整網址由日誌資訊 API 提供。
              網頁僅在播放頁位於前景時約每秒查詢，返回播放頁立即更新。
            </p>
            <CodeBlock label="HTTP" path="查詢、設定循環與跳轉">
              {`curl "http://127.0.0.1:9978/remote"
curl --data-urlencode "type=repeat_on" "http://127.0.0.1:9978/remote"
curl --data-urlencode "type=repeat_off" "http://127.0.0.1:9978/remote"
curl --data-urlencode "type=seek" --data-urlencode "position=120000" "http://127.0.0.1:9978/remote"
curl --data-urlencode "type=speed" --data-urlencode "speed=1.5" "http://127.0.0.1:9978/remote"`}
            </CodeBlock>
            <ReferenceTable
              label="網頁遙控指令"
              columns={["type", "用途"]}
              rows={localRemoteControls}
            />
            <FieldTable label="Remote" fields={localRemoteFields} />
            <p>
              <code>artwork</code> 指向同一服務的 <code>/remote/artwork?key=…</code>，
              成功回傳 PNG；瀏覽器不需傳送來源網址、Referer 或 Cookie。
              key 須對應目前封面；更換封面或停止播放後，舊 key 回傳 404。
              缺少或格式不正確的 key 為 400；載入逾時為 503，載入或編碼失敗為 500。
            </p>
            <p>
              既有 <code>/action?do=control&amp;type=repeat</code> 仍為切換循環。
              上一項、下一項及停止可能先派送至播放介面，應以後續 GET 確認結果。
            </p>
            <p>
              <code>seek</code> 的 <code>position</code> 是非負整數毫秒，超過總長時跳到結尾；
              缺少、格式錯誤或超過 64 位元整數範圍時回傳 400。
              <code>canSeek</code> 為 false 時回傳 409。
              <code>speed</code> 必須是 0.1–5.0 範圍內的有限數值，否則回傳 400；
              <code>canSetSpeed</code> 為 false 時回傳 409。
            </p>
            <p>
              缺少或未知指令回傳 400，跨來源的瀏覽器控制請求回傳 403；
              無可控制的播放、直播循環或不支援上一項／下一項回傳 409。
              不支援的 HTTP 方法回傳 405，主執行緒忙碌或請求中斷回傳 503。
            </p>
            <h3 id="remote-keys">/remote/key：獨立遙控器</h3>
            <p>
              從 App 網頁右上角開啟 <code>/remote.html</code>，即可操作遙控器。
              返回時會保留原分頁與檔案目錄。
            </p>
            <CodeBlock label="HTTP" path="送出遙控器按鍵">
              {`curl --data-urlencode "key=ok" "http://127.0.0.1:9978/remote/key"`}
            </CodeBlock>
            <ReferenceTable
              label="遙控器按鍵參數"
              columns={["POST 表單參數", "說明"]}
              rows={[
                ["key", "up／down／left／right（方向）、ok（確認）、back（返回）、menu（選單）或 0–9。"],
                ["action", "press＝單按（預設）。長按依序送 down（按住）、repeat（持續）、up（放開）；取消用 cancel。"],
                ["id", "非 press 必填，限 1–64 個英數字或 -。同次長按沿用相同的 key 與 id。"],
              ]}
            />
            <p>
              單按或開始長按需有前景頁面或取得焦點的對話框，否則回傳 409。
              成功回傳 <code>{'{"accepted":true,"holding":false}'}</code>；holding 為 true 表示仍按住。
            </p>
            <p>
              長按 5 秒未續送 repeat 會自動放開；正常結束仍請送 up 或 cancel。
            </p>
            <h3 id="remote-media">字幕上傳與彈幕套用</h3>
            <p>
              先以 GET <code>/remote</code> 取得目前 <code>mediaKey</code>，
              再以 POST 表單的 <code>key</code> 與 <code>path</code> 套用來源。
              字幕須先上傳到裝置共用儲存區，支援 SRT／SSA／ASS；
              彈幕可使用同範圍內的既有檔案，或 HTTP(S) 網址。
            </p>
            <CodeBlock label="HTTP" path="將 MEDIA_KEY 替換成剛查到的 mediaKey">
              {`curl --data-urlencode "key=MEDIA_KEY" --data-urlencode "path=/TV/subtitle.srt" "http://127.0.0.1:9978/remote/subtitle"
curl --data-urlencode "key=MEDIA_KEY" --data-urlencode "path=https://example.com/danmaku.xml" "http://127.0.0.1:9978/remote/danmaku"`}
            </CodeBlock>
            <p>
              沒有可控制的本機播放、直播或已換成另一個播放來源時回傳 409；
              key 格式不正確、檔案無效或超出允許路徑時回傳 400。
              這些 POST 與播放控制、按鍵 API 均檢查瀏覽器 Origin，跨來源請求回傳 403。
            </p>
            <h3 id="remote-info">/remote/info：連線狀態與裝置資訊</h3>
            <p>
              點主頁連線狀態可查看 App 版本、variant、buildType、裝置名稱、Android 版本、SDK 與 ABI。
              資料由 GET <code>/remote/info</code> 提供，回傳 JSON 並禁止快取；
              與提供裝置探索及同步資料的 <code>/device</code> 分開。
              網頁離線時保留最後一次成功讀取的資訊。
            </p>
          </section>

          <section id="logs">
            <h2>日誌下載與分享</h2>
            <p>
              開啟 <code>/logs.html</code> 查看日誌，自動擷取最長 60 分鐘。
              「停止擷取」停止來源裝置的收集；「暫停顯示」只暫停畫面更新。
            </p>
            <p>
              「下載日誌」與「分享日誌」匯出同一份檔案，包含版本、裝置、播放資訊與日誌。
              不支援檔案分享時改為下載；讀取失敗可按「重新讀取資訊」，保留日誌。
            </p>
            <p>
              匯出不受搜尋與等級篩選影響，上限 512 KiB；超限保留較新日誌並標示缺漏。
              檔案包含匯出時間、狀態取樣時間與日誌筆數。
            </p>
            <p>
              手機版的裝置清單提供「操作」與「日誌」按鈕，長按裝置名稱可命名。
              別名只存於目前手機，依有效裝置識別碼保留，不受 IP 改變影響；清空即恢復原名。
            </p>
            <h3>日誌資訊 API</h3>
            <CodeBlock label="HTTP" path="取得日誌資訊">
              {`curl "http://127.0.0.1:9978/log/info"`}
            </CodeBlock>
            <ReferenceTable
              label="日誌資訊欄位"
              columns={["欄位", "用途"]}
              rows={[
                ["version / versionCode / variant / buildType", "應用版本與建置資訊。"],
                ["device / android / sdk / abis", "裝置與 Android 資訊。"],
                ["playback", "播放狀態，不含 artwork。"],
                ["playback.url", "解析後的完整網址，保留 query；無來源為空字串。"],
                ["collection", "日誌收集狀態，含 READ_LOGS 權限、目前 UID／PID 等診斷資訊。"],
                ["playbackLog", "目前播放的診斷取樣；無播放器時仍依實際狀態回傳。"],
                ["lastPlaybackError", "最近一次保存的播放錯誤，沒有錯誤時可為 null。"],
              ]}
            />
            <p>
              GET 回傳 JSON；停止或失敗後仍保留來源時，網址也會保留。
              不含封面 URL、UUID、序號或 MAC。
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
            <h3>重新命名與搬移</h3>
            <CodeBlock label="HTTP" path="重新命名後搬到另一個既有目錄">
              {`curl --data-urlencode "path=/TV/subtitle.srt" --data-urlencode "name=demo.srt" "http://127.0.0.1:9978/rename"
curl --data-urlencode "path=/TV/demo.srt" --data-urlencode "target=/TV/subtitles" "http://127.0.0.1:9978/move"`}
            </CodeBlock>
            <p>
              檔案與目錄均可操作；新名稱只是一個名稱，不能包含路徑分隔符。
              目標已存在時不覆蓋，搬移也不能把目錄放進自己裡面。
              網頁提供批次搬移與刪除，逐項操作並回報結果。
            </p>
            <h3>一般上傳與刪除</h3>
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
              上傳、新建、重新命名、搬移與刪除都限制在共用儲存根目錄內，禁止刪除或搬移根目錄本身。/file
              的區網請求也限制於此範圍；本機 loopback 請求可由 Path.local
              解析其他可存取路徑。
            </p>
          </section>

          <section id="transfers">
            <h2>可續傳上傳與裝置下載</h2>
            <p>
              上傳中斷後，重新選取相同檔案即可續傳。「裝置下載」會將網址內容存入 App 裝置。
            </p>
            <p>
              以下均使用 POST；<code>/upload/chunk</code> 用 multipart，其餘用表單。
              JSON 回應使用 <code>text/plain</code>，禁止快取。
            </p>
            <ReferenceTable
              label="續傳與裝置下載端點"
              columns={["端點", "參數", "行為"]}
              rows={localTransferEndpoints}
            />
            <h3>分塊上傳</h3>
            <p>
              先計算整個檔案的 SHA-256，以固定 id 呼叫 start；同一 id 續傳時，
              path、name、size 與 sha256 必須保持一致。
              回應包含 <code>offset</code>、<code>complete</code> 及 <code>chunkSize</code>（目前 4 MiB）。
              依 offset 傳送下一塊，全部上傳後呼叫 finish；伺服器檢查 SHA-256 後才完成落檔。
            </p>
            <p>
              start 指定 <code>parallel=true</code> 時，另外回傳 <code>revision</code>、
              <code>chunks</code> 及 <code>threads</code>（目前 4）。
              chunks 是已收到的分塊索引；chunk 與 finish 均須帶回 revision。
              不要只依 offset 判斷並行分塊是否全部完成；若版本改變，重新 start 取得狀態。
              過期上傳回傳 404，重新選取原檔案建立或恢復傳輸。
            </p>
            <h3>裝置下載任務</h3>
            <CodeBlock label="HTTP" path="由 App 裝置下載到既有 TV 目錄">
              {`curl --data-urlencode "url=https://example.com/video.mp4" --data-urlencode "path=/TV" --data-urlencode "name=demo.mp4" "http://127.0.0.1:9978/download/start"
curl -X POST "http://127.0.0.1:9978/download/status"`}
            </CodeBlock>
            <p>
              每次回應包含任務 <code>id</code>、name、path、state、total、downloaded、speed 與 error。
              大小以位元組、speed 以位元組／秒表示；未知 total 為 -1。
              pause、resume、cancel 必須帶回同一 id，舊 id 回傳 409；尚無任務時回傳 404。
              每個 App 服務同時只執行一個裝置下載，進行中再次 start 回傳 409。
            </p>
            <p>
              暫停或取消可能先回報 pausing／cancelling，請繼續查詢並以最終狀態為準；
              只有 completed 才代表完成，failed 時查看 error。
              可用 status 重新取得遺失的任務回應，但任務狀態不保證跨 App 重啟保留。
              下載暫存由 App 管理，檔案列表隱藏 <code>.tv-downloads</code> 目錄。
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
