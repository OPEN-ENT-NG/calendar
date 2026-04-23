import { defineConfig } from "vite";
import path = require("path");
import { appPrefixRewritePlugin } from "./vite/plugins/appPrefixRewrite";
import { createDevProxyConfig } from "./vite/plugins/devProxy";
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
  const { headers, proxy } = createDevProxyConfig({
    mode,
    routes: [
      "/conf/public",
      "^/(?=applications-list)",
      "^/(?=assets)",
      "^/(?=theme|locale|i18n|skin)",
      "^/(?=auth|appregistry|cas|userbook|directory|communication|conversation|portal|session|timeline|workspace|infra)",
      "^/calendar/(?!public/)",
    ],
  });

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
      proxy,
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