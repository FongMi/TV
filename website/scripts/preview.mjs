// Local preview of exported files only; never falls back to an SPA shell.
import { createServer } from "node:http";
import { readFile, stat } from "node:fs/promises";
import { extname, resolve, sep } from "node:path";
import { basePath } from "../app/site-config.ts";

const root = resolve("out");
const types = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css",
  ".js": "text/javascript",
  ".json": "application/json",
  ".txt": "text/plain; charset=utf-8",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".woff2": "font/woff2",
};
const server = createServer(async (request, response) => {
  try {
    const url = new URL(request.url, "http://localhost");
    if (basePath && url.pathname === basePath) {
      response.writeHead(301, { Location: basePath + "/" + url.search }).end();
      return;
    }
    if (!url.pathname.startsWith(basePath + "/")) {
      response.writeHead(404).end("Not found");
      return;
    }
    let file = resolve(
      root,
      "." + decodeURIComponent(url.pathname.slice(basePath.length)),
    );
    if (file !== root && !file.startsWith(root + sep)) {
      response.writeHead(403).end("Forbidden");
      return;
    }
    if ((await stat(file)).isDirectory()) {
      if (!url.pathname.endsWith("/")) {
        response
          .writeHead(301, { Location: url.pathname + "/" + url.search })
          .end();
        return;
      }
      file = resolve(file, "index.html");
    }
    const body = await readFile(file);
    response.writeHead(200, {
      "Content-Type": types[extname(file)] || "application/octet-stream",
      "Cache-Control": "no-store",
    });
    response.end(request.method === "HEAD" ? undefined : body);
  } catch (error) {
    response
      .writeHead(
        error.code === "ENOENT" || error.code === "ENOTDIR" ? 404 : 500,
      )
      .end("Unable to serve file");
  }
});

server.listen(3000, "127.0.0.1", () =>
  console.log(`Static preview: http://localhost:3000${basePath}/`),
);
