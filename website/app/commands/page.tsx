import type { Metadata } from "next";
import { CodeBlock, DocsLayout } from "../ui";
import { CopyButton } from "../CopyButton";
import { DocsNav } from "../DocsNav";
import {
  playbackSections,
  playbackProperties,
  playbackCommandGroups,
  type PlaybackCommand,
} from "../playback-commands";

export const metadata: Metadata = {
  title: "播放指令",
  description:
    "依功能查找 Exo 與 mpv 支援的播放指令：播放、音量、音軌、字幕、跳轉、章節與畫面調整。",
};

function CommandTable({ title, commands }: { title: string; commands: readonly PlaybackCommand[] }) {
  return (
    <div className="table-scroll">
      <table className="method-table command-examples" aria-label={title + "指令"}>
        <thead>
          <tr><th scope="col">功能</th><th scope="col">指令</th><th scope="col">說明</th></tr>
        </thead>
        <tbody>
          {commands.map(([name, command, description]) => (
            <tr key={command}>
              <td data-label="功能">{name}</td>
              <td data-label="指令">
                <div className="command-example">
                  <code>{command}</code>
                  <CopyButton text={command} label={name + "指令"} />
                </div>
              </td>
              <td data-label="說明">{description}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function CommandsPage() {
  return (
    <DocsLayout title="播放指令" lead="依功能找到需要的指令，複製後存成播放按鈕。">
      <div className="docs-layout wrap">
        <DocsNav items={playbackSections} />
        <div className="docs-content">
          <section id="start">
            <h2>新增指令按鈕</h2>
            <ol>
              <li>播放時，開啟控制列的「指令」。</li>
              <li>按「新增」，填入名稱、貼上指令。</li>
              <li>儲存後，按一下即可執行。</li>
            </ol>
            <p>
              mpv：進入「腳本」→「按鈕」→「新增」→「新增指令按鈕」。
              每個按鈕填一條指令，不需加上 <code>mp.commandv(...)</code>。
            </p>
            <p>各區塊會標示支援的播放器；表內指令可直接複製。</p>
          </section>

          {playbackCommandGroups.map((group) => (
            <section id={group.id} key={group.id}>
              {group.id === "playback" && <><span id="examples" /><span id="properties" /></>}
              <h2>{group.title}</h2>
              <p><b>支援：{group.support}。</b> {group.description}</p>
              <CommandTable title={group.title} commands={group.commands} />
              {playbackProperties.filter((property) => group.properties.includes(property.name)).map((property) => (
                <p id={property.name} key={property.name}>
                  <code>{property.name}</code>：{property.values}。支援 {property.modes.join("、")}。
                  {property.description}
                </p>
              ))}
              {group.notes.map((note) => <p key={note}>{note}</p>)}
            </section>
          ))}

          <section id="cycle-values">
            <h2>輪切語法</h2>
            <p>
              <code>cycle-values 屬性 值1 值2</code> 依目前值輪切，末項後回到首項。
              Exo 支援各區塊列出的共用屬性；目前值不在清單時，從首項開始。
            </p>
            <p>
              加上 <code>!reverse</code> 反向切換；目前值不在清單時，從末項開始。
            </p>
            <CodeBlock label="指令" path="反向倍速輪切">
              {"cycle-values !reverse speed 1 1.25 1.5 2"}
            </CodeBlock>
            <p>
              Exo 會先檢查整份清單，任一值無效就不執行。
              Exo 的 <code>cycle</code> 支援 <code>pause</code>、<code>aid</code> 與 <code>audio</code>。
              音軌可加上 <code>up</code> 或 <code>down</code> 指定方向。屬性名稱請用小寫。
            </p>
          </section>

          <section id="limits">
            <h2>使用限制</h2>
            <div className="callout">
              <b>功能取決於目前影片</b>
              <span>未播放或影片不支援該操作時，會提示「目前播放無法執行此指令」。</span>
            </div>
            <p>Exo 不支援分號串接、額外前綴或 seek 旗標。</p>
            <p>Lua 外掛與 <code>mpv.conf</code> 僅限 mpv，可自行匯入外掛。</p>
            <p>共用屬性的數值範圍以 Exo 為準；mpv 依原生規則，音量與循環行為可能不同。</p>
          </section>
        </div>
      </div>
    </DocsLayout>
  );
}