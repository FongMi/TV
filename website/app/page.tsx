import Image from "next/image";
import Link from "next/link";
import { assetPath } from "./site-config";
import { QuickSearch } from "./QuickSearch";
import { CodeBlock, Footer, Header } from "./ui";

const tracks = [
  {
    title: "開發爬蟲",
    copy: "用 Java、Python 或 JavaScript 範例，開始開發。",
    href: "/spider",
    action: "爬蟲介接",
  },
  {
    title: "維護配置",
    copy: "從範例設定點播、直播、節目表與網路。",
    href: "/config",
    action: "配置字典",
  },
  {
    title: "使用影視TV",
    copy: "了解搜尋、播放、字幕與個人記錄。",
    href: "/features",
    action: "App 功能",
  },
];

export default function Home() {
  return (
    <div className="site-shell" id="top">
      <Header />
      <main id="main-content">
        <section className="hero wrap">
          <div className="hero-copy">
            <p className="hero-brand">影視TV</p>
            <h1>
              你的影音體驗，
              <br />
              從這裡開始。
            </h1>
            <p className="hero-lead">
              認識功能、設定來源，開始開發自己的爬蟲。
            </p>
            <div className="hero-actions">
              <Link className="button primary" href="/config#examples">
                開始配置 <span aria-hidden="true">→</span>
              </Link>
              <Link className="button" href="/features">
                認識 App
              </Link>
            </div>
            <p className="hero-note">Android TV · Android 手機</p>
          </div>
          <div className="hero-identity" aria-hidden="true">
            <Image
              src={assetPath("/logo.svg")}
              alt=""
              width={256}
              height={256}
              priority
            />
          </div>
        </section>
        <section className="track-section wrap" aria-label="文件入口">
          <div className="track-grid">
            {tracks.map((track) => (
              <Link className="track-card" href={track.href} key={track.href}>
                <h2>{track.title}</h2>
                <p>{track.copy}</p>
                <span className="track-action">
                  {track.action}
                  <span aria-hidden="true">→</span>
                </span>
              </Link>
            ))}
          </div>
        </section>
        <section className="search-section wrap">
          <div>
            <p className="section-label">查找文件</p>
            <h2>快速找到需要的說明</h2>
            <p>搜尋 API、配置欄位或 App 功能。</p>
          </div>
          <QuickSearch />
        </section>
        <section className="contract-section wrap">
          <div>
            <p className="section-label">開發者起點</p>
            <h2>三種語言，共用一份資料格式。</h2>
            <p>
              爬蟲取得資料，App 負責呈現。依 Result 格式回傳分類、列表與播放資訊。
            </p>
          </div>
          <CodeBlock label="JSON / Result" path="homeContent">{`{
  "class": [
    { "type_id": "movie", "type_name": "電影" }
  ],
  "list": [
    {
      "vod_id": "demo-1",
      "vod_name": "範例項目",
      "vod_pic": "https://example.com/poster.jpg",
      "vod_remarks": "示意"
    }
  ]
}`}</CodeBlock>
          <Link className="text-link" href="/spider#result">
            查看完整回傳格式 →
          </Link>
        </section>
        <section className="source-strip wrap">
          <div>
            <h2>加入自己的內容來源</h2>
            <p>複製範例，替換成你的配置。</p>
          </div>
          <Link className="button" href="/config">
            查看配置指南 →
          </Link>
        </section>
      </main>
      <Footer />
    </div>
  );
}
