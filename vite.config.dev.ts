import { defineConfig, loadEnv, type ProxyOptions } from "vite";
import path = require("path");
import { appPrefixRewritePlugin } from "./vite/plugins/appPrefixRewrite";
import { entcoreGlobalsPlugin } from "./vite/plugins/entcoreGlobals";
import { injectAppPrefixPlugin } from "./vite/plugins/injectAppPrefix";

const publicRoot = path.resolve(__dirname, "src/main/resources/public");

// All named exports from entcore used across the codebase
const ENTCORE_EXPORTS = [
  "$", "_", "angular", "Behaviours", "Document", "idiom",
  "model", "moment", "ng", "notify", "Rights", "routes",
  "Shareable", "template", "toasts",
];

/**
 * UMD libraries that ng-app.js installs as window globals. Without these,
 * Vite's dep pre-bundler would resolve `import ... from "angular"` to the
 * angular package in node_modules and load a second AngularJS copy, causing
 * `Tried to load AngularJS more than once` and $digest infdig errors.
 *
 * `named` lists the value imports actually used at runtime; type-only imports
 * (e.g. IScope from "angular", Moment from "moment") are elided by esbuild.
 */
const UMD_GLOBALS: Record<string, { source: string; named: string[] }> = {
  angular: { source: "window.angular", named: ["extend"] },
  moment: { source: "window.entcore.moment", named: [] },
  jquery: { source: "window.entcore.$", named: [] },
  underscore: { source: "window.entcore._", named: [] },
};

const APP_NAME = "calendar";

export default ({ mode }: { mode: string }) => {
  const envFile = loadEnv(mode, process.cwd());
  const envs = { ...process.env, ...envFile };
  const hasEnvFile = Object.keys(envFile).length;

  const headers = hasEnvFile
    ? {
        "set-cookie": [
          `oneSessionId=${envs.VITE_ONE_SESSION_ID}`,
          `XSRF-TOKEN=${envs.VITE_XSRF_TOKEN}`,
          `authenticated=true`,
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
            proxyReq.setHeader("X-XSRF-TOKEN", envs.VITE_XSRF_TOKEN || "");
          });
        },
      }
    : {
        target: "http://localhost:8090",
        changeOrigin: false,
      };

  return defineConfig({
    root: publicRoot,
    appType: "spa",

    optimizeDeps: {
      exclude: ["entcore", "angular", "moment", "jquery", "underscore"],
    },

    server: {
      port: 4200,
      host: "localhost",
      headers,
      proxy: {
        "/conf/public": proxyObj,
        "^/(?=applications-list)": proxyObj,
        "^/(?=assets)": proxyObj,
        "^/(?=theme|locale|i18n|skin)": proxyObj,
        "^/(?=auth|appregistry|cas|userbook|directory|communication|conversation|portal|session|timeline|workspace|infra)":
          proxyObj,
        "^/calendar/(?!public/)": proxyObj,
      },
    },

    plugins: [
      entcoreGlobalsPlugin({
        entcoreExports: ENTCORE_EXPORTS,
        umdGlobals: UMD_GLOBALS,
      }),
      injectAppPrefixPlugin({ appName: APP_NAME }),
      appPrefixRewritePlugin({ appName: APP_NAME }),
    ],
  });
};