import { defineConfig, loadEnv, type ProxyOptions, type Plugin } from "vite";
import path = require("path");
import fs = require("fs");

const publicRoot = path.resolve(__dirname, "src/main/resources/public");

const MIME_TYPES: Record<string, string> = {
  ".js": "application/javascript",
  ".css": "text/css",
  ".html": "text/html",
  ".map": "application/json",
};

/**
 * Vite plugin that serves local built assets for /calendar/public/* requests.
 * Since this is a legacy AngularJS app using UMD bundles (not ES modules),
 * we can't use Vite's normal dev server — we serve pre-built files instead.
 */
function serveCalendarAssets(): Plugin {
  return {
    name: "serve-calendar-assets",
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const url = (req.url || "").split("?")[0];

        if (!url.startsWith("/calendar/public/")) {
          return next();
        }

        const relativePath = url.replace("/calendar/public/", "");
        const filePath = path.join(publicRoot, relativePath);

        if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
          const ext = path.extname(filePath);
          res.setHeader(
            "Content-Type",
            MIME_TYPES[ext] || "application/octet-stream"
          );
          fs.createReadStream(filePath).pipe(res);
        } else {
          next();
        }
      });
    },
  };
}

export default ({ mode }: { mode: string }) => {
  const envFile = loadEnv(mode, process.cwd());
  const envs = { ...process.env, ...envFile };
  const hasEnvFile = Object.keys(envFile).length;

  const headers = hasEnvFile
    ? {
      "set-cookie": [
        `oneSessionId=${envs.VITE_ONE_SESSION_ID}`,
        `XSRF-TOKEN=${envs.VITE_XSRF_TOKEN}`,
      ],
      "Cache-Control": "public, max-age=300",
    }
    : {};

  const proxyObj: ProxyOptions = hasEnvFile
    ? {
      target: envs.VITE_RECETTE,
      changeOrigin: true,
      headers: {
        cookie: `oneSessionId=${envs.VITE_ONE_SESSION_ID};authenticated=true; XSRF-TOKEN=${envs.VITE_XSRF_TOKEN}`,
      },
      configure: (proxy) => {
        proxy.on("proxyReq", (proxyReq) => {
          proxyReq.setHeader(
            "X-XSRF-TOKEN",
            envs.VITE_XSRF_TOKEN || ""
          );
        });
      },
    }
    : {
      target: "http://localhost:8090",
      changeOrigin: false,
    };

  return defineConfig({
    // No build, no module transformation — this is just a proxy + static server
    appType: "custom",

    server: {
      port: 4200,
      host: "localhost",
      headers,
      proxy: {
        // Proxy everything to recette, except /calendar/public/* (served locally by plugin)
        "/": {
          ...proxyObj,
          bypass(req) {
            if (req.url?.split("?")[0].startsWith("/calendar/public/")) {
              return req.url;
            }
          },
        },
      }
    },
    plugins: [serveCalendarAssets()],
  });
};
