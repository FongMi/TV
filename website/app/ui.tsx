import Link from "next/link";
import Image from "next/image";
import { SiteNav } from "./SiteNav";
import { assetPath } from "./site-config";
export { CodeBlock } from "./CodeBlock";

export function Header() {
  return (
    <>
      <a className="skip-link" href="#main-content">
        跳到主要內容
      </a>
      <header className="site-header">
        <div className="wrap header-inner">
          <Link className="brand" href="/" aria-label="影視TV 首頁">
            <Image
              src={assetPath("/logo.svg")}
              alt=""
              width={32}
              height={32}
              priority
            />
            <b>影視TV</b>
            <span className="brand-caption">使用與開發指南</span>
          </Link>
          <SiteNav />
        </div>
      </header>
    </>
  );
}

export function Footer() {
  return (
    <footer>
      <div className="wrap footer-inner">
        <Link className="brand" href="/">
          <Image src={assetPath("/logo.svg")} alt="" width={26} height={26} />
          <b>影視TV</b>
        </Link>
        <p>App 本身不內建或提供任何內容來源。</p>
        <a href="#top">回到頂端 ↑</a>
      </div>
    </footer>
  );
}

export function DocsLayout({
  title,
  lead,
  children,
}: {
  title: string;
  lead: string;
  children: React.ReactNode;
}) {
  return (
    <div className="site-shell" id="top">
      <Header />
      <main id="main-content">
        <section className="docs-hero wrap">
          <h1>{title}</h1>
          <p>{lead}</p>
        </section>
        {children}
      </main>
      <Footer />
    </div>
  );
}
