import type { Metadata } from "next";
import Link from "next/link";
import { DocsLayout } from "../ui";
import { features } from "../feature-content";

export const metadata: Metadata = {
  title: "App 功能",
  description:
    "影視TV 的電視與手機介面、Android Auto、獨立模組、爬蟲測試、遠端控制與檔案傳輸功能。",
};

export default function FeaturesPage() {
  return (
    <DocsLayout
      title="認識影視TV"
      lead="為 Android 電視與手機設計，加入自己的來源即可開始使用。"
    >
      <div className="wrap feature-page">
        <section className="feature-grid">
          {features.map(([title, meta, copy, id]) => (
            <article key={title} id={id}>
              <h2>{title}</h2>
              <p className="feature-meta">{meta}</p>
              <p className="feature-copy">{copy}</p>
            </article>
          ))}
        </section>
        <section className="start-panel">
          <div>
            <p className="start-label">自訂播放控制</p>
            <h2>常用操作，一鍵執行。</h2>
            <p>
              用指令按鈕調整倍速、音量、延遲與影片縮放。
            </p>
          </div>
          <Link className="button" href="/commands">
            查看播放指令 <span aria-hidden="true">→</span>
          </Link>
        </section>
        <section className="start-panel">
          <div>
            <p className="start-label">開始使用</p>
            <h2>第一次使用，從配置開始。</h2>
            <p>
              加入點播或直播配置，載入自己的站點與頻道。
            </p>
          </div>
          <Link className="button primary" href="/config">
            查看配置字典 <span>→</span>
          </Link>
        </section>
      </div>
    </DocsLayout>
  );
}
