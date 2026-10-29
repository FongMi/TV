export type PlaybackCommand = readonly [title: string, command: string, description: string];

type CommandGroup = {
  id: string;
  title: string;
  support: string;
  description: string;
  properties: readonly string[];
  commands: readonly PlaybackCommand[];
  notes: readonly string[];
};

export const playbackProperties = [
  {
    name: "pause",
    modes: ["set", "cycle-values", "cycle"],
    values: "yes 暫停；no 播放",
    description: "cycle pause 切換播放／暫停。",
  },
  {
    name: "speed",
    modes: ["set", "add", "cycle-values"],
    values: "0.1～5 倍；1 為原速",
    description: "add 超出範圍時取上下限。",
  },
  {
    name: "volume",
    modes: ["set", "add", "cycle-values"],
    values: "0～100；0 靜音",
    description: "播放器音量，系統音量不變；add 超出範圍時取上下限。",
  },
  {
    name: "sub-delay",
    modes: ["set", "add", "cycle-values"],
    values: "秒，可用小數；0 清除",
    description: "主字幕：正值延後、負值提前；副字幕不變。",
  },
  {
    name: "audio-delay",
    modes: ["set", "add", "cycle-values"],
    values: "秒，可用小數；0 清除",
    description: "聲音：正值延後、負值提前。",
  },
  {
    name: "loop-file",
    modes: ["set", "cycle-values"],
    values: "inf 或 yes 開啟；no 關閉",
    description: "單片無限循環，不支援指定次數或整份清單。",
  },
  {
    name: "chapter",
    modes: ["set", "add", "cycle-values"],
    values: "從 0 起算；add 僅接受 1、-1",
    description: "需有章節；set chapter 2 跳第三章。",
  },
  {
    name: "video-scale-x",
    modes: ["set", "cycle-values"],
    values: "0 < 倍率 ≤ 10000；1 取消縮放",
    description: "寬度倍率；1.15 加寬 15%。",
  },
  {
    name: "video-scale-y",
    modes: ["set", "cycle-values"],
    values: "0 < 倍率 ≤ 10000；1 取消縮放",
    description: "高度倍率；1.15 增高 15%。",
  },
  {
    name: "video-aspect-override",
    modes: ["set", "cycle-values"],
    values: "no、正數小數或寬:高，例如 16:9、4:3、2.35:1",
    description: "覆寫影片比例；no 恢復來源比例，保留額外縮放與平移。填充模式仍拉伸至播放區域。不接受 0、-1 或超出 float 範圍的比例。",
  },
] as const;

export const playbackCommandGroups: readonly CommandGroup[] = [
  {
    id: "playback",
    title: "播放與循環",
    support: "Exo、mpv",
    description: "播放、暫停、倍速與單片循環。",
    properties: ["pause", "speed", "loop-file"],
    commands: [
      ["播放／暫停", "cycle pause", "在播放與暫停之間切換。"],
      ["暫停", "set pause yes", "暫停目前影片。"],
      ["播放", "set pause no", "繼續播放。"],
      ["指定倍速", "set speed 1.5", "以 1.5 倍速播放。"],
      ["調整倍速", "add speed 0.25", "目前倍速增加 0.25。"],
      ["切換倍速", "cycle-values speed 1 1.25 1.5 2", "依序輪切，最後回到原速。"],
      ["開啟單片循環", "set loop-file inf", "重複播放目前影片。"],
      ["關閉單片循環", "set loop-file no", "恢復一般播放順序。"],
      ["切換單片循環", "cycle-values loop-file no inf", "在一般播放與單片循環之間切換。"],
    ],
    notes: [],
  },
  {
    id: "audio",
    title: "音量與同步",
    support: "Exo、mpv",
    description: "調整播放器音量與聲音同步。",
    properties: ["volume", "audio-delay"],
    commands: [
      ["指定音量", "set volume 60", "將播放器音量設為 60%。"],
      ["降低音量", "add volume -10", "目前播放器音量減少 10。"],
      ["切換音量", "cycle-values volume 30 60 100", "依序切換三個音量。"],
      ["聲音延後 0.2 秒", "set audio-delay 0.2", "設定聲音相對畫面的延遲。"],
      ["聲音提早 0.1 秒", "add audio-delay -0.1", "在目前延遲上減少 0.1 秒。"],
    ],
    notes: [],
  },
  {
    id: "track-selection",
    title: "音軌切換",
    support: "Exo、mpv",
    description: "切換音軌、關閉音訊或恢復自動選軌。",
    properties: [],
    commands: [
      ["下一音軌", "cycle aid", "也可使用 cycle aid up；循環包含關閉音訊。"],
      ["上一音軌", "cycle aid down", "反向循環，也包含關閉音訊。"],
      ["指定音軌", "set aid 2", "需先確認目前影片有 ID 為 2 的可播放音軌。"],
      ["關閉音訊", "set aid no", "停用音訊，影片仍可繼續播放。"],
      ["自動選軌", "set aid auto", "清除手動指定，依語言偏好與預設旗標重新選軌。"],
      ["只在指定音軌間切換", "cycle-values aid 2 4 3", "依列出的 ID 順序輪切，不加入關閉音訊。"],
      ["反向切換指定音軌", "cycle-values !reverse aid 2 4 3", "按 ID 清單反向輪切。"],
    ],
    notes: [
      "audio 是 aid 的別名，可用於上述語法。範例 ID 需依目前影片調整。",
      "音軌 ID 不是清單索引，可能隨影片或播放器改變。Exo 可使用不含空白或分號的字串 ID。",
      "Exo 的 cycle-values 音軌清單只接受不重複的 ID；任一 ID 不存在、無法播放或有歧義時不執行。no、auto 請使用 set aid 設定。",
    ],
  },
  {
    id: "subtitles",
    title: "字幕調整",
    support: "Exo、mpv",
    description: "調整主字幕的時間，副字幕不變。",
    properties: ["sub-delay"],
    commands: [
      ["字幕延後 0.2 秒", "set sub-delay 0.2", "設定主字幕延遲。"],
      ["字幕提早 0.1 秒", "add sub-delay -0.1", "在目前延遲上減少 0.1 秒。"],
      ["清除字幕延遲", "set sub-delay 0", "恢復原始字幕時間。"],
      ["切換字幕延遲", "cycle-values sub-delay 0 0.1 0.2", "依序輪切三個延遲值。"],
    ],
    notes: [],
  },
  {
    id: "seek",
    title: "跳轉與章節",
    support: "Exo、mpv",
    description: "跳到指定時間、影片比例或章節。",
    properties: ["chapter"],
    commands: [
      ["快轉 90 秒", "seek +90", "預設使用相對秒數。"],
      ["倒退 10 秒", "seek -10 relative", "從目前位置倒退。"],
      ["跳到第 120 秒", "seek 120 absolute", "使用從片頭起算的秒數。"],
      ["跳到結束前 10 秒", "seek -10 absolute", "負數由片尾倒數。"],
      ["跳到影片一半", "seek 50 absolute-percent", "使用整部片長的百分比。"],
      ["快轉片長的 10%", "seek 10 relative-percent", "從目前位置向後移動片長的 10%。"],
      ["跳到第三章", "set chapter 2", "章節索引從 0 起算。"],
      ["下一章", "add chapter 1", "跳到下一章。"],
      ["上一章", "add chapter -1", "本章已播放超過 5 秒時，先回到本章開頭。"],
      ["輪切前兩章", "cycle-values chapter 0 1", "影片需包含這兩個章節。"],
    ],
    notes: ["來源需支援跳轉；百分比與從片尾倒數都需要已知片長。超出時間範圍時停在起點或終點。"],
  },
  {
    id: "video",
    title: "畫面調整",
    support: "Exo、mpv",
    description: "調整影片寬高與比例。",
    properties: ["video-scale-x", "video-scale-y", "video-aspect-override"],
    commands: [
      ["加寬影片", "set video-scale-x 1.15", "寬度增加 15%。"],
      ["增高影片", "set video-scale-y 1.15", "高度增加 15%。"],
      ["切換影片高度", "cycle-values video-scale-y 1 1.15 1.3", "依序輪切高度倍率。"],
      ["指定影片比例", "set video-aspect-override 16:9", "將來源比例覆寫為 16:9。"],
      ["切換影片比例", "cycle-values video-aspect-override no 16:9 4:3 2.35:1", "在來源比例與指定比例間輪切。"],
      ["恢復來源比例", "set video-aspect-override no", "清除比例覆寫，保留額外縮放與平移。"],
    ],
    notes: ["縮放倍率設為 1，取消該方向的額外縮放，保留畫面比例模式。放大可能裁切邊緣；字幕、彈幕與控制介面大小不變。"],
  },
];

export const playbackSections = [
  ["start", "新增指令按鈕"],
  ...playbackCommandGroups.map((group) => [group.id, group.title] as const),
  ["cycle-values", "輪切語法"],
  ["limits", "使用限制"],
] as const;
