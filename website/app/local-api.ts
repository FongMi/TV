import type { Field } from "./config-fields";

export const localSections = [
  ["connection", "連線與回應"],
  ["action", "動作指令"],
  ["sync", "裝置同步"],
  ["remote", "網頁播放與遙控器"],
  ["logs", "日誌下載與分享"],
  ["media", "播放狀態"],
  ["device", "裝置資訊"],
  ["files", "檔案管理"],
  ["transfers", "續傳與裝置下載"],
  ["cache", "鍵值快取"],
  ["internal", "代理與內部端點"],
] as const;

export const localActions = [
  ["control", "type", "控制目前播放器；可用指令見下表。"],
  ["danmaku", "text", "送出一條即時彈幕；需要有效的播放服務與非空文字。"],
  [
    "refresh",
    "type；依類型加 path 或 json",
    "重新整理頁面，或推送字幕、彈幕及 Vod 更新。",
  ],
  ["push", "url", "把非空 URL 交給 App 推送播放流程。"],
  ["file", "path", "開啟裝置上的檔案；不是上傳檔案。"],
  ["search", "word", "把非空關鍵字交給 App 搜尋介面。"],
  [
    "setting",
    "text、name（選填）",
    "把配置位置或設定文字交給設定流程；text 不可為空。",
  ],
  [
    "cast",
    "config、device、history",
    "三者均為 JSON 物件字串，交給 App 投放事件處理。",
  ],
  [
    "sync",
    "type、mode、force 等",
    "交換觀看紀錄或收藏；參數與資料格式見下節。",
  ],
] as const;

export const localControls = [
  ["play", "播放"],
  ["pause", "暫停"],
  ["stop", "停止"],
  ["prev", "上一項"],
  ["next", "下一項"],
  ["repeat", "切換循環模式"],
  ["replay", "重新播放"],
] as const;

export const localRemoteControls = [
  ["play", "播放；已播完時從頭播放。"],
  ["pause", "暫停目前影片。"],
  ["stop", "停止播放。"],
  ["prev", "上一集／上一台；須支援切換項目。"],
  ["next", "下一集／下一台；須支援切換項目。"],
  ["repeat_on", "開啟目前影片的單集循環；重複請求仍為開啟。"],
  ["repeat_off", "關閉單集循環；重複請求仍為關閉。"],
  ["seek", "跳到 position 指定的毫秒位置；須支援跳轉，不改變播放／暫停狀態。"],
  ["speed", "以 speed 設定 0.1–5.0 倍速；須支援倍速，直播不可用。"],
] as const;

export const localRemoteFields: Field[] = [
  {
    name: "active",
    type: "boolean",
    description: "是否有可遙控的本機播放；false 時不回傳其餘欄位。",
  },
  {
    name: "title",
    type: "string",
    description: "目前影片的顯示標題。",
  },
  {
    name: "mediaKey",
    type: "string",
    description: "目前播放來源的識別鍵；套用字幕或彈幕時以 key 傳回，換片後須重新取得。",
  },
  {
    name: "artwork",
    type: "string",
    description: "目前影片的本機封面端點；沒有封面時為空字串，載入失敗時網頁顯示佔位圖。僅 /remote 回傳此欄位。",
  },
  {
    name: "position",
    type: "integer",
    description: "目前播放位置，單位毫秒，最小為 0。",
  },
  {
    name: "duration",
    type: "integer",
    description: "總長，單位毫秒；未知長度為 0。",
  },
  {
    name: "state",
    type: "integer",
    description: "1＝播放結束；2＝就緒但未播放；3＝正在播放；6＝緩衝中。",
  },
  {
    name: "playWhenReady",
    type: "boolean",
    description: "播放器是否設定為播放；影片已播完時為 false。",
  },
  {
    name: "repeatOne",
    type: "boolean",
    description: "目前是否開啟單集循環，播完會重新播放同一部影片。",
  },
  {
    name: "live",
    type: "boolean",
    description: "是否為直播；網頁隱藏循環開關，API 拒絕循環指令。",
  },
  {
    name: "canNavigate",
    type: "boolean",
    description: "是否支援上一項／下一項。",
  },
  {
    name: "canSeek",
    type: "boolean",
    description: "是否支援網頁跳轉；直播、未知長度或播放器不能跳轉時為 false。",
  },
  {
    name: "canSetSpeed",
    type: "boolean",
    description: "是否支援倍速；直播或播放器不支援時為 false。",
  },
  {
    name: "speed",
    type: "number",
    description: "目前播放倍速，例如 1 或 1.5。",
  },
  {
    name: "engine",
    type: "string",
    description: "目前播放引擎，ExoPlayer 或 mpv。",
  },
  {
    name: "video",
    type: "string",
    description: "播放器回報的視訊尺寸文字。",
  },
];

export const localRefresh = [
  ["live", "—", "直播"],
  ["detail", "—", "詳情"],
  ["player", "—", "播放器"],
  ["category", "—", "分類"],
  ["subtitle", "path", "字幕檔位置"],
  ["danmaku", "path", "彈幕檔位置"],
  ["vod", "json", "Vod 物件的 JSON 字串"],
] as const;

export const localFileEndpoints = [
  ["/file/{path}", "GET", "瀏覽目錄或下載檔案；/file 為共用儲存根目錄。"],
  [
    "/upload",
    "POST multipart/form-data",
    "path 必填且目錄須已存在；檔案使用 multipart 欄位上傳。",
  ],
  ["/newFolder", "GET / POST", "path 為既有父目錄，name 為新資料夾名稱。"],
  ["/rename", "POST 表單", "path 為既有檔案或目錄，name 為新名稱；目標不可已存在。"],
  ["/move", "POST 表單", "path 為來源，target 為既有目標目錄；保留名稱，不覆蓋同名項目，也不可搬進自身目錄。"],
  [
    "/delFolder",
    "GET / POST",
    "path 必填；遞迴刪除指定項目，不可刪除儲存根目錄。",
  ],
  [
    "/delFile",
    "GET / POST",
    "與 /delFolder 共用刪除邏輯，並非只允許刪除一般檔案。",
  ],
] as const;

export const localTransferEndpoints = [
  ["/upload/start", "id、path、name、size、sha256；parallel（選填）", "建立或查詢續傳。id 是 32 位小寫十六進位；size 是檔案位元組數，sha256 是 64 位小寫十六進位。"],
  ["/upload/chunk", "id、offset、size；multipart chunk，並行模式另帶 revision", "上傳分塊；依 start 回傳的 chunkSize 切分，offset 與 size 以位元組計算。"],
  ["/upload/finish", "id；並行模式另帶 revision", "檢查完整大小與 SHA-256，再落檔或解壓 ZIP；只有 complete=true 才完成。"],
  ["/download/start", "url、path；name（選填）", "由 App 裝置下載 HTTP(S) 檔案到既有目錄；省略 name 時取網址最後一段，無名稱時用 download。"],
  ["/download/status", "id（選填）", "查詢目前任務；省略 id 可重新取得目前任務。"],
  ["/download/pause", "id", "暫停進行中的任務；依回傳狀態確認，已結束的任務不改變狀態。"],
  ["/download/resume", "id", "恢復 paused 或 failed 任務；能否接續取決於來源支援及已保存資料。"],
  ["/download/cancel", "id", "取消任務；停止後為 cancelled，不等同刪除目的檔案。"],
] as const;

export const localCacheActions = [
  ["get", "key、rule（選填）", "回傳儲存的字串；不存在時為空字串。"],
  ["set", "key、value、rule（選填）", "寫入字串，回傳 OK。"],
  ["del", "key、rule（選填）", "移除鍵，回傳 OK。"],
] as const;

export const localMediaFields: Field[] = [
  {
    name: "state",
    type: "integer",
    description: "3＝正在播放；6＝緩衝中；2＝就緒但未播放；其餘狀態回傳 1。",
  },
  {
    name: "speed",
    type: "number",
    description: "目前播放速率，1 表示正常速度。",
  },
  {
    name: "duration",
    type: "integer",
    description:
      "播放器回報的總長，單位毫秒；未知長度可能是負值，不保證為 -1。",
  },
  {
    name: "position",
    type: "integer",
    description: "目前播放位置，單位毫秒。",
  },
  {
    name: "url",
    type: "string",
    description: "目前播放 URL；沒有值時為空字串。",
  },
  {
    name: "title",
    type: "string",
    description: "媒體標題；沒有值時為空字串。",
  },
  {
    name: "artist",
    type: "string",
    description: "媒體的 artist 中繼資料；沒有值時為空字串。",
  },
  {
    name: "artwork",
    type: "string",
    description: "封面 URI；沒有值時為空字串。",
  },
];

export const localDeviceFields: Field[] = [
  {
    name: "uuid",
    type: "string",
    description: "此 App 裝置使用的 Android ID。",
  },
  { name: "name", type: "string", description: "裝置顯示名稱。" },
  {
    name: "ip",
    type: "string",
    description: "完整服務位址，包含 http://、區域網路 IP 與實際埠號。",
  },
  {
    name: "type",
    type: "integer",
    description:
      "0＝電視版；1＝手機版。Device 物件另以 2 表示 DLNA 裝置，但本機 /device 不使用此值。",
  },
  {
    name: "serial",
    type: "string",
    description: "系統可取得的裝置序號；可用性受裝置與權限限制。",
  },
  {
    name: "eth",
    type: "string",
    description: "eth0 的 MAC 位址；不保證每個裝置都能取得。",
  },
  {
    name: "wlan",
    type: "string",
    description: "wlan0 的 MAC 位址；不保證每個裝置都能取得。",
  },
  {
    name: "time",
    type: "integer",
    description:
      "App 實例建立時的 Unix 時間，單位毫秒；不是這次 HTTP 回應的時間。",
  },
  {
    name: "id",
    type: "integer",
    description:
      "資料庫識別碼；/device 建立的是未儲存的 Device，因此通常不輸出此欄位。",
  },
];

export const localExamples = {
  media: {
    state: 3,
    speed: 1,
    duration: 7200000,
    position: 120000,
    url: "https://example.com/video.m3u8",
    title: "範例項目",
    artist: "Demo",
    artwork: "https://example.com/poster.jpg",
  },
  device: {
    uuid: "demo-android-id",
    name: "影視TV 示意裝置",
    ip: "http://192.0.2.10:9978",
    type: 0,
    serial: "",
    eth: "",
    wlan: "",
    time: 1788825600000,
  },
  folder: {
    parent: "",
    files: [
      {
        name: "demo.mp4",
        path: "/TV/demo.mp4",
        time: "2026/09/08 12:00:00",
        dir: 0,
      },
      {
        name: "subtitles",
        path: "/TV/subtitles",
        time: "2026/09/08 12:00:00",
        dir: 1,
      },
    ],
  },
  sync: {
    configs: [
      { id: 7, type: 0, url: "https://example.com/vod.json", name: "示意配置" },
    ],
    targets: [
      {
        key: "demo@@@demo-1@@@7",
        cid: 7,
        type: 0,
        siteName: "Demo",
        vodName: "範例項目",
        vodPic: "https://example.com/poster.jpg",
        createTime: 1788825600000,
      },
    ],
  },
};
