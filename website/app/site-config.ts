// Build-time URL; GitHub Actions supplies the URL returned by configure-pages.
export const siteUrl = new URL(
  process.env.SITE_URL || "http://localhost:3000/",
);
export const basePath = siteUrl.pathname.replace(/\/$/, "");

export function assetPath(path: string) {
  return `${basePath}${path}`;
}
