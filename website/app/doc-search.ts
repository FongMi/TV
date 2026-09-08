import { configFieldGroups } from "./config-fields.ts";
import { methods, objectGroups, resultFields } from "./spider-fields.ts";
import { features } from "./feature-content.ts";
import {
  localActions,
  localMediaFields,
  localDeviceFields,
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
  },
  {
    title: "Node.js 載入",
    description: "index.js.md5、bundle 與 HTTP 站點路由",
    href: "/spider#node",
  },
  {
    title: "三語言 Demo",
    description: "Java、Python、JavaScript 範例",
    href: "/spider#demo",
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
    description: "/file /upload /newFolder /delFile /delFolder、Range 與 ETag",
    href: "/local#files",
    names: ["/file", "/upload", "/newFolder", "/delFile", "/delFolder"],
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
  ...features.map(([title, meta, copy]) => ({
    title,
    description: meta,
    href: "/features",
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
