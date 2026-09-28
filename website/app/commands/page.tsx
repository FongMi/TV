import type { Metadata } from "next";
import { CodeBlock, DocsLayout } from "../ui";
import { CopyButton } from "../CopyButton";
import {
  playbackSections,
  playbackProperties,
  playbackExamples,
  seekExamples,
} from "../playback-commands";

export const metadata: Metadata = {
  title: "播放指令",
  description:
    "Exo 支援的 mpv 播放指令與可複製範例：跳轉、倍速、音量、延遲、循環、章節與縮放。",
};

export default function CommandsPage() {
  return (
    <DocsLayout
      title="Exo 播放指令"
      lead="複製指令，存成播放按鈕。Exo 與 mpv 都能使用。"
    >
      <div className="docs-layout wrap">
        <aside className="docs-nav">
          <b>本頁導覽</b>
          {playbackSections.map(([id, title]) => (
            <a href={`#${id}`} key={id}>
              {title}
            </a>
          ))}
        </aside>
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
          </section>

          <section id="examples">
            <h2>常用範例</h2>
            <p>按「複製」，貼到指令欄位。</p>
            <div className="table-scroll">
              <table className="method-table command-examples" aria-label="常用播放指令範例">
                <thead>
                  <tr>
                    <th scope="col">功能</th>
                    <th scope="col">指令</th>
                  </tr>
                </thead>
                <tbody>
                  {playbackExamples.map(([title, command]) => (
                    <tr key={title}>
                      <td data-label="功能">{title}</td>
                      <td data-label="指令">
                        <div className="command-example">
                          <code>{command}</code>
                          <CopyButton text={command} label={`${title}指令`} />
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section id="properties">
            <h2>支援的屬性</h2>
            <p>
              <code>set 屬性 值</code> 設定；<code>add 屬性 增量</code> 增減；
              <code>cycle-values 屬性 值1 值2</code> 輪切。名稱請用小寫。
            </p>
            <div className="table-scroll">
              <table className="method-table" aria-label="Exo 支援的播放屬性">
                <thead>
                  <tr>
                    {["屬性", "支援寫法", "數值與單位", "用途與限制"].map((title) => (
                      <th scope="col" key={title}>{title}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {playbackProperties.map((property) => (
                    <tr key={property.name} id={property.name}>
                      <td data-label="屬性"><code>{property.name}</code></td>
                      <td data-label="支援寫法">{property.modes.join("、")}</td>
                      <td data-label="數值與單位">{property.values}</td>
                      <td data-label="用途與限制">{property.description}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p>
              <code>add chapter -1</code> 跳上一章；本章已播放超過 5 秒時，先回到本章開頭。
            </p>
            <p>
              縮放設為 <code>1</code>，取消該方向的額外縮放，保留畫面比例模式。
              放大可能裁切邊緣；字幕、彈幕與控制介面大小不變。
            </p>
          </section>

          <section id="seek">
            <h2>跳轉位置</h2>
            <p>
              預設以秒計算；百分比以整部片長計算。超出範圍時停在起點或終點。
            </p>
            <div className="table-scroll">
              <table className="method-table" aria-label="跳轉指令範例">
                <thead>
                  <tr><th scope="col">指令</th><th scope="col">效果</th></tr>
                </thead>
                <tbody>
                  {seekExamples.map(([command, description]) => (
                    <tr key={command}>
                      <td data-label="指令"><code>{command}</code></td>
                      <td data-label="效果">{description}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p>
              來源需支援跳轉；百分比與從片尾倒數都需要已知片長。
            </p>
          </section>

          <section id="cycle-values">
            <h2>輪切數值</h2>
            <p>
              <code>cycle-values</code> 支援上表所有屬性，依目前值輪切，末項後回到首項。
              目前值不在清單時，從首項開始。
            </p>
            <p>
              加上 <code>!reverse</code> 反向切換；目前值不在清單時，從末項開始。
            </p>
            <CodeBlock label="指令" path="反向倍速輪切">
              {"cycle-values !reverse speed 1 1.25 1.5 2"}
            </CodeBlock>
            <p>
              任一值無效，整條指令不執行。<code>cycle</code> 僅支援 <code>pause</code>。
            </p>
          </section>

          <section id="limits">
            <h2>使用限制</h2>
            <div className="callout">
              <b>功能取決於目前影片</b>
              <span>
                未播放或影片不支援該操作時，會提示「目前播放無法執行此指令」。
              </span>
            </div>
            <p>
              Exo 只支援本頁語法，不支援分號串接、額外前綴或 seek 旗標。
            </p>
            <p>
              Lua 外掛與 <code>mpv.conf</code> 僅限 mpv，可自行匯入外掛。
            </p>
            <p>
              本頁範圍以 Exo 為準；mpv 依原生規則，音量與循環行為可能不同。
            </p>
          </section>
        </div>
      </div>
    </DocsLayout>
  );
}
