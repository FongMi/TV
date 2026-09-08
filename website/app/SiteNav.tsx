"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";

export function SiteNav() {
  const pathname = usePathname().replace(/\/$/, "") || "/";
  return (
    <nav aria-label="主要導覽">
      {[
        ["/spider", "爬蟲介接"],
        ["/config", "配置字典"],
        ["/local", "本地 API"],
        ["/features", "App 功能"],
      ].map(([href, label]) => (
        <Link
          href={href}
          key={href}
          aria-current={pathname === href ? "page" : undefined}
        >
          {label}
        </Link>
      ))}
    </nav>
  );
}
