import type { Field } from "./config-fields";

export const networkMethods = [
  ["req / http", "取得 code、headers、content 與 error；HTTP 非 2xx 需自行判斷。"],
  ["json", "取得 JSON；連線失敗、HTTP 非 2xx 或無效 JSON 會拋出例外。"],
  ["get / post", "Java 取得文字正文；連線失敗拋出例外，HTTP 非 2xx 仍保留正文。"],
  ["session", "建立隔離 Cookie 的請求流程，用完關閉。"],
  ["getCookie / setCookie", "讀寫 WebView Cookie；Session 使用隔離 Cookie。"],
  ["batch", "批次請求，預設每批最多 4 筆並行，按輸入順序回傳。"],
  ["cached / peek / refresh", "TTL 快取；peek 只讀已有值，refresh 背景更新且遵守 TTL。"],
  ["clearCache", "清除一筆；省略 key 清除目前站點全部 TTL 快取並取消載入。"],
  ["download", "直接串流至檔案；下載失敗會拋出例外，部分檔案由呼叫者處理。"],
  ["ws", "一次 WebSocket 訊息交換，取得首個完整文字訊息後關閉。"],
  ["sleep", "可取消的等待，單位為毫秒。"],
];

export const methods = [
  ["init", "建立實例時；Node 重啟可再次呼叫", "讀取 ext、建立連線或快取", "無"],
  ["homeContent / home", "進入首頁", "分類與選填 filters", "Result.class"],
  ["homeVideoContent / homeVod", "首頁分類完成", "首頁推薦卡片", "Result.list"],
  [
    "categoryContent / category",
    "分類、換頁、變更篩選",
    "該頁卡片與總頁數",
    "list + pagecount",
  ],
  ["detailContent / detail", "點擊卡片", "完整資訊與播放分組", "list[0]"],
  [
    "searchContent / search",
    "搜尋",
    "搜尋結果，可支援頁碼",
    "list + pagecount",
  ],
  ["playerContent / play", "選擇集數", "最終 URL 與播放參數", "Result.url、選填 skips"],
  ["liveContent / live", "載入直播配置", "TXT、M3U 或 Group JSON", "文字 / Group 陣列"],
  [
    "proxy / localProxy",
    "本地代理收到請求",
    "依執行環境提供狀態、類型、內容與標頭",
    "陣列 / HTTP 回應",
  ],
  ["action", "自訂操作", "可由 Result 解析的結果", "Java JSON 字串 / Python、JS 物件"],
  ["destroy", "重新載入或清除", "釋放執行緒、連線、引擎資源", "無"],
];

export const resultFields: Field[] = [
  {
    name: "web",
    type: "{url, mode?, cookie?: {url?, keys: string[]}[]}",
    description: "action 回應的開網頁指令，只執行一次。url 限 HTTP(S)；mode 省略為普通網頁，view 為返回退出的內容視窗。Cookie 同組全部需要、不同組任一組完成即可；需有新值且頁面同源。詳見開網頁的 JSON 格式說明。",
  },
  {
    name: "class",
    type: "Class[]",
    description: "首頁分類清單。",
    default: "[]",
  },
  {
    name: "list",
    type: "Vod[]",
    description: "首頁、分類與搜尋卡片；詳情取 list[0]。",
    default: "[]",
  },
  {
    name: "filters",
    type: "object",
    description: "以分類 ID 為鍵，值為 Filter 陣列或單一 Filter。",
    default: "{}",
  },
  {
    name: "url",
    type: "string | string[] | Url",
    description: "播放 URL 或多畫質清單，三種寫法見下方範例。",
  },
  {
    name: "skips",
    type: "SkipSegment[]",
    description: "每集片頭、片中、片尾的跳過區間，start / end 都用毫秒；格式錯誤的項目會忽略。",
    default: "[]",
  },
  {
    name: "header",
    type: "object | string",
    description:
      "播放請求標頭；也接受 JSON 物件字串。整份為空時回退 Site.header，不逐鍵合併。",
    default: "{}",
  },
  {
    name: "msg",
    type: "string",
    description: "顯示提示文字；只有 code 為 0 時顯示。",
    default: "空字串",
  },
  {
    name: "code",
    type: "integer",
    description: "訊息控制值，不是 HTTP 狀態碼；非 0 時隱藏 msg。",
    default: "0",
  },
  {
    name: "danmaku",
    type: "Danmaku[] | string",
    description:
      "彈幕清單、單一 URL 或 JSON 陣列字串；仍受 App 彈幕載入設定控制。",
    default: "[]",
  },
  { name: "subs", type: "Sub[]", description: "外掛字幕清單。", default: "[]" },
  {
    name: "playUrl",
    type: "string",
    description: "解析前綴；json: 後接 JSON 解析端點，parse: 後接具名解析器。",
    default: "空字串",
  },
  {
    name: "artwork",
    type: "string",
    description: "更新播放頁顯示的圖片。",
    default: "空字串",
  },
  {
    name: "jxFrom",
    type: "string",
    description:
      "擴展 JSON 解析結果回報的解析器名稱，用於成功提示；不是播放分組旗標。",
    default: "空字串",
  },
  {
    name: "flag",
    type: "string",
    description:
      "播放分組與解析比對旗標；未填時 App 補入呼叫 playerContent 時的 flag。",
    default: "由 App 補入",
  },
  {
    name: "desc",
    type: "string",
    description: "清理後更新播放頁描述。",
    default: "空字串",
  },
  {
    name: "format",
    type: "string",
    description:
      "媒體 MIME type 提示，請填完整類型，例如 application/x-mpegURL。",
  },
  {
    name: "click",
    type: "string",
    description: "WebView 載入後執行的 JavaScript；Site.click 非空時優先。",
    default: "空字串",
  },
  {
    name: "key",
    type: "string",
    description: "App 內部回填的 Site key；爬蟲不必提供，提供也會被覆寫。",
    default: "由 App 補入",
  },
  {
    name: "position",
    type: "integer",
    description:
      "播放起點，單位毫秒；120000 為 2 分鐘。未填時不覆蓋既有播放進度。",
  },
  {
    name: "pagecount",
    type: "integer",
    description: "總頁數；0 或未填表示未知，仍可繼續載入更多。",
    default: "0",
  },
  {
    name: "parse",
    type: "integer",
    description: "1 要求解析；0 仍可能因 jx=1 或配置旗標比對進入解析。",
    default: "0",
  },
  {
    name: "jx",
    type: "integer",
    description: "1 要求進入解析，即使 parse=0。",
    default: "0",
  },
  {
    name: "drm",
    type: "Drm",
    description: "播放 DRM 設定，與 Channel.drm 使用相同物件，見配置字典。",
  },
];

export const objectGroups = [
  {
    name: "Vod",
    fields: [
      ["vod_id", "傳給 detailContent 的唯一值。"],
      ["vod_name", "顯示名稱。"],
      ["vod_pic / vod_remarks", "縮圖與角標。"],
      ["type_name", "分類文字。"],
      ["vod_year / vod_area", "年份與地區。"],
      ["vod_director / vod_actor", "人員資訊。"],
      ["vod_content", "詳細描述。"],
      ["vod_play_from / vod_play_url", "播放分組與集數，分隔方式見下節。"],
      ["vod_tag / action", "folder 表示資料夾；非空 action 在點擊時觸發操作。type 3 傳給爬蟲 action()；type 4 填回傳 JSON 的接口網址。"],
      ["cate", "提供此物件時也視為資料夾，可含 land、circle、ratio。"],
      ["land / circle / ratio / style", "卡片外觀覆蓋。"],
    ],
  },
  {
    name: "Class",
    fields: [
      ["type_id / id", "傳給 categoryContent 的分類 ID。"],
      ["type_name / name", "分類顯示名稱。"],
      ["type_flag", "1 代表資料夾分類。"],
      ["land / circle / ratio", "該分類預設卡片樣式。"],
      ["filters", "也可在分類物件內提供 Filter 陣列。"],
    ],
  },
  {
    name: "Filter",
    fields: [
      ["key / name", "傳入鍵與顯示名稱。"],
      ["init", "預設選項值。"],
      [
        "value",
        '選項陣列，例如 [{"n":"全部","v":""}]；n 是顯示名稱，v 是傳入值。',
      ],
    ],
  },
  {
    name: "Sub",
    fields: [
      ["url", "字幕檔位置。"],
      ["name / lang", "顯示名稱與語言代碼。"],
      ["format", "字幕 MIME type，可省略並由 App 推定。"],
      [
        "flag",
        "0 交由 App 依字幕數量與偏好語言選擇；非零使用 Media3 選擇旗標：1 預設、2 強制、4 自動選擇，可位元組合。",
      ],
    ],
  },
  {
    name: "Danmaku",
    fields: [
      ["url", "彈幕資料位置。"],
      ["name", "選填顯示名稱。"],
    ],
  },
  {
    name: "SkipSegment",
    fields: [
      ["type", "opening 為片頭，middle 為片中，ending 為片尾。"],
      ["start", "開始跳過的時間（毫秒）。"],
      ["end", "要跳到的時間（毫秒）；須晚於 start，僅片尾且 start > 0 時可省略。"],
    ],
  },
];
