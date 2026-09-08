import type { Metadata } from "next";
import { IBM_Plex_Sans, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import { assetPath, siteUrl } from "./site-config";

const plex = IBM_Plex_Sans({
  variable: "--font-plex",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
});

const jetbrains = JetBrains_Mono({
  variable: "--font-jetbrains",
  subsets: ["latin"],
});

const previewUrl = new URL(assetPath("/og.png"), siteUrl.origin).href;
const description = "影視TV 的爬蟲介接、配置字典與 App 功能指南。";

export const metadata: Metadata = {
  metadataBase: siteUrl,
  title: { default: "影視TV Developer Hub", template: "%s · 影視TV" },
  description,
  openGraph: {
    title: "影視TV Developer Hub",
    description,
    type: "website",
    images: [
      {
        url: previewUrl,
        width: 1730,
        height: 909,
        alt: "影視TV 使用與開發指南",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: "影視TV Developer Hub",
    description,
    images: [previewUrl],
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-Hant">
      <body className={`${plex.variable} ${jetbrains.variable}`}>
        {children}
      </body>
    </html>
  );
}
