import type { Field } from "./config-fields";

export const localSections = [
  ["connection", "連線與回應"],
  ["action", "動作指令"],
  ["sync", "裝置同步"],
  ["media", "播放狀態"],
  ["device", "裝置資訊"],
  ["files", "檔案管理"],
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
