export type Field = {
  name: string;
  type: string;
  required?: boolean;
  description: string;
  default?: string;
};

export function filterFields(fields: Field[], query: string) {
  const value = query.trim().toLowerCase();
  return fields.filter((field) =>
    `${field.name} ${field.type} ${field.description} ${field.default ?? ""}`
      .toLowerCase()
      .includes(value),
  );
}

export const vodTop: Field[] = [
  {
    name: "urls",
    type: "Depot[]",
    description: "配置倉庫清單；登記各項配置後載入第一項，清單不可為空。",
  },
  {
    name: "msg",
    type: "string",
    description: "配置回傳的錯誤訊息；存在時停止載入，普通配置與倉庫都適用。",
  },
  {
    name: "spider",
    type: "string",
    description: "全域 Spider JAR 路徑或 URL；Site 未指定 jar 時使用。",
  },
  {
    name: "wallpaper",
    type: "string",
    description: "桌布圖片、GIF 或影片位置。",
  },
  { name: "logo", type: "string", description: "App Logo 圖片位置。" },
  { name: "notice", type: "string", description: "啟動後顯示的公告文字。" },
  {
    name: "sites",
    type: "Site[]",
    required: true,
    description: "點播站點清單。",
  },
  {
    name: "parses",
    type: "Parse[]",
    description: "需要進一步處理 URL 時使用的解析規則。",
  },
  {
    name: "lives",
    type: "Live[]",
    description: "內嵌直播配置清單；也可以另外載入獨立 LiveConfig。",
  },
  { name: "doh", type: "Doh[]", description: "DNS over HTTPS 端點。" },
  { name: "proxy", type: "Proxy[]", description: "依 host 套用的代理伺服器。" },
  {
    name: "rules",
    type: "Rule[]",
    description: "WebView 網路擷取與腳本規則。",
  },
  {
    name: "headers",
    type: "Header[]",
    description: "依 host 注入或覆寫 HTTP 請求標頭。",
  },
  {
    name: "hosts",
    type: "string[]",
    description: "原主機=目標主機或 IP 的 DNS 覆蓋。",
  },
  {
    name: "flags",
    type: "string[]",
    description: "提供給 playerContent 的全域平台旗標。",
  },
  { name: "ads", type: "string[]", description: "要攔截的主機名稱清單。" },
  {
    name: "danmaku",
    type: "string",
    description: "彈幕搜尋 API；可使用 {name} 與 {episode}。",
  },
  {
    name: "assrt",
    type: "string",
    description: "字幕搜尋服務使用的存取字串。",
  },
];

export const siteFields: Field[] = [
  {
    name: "key",
    type: "string",
    required: true,
    description: "站點唯一識別碼，不可重複。",
  },
  { name: "name", type: "string", required: true, description: "顯示名稱。" },
  {
    name: "type",
    type: "integer",
    description:
      "0=XML HTTP、1=JSON HTTP、3=Spider；4=擴充 HTTP JSON，分類篩選以 Base64 ext 傳送，站點 ext 另以 extend 傳送。",
    default: "0",
  },
  {
    name: "api",
    type: "string",
    required: true,
    description:
      "HTTP 端點、csp_ 類別，或以 ./、../ 表示的 .js／.py 相對路徑；也可使用完整網址。",
  },
  {
    name: "ext",
    type: "string | object | array",
    description:
      "type 3 傳給 Spider.init；HTTP 站點以 extend 傳送。物件與陣列先序列化為字串；主配置可填 URL 或相對路徑。",
  },
  { name: "jar", type: "string", description: "覆蓋全域 spider 的 JAR 位置。" },
  {
    name: "click",
    type: "string",
    description: "在 WebView 執行的 JavaScript 點擊程式。",
  },
  {
    name: "playUrl",
    type: "string",
    description:
      "type 0/1 的解析 URL 前綴；支援 json: 端點或 parse: 解析器名稱。type 3/4 由播放 Result.playUrl 提供。",
  },
  {
    name: "hide",
    type: "integer",
    description: "1 時從站點清單隱藏。",
    default: "0",
  },
  {
    name: "indexs",
    type: "integer",
    description: "1 時作為索引站點。",
    default: "0",
  },
  {
    name: "timeout",
    type: "integer",
    description: "播放逾時秒數，最小 1。",
    default: "15",
  },
  {
    name: "searchable",
    type: "integer",
    description: "0=永久停用、1=啟用；執行期間也可能保存為 2。",
    default: "1",
  },
  {
    name: "changeable",
    type: "integer",
    description: "0=禁止切換、1=可切換；執行期間也可能保存為 2。",
    default: "1",
  },
  {
    name: "lang",
    type: "string",
    description:
      "搜尋關鍵字轉換語系，支援繁簡中文標籤；非空的其他值保留原文，空值使用既有轉換。",
  },
  {
    name: "danmaku",
    type: "integer",
    description:
      "0 停用此站點透過全域彈幕 API 自動搜尋，不影響 Spider 自帶彈幕。",
    default: "1",
  },
  {
    name: "quickSearch",
    type: "integer",
    description: "0 跳過快速搜尋，1 啟用。",
    default: "1",
  },
  {
    name: "categories",
    type: "string[]",
    description:
      "有匹配名稱時只保留匹配分類並依清單排序；完全無匹配時保留原分類。",
  },
  {
    name: "header",
    type: "object",
    description:
      "HTTP API 請求標頭；播放 Result.header 全空時也作為預設。type 3 不會自動套用到 Spider 自行發出的請求。",
  },
  { name: "style", type: "Style", description: "站點預設卡片樣式。" },
];

export const parseFields: Field[] = [
  {
    name: "name",
    type: "string",
    required: true,
    description: "解析器顯示名稱，也是具名選取依據。",
  },
  {
    name: "type",
    type: "integer",
    description: "0=WebView、1=JSON API、2=JAR JSON、3=JAR 混合、4=並行嘗試。",
    default: "0",
  },
  { name: "url", type: "string", description: "解析 API 端點或前綴。" },
  {
    name: "ext.flag",
    type: "string[]",
    description:
      "type 4 並行解析時，優先選取同類型且匹配旗標的解析器；沒有匹配時回退該類型全部解析器。",
  },
  {
    name: "ext.header",
    type: "object",
    description:
      "附加於解析請求；JSON 解析結果未提供標頭時，也作為播放請求的預設標頭。",
  },
];

export const liveFields: Field[] = [
  {
    name: "name",
    type: "string",
    required: true,
    description: "直播配置唯一名稱。",
  },
  {
    name: "url",
    type: "string",
    description: "外部 TXT、M3U 或 JSON 列表位置；與 groups 擇一。",
  },
  {
    name: "api",
    type: "string",
    description:
      "groups 為空時，用此 Spider 的 liveContent(url) 取得外部列表文字。",
  },
  {
    name: "ext",
    type: "string | object | array",
    description: "傳給直播 Spider 的初始化資料；物件與陣列會先序列化為字串。",
  },
  { name: "jar", type: "string", description: "此直播配置專用 JAR。" },
  {
    name: "click",
    type: "string",
    description: "預設在 WebView 執行的 JavaScript 點擊程式。",
  },
  {
    name: "logo",
    type: "string",
    description: "預設 Logo 範本，可用 {id}、{name}、{logo}。",
  },
  {
    name: "epg",
    type: "string",
    description: "節目表位置，逗號分隔；支援 XMLTV 與 API 範本。",
  },
  { name: "ua", type: "string", description: "預設 User-Agent。" },
  { name: "origin", type: "string", description: "預設 Origin 標頭。" },
  { name: "referer", type: "string", description: "預設 Referer 標頭。" },
  {
    name: "timeZone",
    type: "string",
    description: "節目表時區，例如 Asia/Taipei。",
    default: "系統時區",
  },
  {
    name: "keep",
    type: "string",
    description: "已保存頻道定位資訊；通常由 App 自行維護。",
  },
  {
    name: "timeout",
    type: "integer",
    description: "播放逾時秒數，最小 1。",
    default: "15",
  },
  { name: "header", type: "object", description: "直播請求附加 HTTP 標頭。" },
  {
    name: "catchup",
    type: "Catchup",
    description:
      "外部列表頻道的預設追看／時移設定；內嵌 groups 需在頻道自行提供。",
  },
  { name: "core", type: "Core", description: "特殊播放核心的進階初始化設定。" },
  { name: "groups", type: "Group[]", description: "內嵌頻道分組。" },
  {
    name: "boot",
    type: "boolean",
    description: "目前選中的直播配置為 true 時，載入完成後觸發自動進入直播。",
    default: "false",
  },
  {
    name: "pass",
    type: "boolean",
    description:
      "true 時不把 TXT／M3U 分組名稱中的 _後綴 解析成密碼；JSON 內明確設定的 Group.pass 不受影響。",
    default: "false",
  },
];

export const channelFields: Field[] = [
  {
    name: "Group.name",
    type: "string",
    required: true,
    description: "分組顯示名稱。",
  },
  {
    name: "Group.pass",
    type: "string",
    description: "分組密碼；未通過前視為隱藏。",
  },
  {
    name: "Group.channel",
    type: "Channel[]",
    required: true,
    description: "分組內頻道清單。",
  },
  {
    name: "Channel.name",
    type: "string",
    required: true,
    description: "頻道顯示名稱。",
  },
  {
    name: "Channel.urls",
    type: "string[]",
    required: true,
    description:
      "播放位置清單；未設定 drm 時可用 $線路名稱 指定標籤。設定 drm 時 URL 原樣使用，請勿附加標籤。",
  },
  { name: "Channel.number", type: "string", description: "顯示用頻道號碼。" },
  { name: "Channel.logo", type: "string", description: "覆蓋配置預設 Logo。" },
  { name: "Channel.epg", type: "string", description: "覆蓋配置預設 EPG。" },
  {
    name: "Channel.ua",
    type: "string",
    description: "覆蓋配置預設 User-Agent。",
  },
  {
    name: "Channel.click",
    type: "string",
    description: "覆蓋配置的 JavaScript 點擊程式。",
  },
  {
    name: "Channel.format",
    type: "string",
    description: "直接指定媒體 MIME type。",
  },
  { name: "Channel.origin", type: "string", description: "覆蓋 Origin。" },
  { name: "Channel.referer", type: "string", description: "覆蓋 Referer。" },
  { name: "Channel.tvgId", type: "string", description: "EPG 頻道 ID。" },
  { name: "Channel.tvgName", type: "string", description: "EPG 頻道名稱。" },
  {
    name: "Channel.header",
    type: "object",
    description:
      "頻道 HTTP 標頭；外部列表僅在整份 header 為空時繼承 Live.header，不逐鍵合併。",
  },
  {
    name: "Channel.drm",
    type: "Drm",
    description: "媒體 DRM 參數，子欄位見共用物件。",
  },
  {
    name: "Channel.parse",
    type: "integer",
    description: "0=不解析、1=解析。",
    default: "0",
  },
  {
    name: "Channel.catchup",
    type: "Catchup",
    description: "覆蓋配置的追看／時移設定。",
  },
];

export const commonFields: Field[] = [
  { name: "Doh.name", type: "string", description: "DoH 顯示名稱。" },
  { name: "Doh.url", type: "string", description: "DoH 查詢端點。" },
  { name: "Doh.ips", type: "string[]", description: "Bootstrap IP。" },
  { name: "Proxy.name", type: "string", description: "代理規則名稱。" },
  {
    name: "Proxy.hosts",
    type: "string[]",
    description: "適用 host，支援規則比對。",
  },
  {
    name: "Proxy.urls",
    type: "string[]",
    description:
      "含主機與埠號的代理位置，例如 http://127.0.0.1:8080、socks5://127.0.0.1:1080；依 http 或 socks 前綴選擇 Java 代理類型。",
  },
  { name: "Rule.name", type: "string", description: "規則顯示名稱。" },
  { name: "Rule.hosts", type: "string[]", description: "觸發目標 host。" },
  {
    name: "Rule.regex",
    type: "string[]",
    description: "有效媒體位置的擷取條件。",
  },
  {
    name: "Rule.click",
    type: "string[]",
    description:
      "WebView 中要點擊的 CSS 選擇器清單；與 JavaScript script 分開設定。",
  },
  {
    name: "Rule.script",
    type: "string[]",
    description: "WebView 中執行的腳本。",
  },
  {
    name: "Rule.exclude",
    type: "string[]",
    description: "不應被擷取的排除條件。",
  },
  { name: "Header.host", type: "string", description: "要處理的 host。" },
  {
    name: "Header.header",
    type: "object",
    description: "要注入或覆寫的 HTTP 請求標頭。",
  },
  {
    name: "Catchup.type",
    type: "string",
    description: "default 完全替換；其他值附加於原位置。",
    default: "空字串；行為為附加",
  },
  {
    name: "Catchup.days",
    type: "string",
    description: "保留欄位；目前不限制可追看的天數。",
  },
  {
    name: "Catchup.regex",
    type: "string",
    description: "判斷設定是否套用的字串或規則。",
  },
  {
    name: "Catchup.source",
    type: "string",
    description:
      "追看／時移 URL 範本，請提供非空值；可含開始與結束時間 token。",
  },
  {
    name: "Catchup.replace",
    type: "string",
    description: "附加模式使用 正規表示式,替換內容；default 模式不套用。",
  },
  {
    name: "Drm.type",
    type: "string",
    description: "媒體 DRM 類型，例如 widevine、playready、clearkey。",
  },
  {
    name: "Drm.key",
    type: "string",
    description: "DRM 請求位置或該類型接受的金鑰資料。",
  },
  { name: "Drm.header", type: "object", description: "DRM 請求標頭。" },
  {
    name: "Drm.forceKey",
    type: "boolean",
    description:
      "強制使用指定的 DRM 請求位置，不使用媒體內的預設位置；對應 forceDefaultLicenseUri。",
    default: "false",
  },
  {
    name: "Style.type",
    type: "string",
    description: "rect、oval 或 list。",
    default: "rect",
  },
  {
    name: "Style.ratio",
    type: "float",
    description:
      "寬／高；正值最大 4。未設或小於等於 0 時，oval 為 1，其餘為 0.75。",
    default: "依 type",
  },
];

export const coreFields: Field[] = [
  {
    name: "auth",
    type: "string",
    description:
      "核心授權端點；resp 取得非空內容時，App 會改用本機 /tvbus 端點。",
  },
  {
    name: "name",
    type: "string",
    description: "傳給播放核心的名稱；值也可以是回傳文字的網址。",
  },
  {
    name: "pass",
    type: "string",
    description: "傳給播放核心的密碼；值也可以是回傳文字的網址。",
  },
  {
    name: "broker",
    type: "string",
    description: "傳給播放核心的 broker 位置。",
  },
  {
    name: "domain",
    type: "string",
    description: "傳給播放核心的 domain；值也可以是回傳文字的網址。",
  },
  {
    name: "resp",
    type: "string",
    description:
      "本機 /tvbus 端點要回傳的內容，也可填回傳文字的網址；取得非空內容時取代 auth 的對外位置。",
  },
  {
    name: "sign",
    type: "string",
    description: "與 pkg 一起建立執行期 Hook；值也可以由網址取得。",
  },
  {
    name: "pkg",
    type: "string",
    description: "與 sign 一起建立執行期 Hook；值也可以由網址取得。",
  },
  {
    name: "so",
    type: "string",
    description: "核心原生函式庫位置；App 會下載到本機後載入。",
  },
  {
    name: "key",
    type: "string",
    description:
      "目前保留於 Core 配置中的識別值；現行 Core 流程沒有公開讀取方法。",
  },
  {
    name: "option",
    type: "Core.Option[]",
    description: "初始化核心前逐筆套用的額外選項。",
  },
  { name: "option[].key", type: "string", description: "選項名稱。" },
  {
    name: "option[].values",
    type: "string[]",
    description: "傳給該選項的值清單。",
  },
];

export const configFieldGroups = [
  { label: "VodConfig", href: "/config#vod", fields: vodTop },
  { label: "Site", href: "/config#site", fields: siteFields },
  { label: "Parse", href: "/config#parse", fields: parseFields },
  { label: "Live", href: "/config#live", fields: liveFields },
  { label: "Group / Channel", href: "/config#channel", fields: channelFields },
  { label: "共用物件", href: "/config#common", fields: commonFields },
  { label: "Core", href: "/config#core", fields: coreFields },
];
