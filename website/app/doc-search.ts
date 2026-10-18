import { configFieldGroups } from "./config-fields.ts";
import { methods, networkMethods, objectGroups, resultFields } from "./spider-fields.ts";
import { features } from "./feature-content.ts";
import { playbackProperties } from "./playback-commands.ts";
import {
  localActions,
  localRemoteControls,
  localRemoteFields,
  localMediaFields,
  localDeviceFields,
  localTransferEndpoints,
} from "./local-api.ts";

type Entry = {
  title: string;
  description: string;
  href: string;
  keywords?: string;
  names?: string[];
};

const entries: Entry[] = [
  {
    title: "爬蟲載入方式",
    description: "Java JAR、Python、QuickJS 的 api 寫法",
    href: "/spider#start",
    names: ["catvod-api.jar", ":apiJar", "compileOnly"],
    keywords: "SDK 執行接口",
  },
  {
    title: "三語言 Demo",
    description: "Java、Python、JavaScript 範例",
    href: "/spider#demo",
    names: ["JSONObject", "JSONArray", "self.page", "page", "Response", "Response.page", "com.github.catvod.bean"],
    keywords: "CatVodSpider examples/python/spider.py examples/javascript/spider.js Demo.java 分頁",
  },
  {
    title: "爬蟲共用網路",
    description: "Python、QuickJS、Java 的 HTTP、Cookie、TTL 快取、批次請求與下載",
    href: "/spider#network",
    names: networkMethods.flatMap(([name]) => name.split(" / ").map((method) => `net.${method}`)).concat("self.net.cachedText"),
    keywords: networkMethods.flat().join(" ") + " self.net stale persist",
  },
  {
    title: "Node.js 載入",
    description: "index.js.md5、bundle 與 HTTP 站點路由",
    href: "/spider#node",
  },
  {
    title: "安裝爬蟲執行模組",
    description: "Python、QuickJS-NG、Node.js 獨立 runtime APK 的安裝、更新與測試",
    href: "/spider#modules",
    names: ["runtime APK", "QuickJS-NG", "模組安裝"],
    keywords: "模組 缺少 不相容 ABI 重新啟動 Thunder mpv BD-J",
  },
  {
    title: "爬蟲編輯與測試",
    description: "載入 Python、QuickJS 或 Java JAR，儲存、格式化、重測與版本還原",
    href: "/spider#testing",
    names: ["/spider.html", "爬蟲測試", "格式化", "重測"],
    keywords: "ext JSON 參數 方法 網路紀錄 上傳 來源 版本 播放結果 Java 編譯",
  },
  {
    title: "Runtime 與 Android UI",
    description: "Java、Python 的 Context、Activity 與 Toast 接口",
    href: "/spider#runtime",
    names: ["com.github.catvod.Runtime", "Runtime.context", "Runtime.activity", "Runtime.toast", "getActivity", "self.getActivity"],
    keywords: "Application Context Activity Toast Dialog 主執行緒 jclass com.github.catvod.spider.Init",
  },
  {
    title: "爬蟲 Local 設定",
    description: "按站點隔離的 JSON 設定讀寫與刪除",
    href: "/spider#network",
    names: ["self.local", "local.get", "local.set", "local.delete"],
    keywords: "Python QuickJS Java get set delete JSON 本地儲存",
  },
  {
    title: "演員與分類超連結",
    description: "用共用 link 格式連到分類與篩選",
    href: "/spider#objects",
    names: ["self.link", "link", "Json.link"],
    keywords: "vod_actor 演員 id name type_flag filter",
  },
  {
    title: "完整配置範例",
    description: "點播與直播的配置起點",
    href: "/config#examples",
  },
  {
    title: "方法與呼叫時機",
    description: "初始化、首頁、分類、搜尋、詳情、播放與代理",
    href: "/spider#lifecycle",
    names: methods.flatMap(([name]) => name.split(" / ")),
    keywords: methods.flat().join(" "),
  },
  {
    title: "片段跳過",
    description: "playerContent 的片頭、片中、片尾 skips 區間與毫秒規則",
    href: "/spider#skips",
    names: ["skips", "SkipSegment"],
    keywords: "opening middle ending start end 片頭片尾 片中跳過 自動 手動 關閉",
  },
  {
    title: "開網頁與登入",
    description: "action 回應的 web JSON、獨立視窗與多組 Cookie",
    href: "/spider#web",
    names: ["web", "action"],
    keywords: "QuickJS Python Java JAR Node.js JSON url mode view browser keys cookie 登入 全螢幕 返回",
  },
  {
    title: "Result 回傳欄位",
    description: "探索與播放資料、標頭、字幕、起點及解析參數",
    href: "/spider#result",
    names: resultFields.map((field) => field.name),
    keywords: resultFields
      .map((field) => `${field.name} ${field.type} ${field.description}`)
      .join(" "),
  },
  ...objectGroups.map((group) => ({
    title: `${group.name} 資料物件`,
    description: group.fields
      .slice(0, 3)
      .map(([name]) => name)
      .join("、"),
    href: "/spider#objects",
    names: group.fields.flatMap(([name]) => name.split(" / ")),
    keywords: group.fields.flat().join(" "),
  })),
  ...configFieldGroups.map((group) => ({
    title: `${group.label} 配置欄位`,
    description:
      group.fields
        .slice(0, 4)
        .map((field) => field.name)
        .join("、") + " 等欄位",
    href: group.href,
    names: group.fields.map((field) => field.name),
    keywords: group.fields
      .map(
        (field) =>
          `${field.name} ${field.type} ${field.description} ${field.default ?? ""}`,
      )
      .join(" "),
  })),
  {
    title: "直播列表格式",
    description: "JSON、M3U、TXT、EXTINF、genre 與節目表",
    href: "/config#formats",
  },
  {
    title: "播放分組與集數",
    description: "vod_play_from、vod_play_url 與分隔符號",
    href: "/spider#episodes",
  },
  {
    title: "Proxy 回傳",
    description: "狀態碼、Content-Type、內容、標頭、Base64 與 siteKey 路由",
    href: "/spider#proxy",
  },
  {
    title: "本地 HTTP API",
    description: "9978–9998、區域網路連線、表單與回應格式",
    href: "/local#connection",
    names: ["LOCAL.md"],
    keywords: "LOCAL.md localhost NanoHTTPD",
  },
  {
    title: "本地動作指令 /action",
    description: "播放控制、推送、搜尋、配置、字幕與彈幕",
    href: "/local#action",
    names: ["/action"],
    keywords:
      localActions.flat().join(" ") +
      " " +
      localActions.map(([name]) => `do=${name}`).join(" "),
  },
  {
    title: "裝置同步 do=sync",
    description: "history、keep、Config 物件、targets、mode 與 force",
    href: "/local#sync",
    names: ["do=sync", "targets", "configs"],
  },
  {
    title: "網頁播放遙控 /remote",
    description: "影片封面、拖曳跳轉、倍速、切換項目與單集循環",
    href: "/local#remote",
    names: ["/remote", "/remote/artwork", "artwork", "repeat_on", "repeat_off", "repeatOne", "seek", "canSeek", "canSetSpeed", "speed", "?tab=play"],
    keywords: localRemoteControls.flat().join(" ") + " " + localRemoteFields
      .map(({ name, description }) => `${name} ${description}`)
      .join(" "),
  },
  {
    title: "網頁分頁網址",
    description: "搜尋、推送、播放、字幕、彈幕、設定及檔案的語意網址",
    href: "/local#navigation",
    names: ["?tab=search", "?tab=push", "?tab=subtitles", "?tab=danmaku", "?tab=settings", "?tab=files"],
    keywords: "QR Code 數字分頁 path 目錄 返回",
  },
  {
    title: "獨立遙控器與按鍵 API",
    description: "方向、確認、返回、選單、數字鍵與長按",
    href: "/local#remote-keys",
    names: ["/remote.html", "/remote/key", "數字鍵", "長按", "holding"],
    keywords: "up down left right ok back menu 前景 焦點 遙控器 action press repeat cancel id",
  },
  {
    title: "字幕上傳與彈幕套用",
    description: "用目前 mediaKey 套用裝置字幕檔、彈幕檔或彈幕網址",
    href: "/local#remote-media",
    names: ["/remote/subtitle", "/remote/danmaku", "mediaKey"],
    keywords: "SRT SSA ASS XML 字幕上傳 409 換片 key path",
  },
  {
    title: "網頁裝置資訊",
    description: "點連線狀態查看版本、裝置、Android、SDK 與 ABI",
    href: "/local#remote-info",
    names: ["/remote/info", "連線狀態"],
    keywords: "variant buildType versionCode 離線 裝置資訊",
  },
  {
    title: "日誌下載與分享",
    description: "下載或分享日誌，含裝置資訊與完整播放網址",
    href: "/local#logs",
    names: ["/logs.html", "/log/info", "playback.url", "collection", "playbackLog", "lastPlaybackError", "下載日誌", "分享日誌", "日誌資訊 API", "取得日誌資訊", "重新讀取資訊"],
    keywords: "應用日誌 logcat 日誌資訊 日誌文字檔 512 KiB query 參數 匯出時間 狀態取樣時間 日誌筆數 缺漏 裝置命名 別名 循環播放",
  },
  {
    title: "播放狀態 /media",
    description: "播放狀態碼、位置、速率、媒體資訊與 JSON 回應",
    href: "/local#media",
    names: ["/media"],
    keywords: localMediaFields
      .map(({ name, description }) => `${name} ${description}`)
      .join(" "),
  },
  {
    title: "裝置資訊 /device",
    description: "電視與手機類型、服務位址、uuid 與 App 啟動時間",
    href: "/local#device",
    names: ["/device", "uuid"],
    keywords: localDeviceFields
      .map(({ name, description }) => `${name} ${description}`)
      .join(" "),
  },
  {
    title: "本地檔案與上傳",
    description: "瀏覽、上傳、新建、重新命名、搬移與刪除；Range 與 ETag",
    href: "/local#files",
    names: ["/file", "/upload", "/newFolder", "/rename", "/move", "/delFile", "/delFolder"],
    keywords: "重新命名 搬移 批次操作",
  },
  {
    title: "分塊續傳與裝置下載",
    description: "SHA-256 分塊上傳，與 App 裝置端下載的暫停、繼續及任務狀態",
    href: "/local#transfers",
    names: [...localTransferEndpoints.map(([path]) => path), "續傳", "裝置下載", "sha256", "revision"],
    keywords: localTransferEndpoints.flat().join(" ") + " chunkSize chunks threads offset completed failed speed",
  },
  {
    title: "本地快取 /cache",
    description: "do=get、do=set、do=del、rule 與 key 名稱空間",
    href: "/local#cache",
    names: ["/cache"],
  },
  {
    title: "本地代理與內部端點",
    description: "/proxy /parse /image /tvbus 與 App 內建控制頁",
    href: "/local#internal",
    names: ["/proxy", "/parse", "/image", "/tvbus"],
  },
  {
    title: "Exo 播放指令",
    description: "新增播放指令按鈕、支援範圍與可複製的 mpv 指令範例",
    href: "/commands#start",
    names: ["Exo", "mpv"],
    keywords: "播放指令 按鈕 Lua 腳本 mp.commandv 新增",
  },
  {
    title: "cycle-values 輪流切換",
    description: "倍速、音量、延遲、循環與縮放的順序及反向切換",
    href: "/commands#cycle-values",
    names: ["cycle-values", "!reverse"],
    keywords: "set add cycle pause 輪切 回繞 下一個 數值",
  },
  {
    title: "seek 跳轉位置",
    description: "相對、絕對、百分比與影片結束前的位置",
    href: "/commands#seek",
    names: ["seek", "absolute", "relative", "absolute-percent", "relative-percent"],
    keywords: "快轉 倒退 跳轉 秒 百分比 片長",
  },
  ...playbackProperties.map((property) => ({
    title: `${property.name} 播放指令`,
    description: property.description,
    href: `/commands#${property.name}`,
    names: [property.name, ...property.modes.map((mode) => `${mode} ${property.name}`)],
    keywords: property.values,
  })),
  ...features.map(([title, meta, copy, id]) => ({
    title,
    description: meta,
    href: id ? `/features#${id}` : "/features",
    keywords: copy,
  })),
];

const index = entries.map((entry) => ({
  entry,
  text: [entry.title, entry.description, entry.keywords ?? "", ...(entry.names ?? [])]
    .join(" ")
    .toLowerCase(),
  names: entry.names?.map((name) => name.toLowerCase()) ?? [],
}));

export function searchDocumentation(query: string) {
  const value = query.trim().toLowerCase();
  if (!value) return entries.slice(0, 4);
  return index
    .filter(({ text }) => text.includes(value))
    .sort(
      (a, b) =>
        Number(b.names.includes(value)) - Number(a.names.includes(value)),
    )
    .slice(0, 6)
    .map(({ entry }) => entry);
}
