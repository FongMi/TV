import type { NextConfig } from "next";
import { basePath } from "./app/site-config";

const nextConfig: NextConfig = {
  output: "export",
  trailingSlash: true,
  basePath,
  images: { unoptimized: true },
};

export default nextConfig;
