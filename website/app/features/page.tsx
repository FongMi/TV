import type { Metadata } from "next";
import Link from "next/link";
import { DocsLayout } from "../ui";
import { features } from "../feature-content";

export const metadata: Metadata = {
  title: "App 功能",
  description:
    "影視TV 的電視與手機介面、搜尋、字幕、直播、投放與遠端控制功能。",
};

export default function FeaturesPage() {
  return (
    <DocsLayout
      title="認識影視TV"
      lead="影視TV 是一個可由使用者自行配置的 Android 影音應用程式，提供適合遙控器操作的電視版與觸控操作的手機版。App 本身不內建或提供任何內容來源。"
    >
      <div className="wrap feature-page">
        <section className="feature-grid">
          {features.map(([title, meta, copy]) => (
            <article key={title}>
              <h2>{title}</h2>
              <p className="feature-meta">{meta}</p>
              <p className="feature-copy">{copy}</p>
            </article>
          ))}
        </section>
        <section className="start-panel">
          <div>
            <p className="start-label">開始使用</p>
            <h2>第一次使用，從配置開始。</h2>
            <p>
              加入自己的 VodConfig 或 LiveConfig，App
              會依欄位載入站點、頻道、顯示樣式與網路行為。
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
