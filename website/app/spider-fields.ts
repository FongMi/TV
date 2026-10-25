import type { Field } from "./config-fields";

export const networkMethods = [
  ["req / http", "取得 code、headers、content 與 error；HTTP 非 2xx 需自行判斷。body 支援文字或位元組陣列。"],
  ["json", "取得 JSON；連線失敗、HTTP 非 2xx 或無效 JSON 會拋出例外。"],
  ["get / post", "Java 取得文字正文；HTTP 非 2xx 仍保留正文。"],
  ["getUserAgent", "Python／QuickJS 取得 App 的 User-Agent，按需放入 headers。"],
  ["getCookie / setCookie", "讀寫 WebView Cookie；Session 使用隔離 Cookie。"],
  ["session", "建立隔離 Cookie 的請求流程，用完關閉。"],
  ["batch", "批次 HTTP 請求，預設最多 4 筆並行，按輸入順序回傳。"],
  ["map", "三語言逐項執行回呼，預設最多 4 項並行，保留輸入順序。"],
  ["download", "直接下載至檔案，由共用下載器管理暫存、續傳與分段。"],
  ["openStream", "Python／QuickJS 分塊讀取，用完關閉 reader。"],
  ["requestStreamAsync", "Java 傳入 NetOptions，Future 回傳 Net.StreamResult。"],
  ["stream", "Python／QuickJS 原樣轉送網址，或由產生器提供 proxy 正文。"],
  ["ws", "一次 WebSocket 訊息交換，取得首個完整訊息後關閉。"],
  ["connect", "持續 WebSocket 連線，可設定斷線恢復與接收逾時。"],
  ["cached / peek / refresh", "TTL 快取、讀取已有值與背景更新。"],
  ["cachedText", "Python 取得快取的 JSON 原文。"],
  ["clearCache", "清除一筆；省略 key 清除目前站點全部 TTL 快取並取消載入。"],
  ["sleep", "可取消的等待，單位為毫秒。"],
];

export const dialogFields: Field[] = [
  { name: "title / message", type: "string", description: "選填標題與說明。" },
  { name: "image", type: "string", description: "選填 base64 圖片 data URI，如 data:image/png;base64,...；由爬蟲準備內容。onAction 可更新，空字串移除；解碼失敗時，尚未結束的 dialog 會回報錯誤。" },
  { name: "qrcode", type: "string", description: "選填非空白字串，在本機產生 QR Code；內容不必是網址，無法編碼時回報錯誤。" },
  { name: "copyable", type: "boolean", description: "可選取 message，並提供複製全文按鈕；複製不關窗，也不呼叫 onAction。", default: "false" },
  { name: "cancelable", type: "boolean", description: "允許返回鍵取消；false 時也阻止點外側取消。頁面銷毀及呼叫取消仍會清理視窗。", default: "true" },
  { name: "inputs", type: "object[]", description: "選填輸入欄位；name 必填且不可重複，label 預設 name；value 為字串，預設空字串；多選為字串陣列，預設空陣列。" },
  { name: "inputs[].helperText", type: "string", description: "欄位下方的說明文字，省略或空字串時隱藏。" },
  { name: "inputs[].type", type: "string", description: "text、phone、number、password 為單行，textarea 為多行，select 為單選，multiselect 為多選清單；格式與業務驗證由爬蟲負責。", default: "text" },
  { name: "inputs[].options", type: "object[]", description: "select／multiselect 必填非空陣列；value 為非空白且唯一的字串，label 預設 value 且不可空白。顯示 label，單選回傳字串，多選依選項順序回傳字串陣列；非空初始值須存在於選項，多選不可重複。" },
  { name: "inputs[].enabled", type: "boolean", description: "允許操作欄位；停用時略過內建驗證，values 仍包含其值。onAction 可回傳 inputs 陣列，依 name 更新 enabled。", default: "true" },
  { name: "inputs[].required", type: "boolean", description: "不可為空或全空白，多選至少選一項。", default: "false" },
  { name: "inputs[].minLength / maxLength", type: "integer", description: "字串欄位的非負整數限制，以 Unicode 字元計算；maxLength 不可小於 minLength。留空時略過 minLength。", default: "0 / 不限制" },
  { name: "inputs[].minItems / maxItems", type: "integer", description: "多選欄位的非負整數限制，以已選數量計算；maxItems 不可小於 minItems。留空時略過 minItems。", default: "0 / 不限制" },
  { name: "buttons", type: "object[]", description: "選填 1～3 個按鈕，依序對應 positive、negative、neutral；id 與 label 必填且不可為空，id 不可重複。省略時提供確定（ok）與取消。" },
  { name: "buttons[].close", type: "boolean", description: "false 時保留視窗，必須提供 onAction。", default: "true" },
  { name: "buttons[].enabled", type: "boolean", description: "允許操作按鈕；onAction 可回傳 buttons 陣列，依 id 更新 enabled。", default: "true" },
  { name: "buttons[].validate", type: "string[]", description: "驗證指定的欄位 name；空陣列略過驗證。", default: "全部欄位" },
  { name: "onAction", type: "function", description: "驗證通過後接收 {button, values}，button 為按鈕 id。Java／Python 以第二參數傳入同步回呼，QuickJS 放在 options 內，可回傳 Promise。回傳 {message, qrcode, image, values, errors, inputs, buttons, close} 更新內容或關窗設定。" },
];

export const liveDanmakuFields: Field[] = [
  {
    name: "text",
    type: "string",
    required: true,
    description: "解碼後的文字；不得全空白，最多 4096 字元。",
  },
  {
    name: "color",
    type: "integer",
    description: "RGB 顏色，範圍 0～16777215。",
    default: "16777215（白色）",
  },
  {
    name: "size",
    type: "number",
    description: "字級，單位 sp，範圍 0～96；0 跟隨使用者設定。",
    default: "0",
  },
  {
    name: "type",
    type: "integer",
    description: "1 滾動、4 底部、5 頂部、6 反向。",
    default: "1",
  },
  {
    name: "colors",
    type: "integer[]",
    description: "2～8 個 RGB 顏色組成漸層；省略時使用 color。",
  },
];

export const methods = [
  ["init", "初始化爬蟲時", "讀取 ext、建立連線或快取", "無"],
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
  ["playerContent / play", "選擇集數", "最終 URL 與播放參數", "Result"],
  ["danmaku", "選用爬蟲即時彈幕來源", "彈幕或訂閱狀態物件", "Java AutoCloseable／Python、QuickJS 停止函式"],
  ["liveContent / live", "載入直播配置", "TXT、M3U 或 Group JSON", "文字 / Group 陣列"],
  [
    "proxy / localProxy",
    "本地代理收到請求",
    "依執行環境提供狀態、類型、內容與標頭",
    "陣列 / HTTP 回應",
  ],
  ["action", "自訂操作", "可由 Result 解析的結果", "Java JSON 字串／Python、QuickJS 物件"],
  ["destroy", "重新載入或清除", "釋放執行緒與連線；QuickJS 清理 hook 最長 5 秒", "無"],
];

export const resultFields: Field[] = [
  {
    name: "web",
    type: "{url, mode?, cookie?: {url?, keys: string[]}[]}",
    description: "action 回應的開網頁指令，只執行一次；詳見開網頁章節。",
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
    description: "首頁、分類與搜尋卡片；詳情取 list[0]。播放結果可用 list[0] 更新該片詳情，預載時延至採用結果才套用。",
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
    name: "timeshift",
    type: "object",
    description: "直播回看範圍與取源網址 {url, start, window}，格式見直播回看章節。",
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
      "彈幕清單、單一 URL 或 JSON 陣列字串，可使用檔案或爬蟲即時來源；受 App 彈幕載入設定控制。",
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
      ["url", "HTTP(S) 資料網址，或由 getDanmakuUrl(id) 產生的即時來源。"],
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
