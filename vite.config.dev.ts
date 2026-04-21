import { defineConfig, loadEnv, type ProxyOptions, type Plugin } from "vite";
import path = require("path");

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

/**
 * Maps `import { ... } from 'entcore'` to window.entcore.*, and the four
 * UMD globals (angular, moment, jquery, underscore) to their respective
 * window globals. ng-app.js installs all of these before any module runs.
 * Deep type-only imports like 'entcore/types/...' return an empty module.
 */
function entcoreGlobalsPlugin(): Plugin {
  return {
    name: "entcore-globals",
    enforce: "pre",
    resolveId(id) {
      if (id === "entcore" || id.startsWith("entcore/")) return `\0${id}`;
      if (id in UMD_GLOBALS) return `\0umd:${id}`;
    },
    load(id) {
      if (id === "\0entcore") {
        const named = ENTCORE_EXPORTS.map(
          (name) => `export const ${name} = _e.${name};`
        ).join("\n");
        return `const _e = window.entcore;\n${named}\nexport default _e;`;
      }
      if (id.startsWith("\0entcore/")) {
        return "export default {};";
      }
      if (id.startsWith("\0umd:")) {
        const name = id.slice("\0umd:".length);
        const { source, named } = UMD_GLOBALS[name];
        const exports = named
          .map((n) => `export const ${n} = _g.${n};`)
          .join("\n");
        return `const _g = ${source};\n${exports}\nexport default _g;`;
      }
    },
  };
}

/**
 * infra-front's `template.getCompletePath` builds view URLs shaped like
 * `/calendar/public/template/<view>.html` — the production-deployed layout.
 * In dev, Vite's root is already `src/main/resources/public`, so those URLs
 * don't resolve to any file and the SPA fallback returns `index.html`,
 * which AngularJS then injects via `ng-include` inside `<container>` —
 * producing infinite recursion (and re-running the `ng-app.js` <script>
 * on every cycle, which is what surfaces as "AngularJS loaded twice").
 *
 * Strip the `/calendar/public/` prefix so requests land on the local
 * files (templates, css, i18n, …). Also 404 stale `dist/` and
 * `js/behaviours*` artifacts left over from a prior `pnpm build` — those
 * would otherwise be served on top of the Vite-transformed dev modules.
 */
function appPrefixRewritePlugin(): Plugin {
  const blocked = /^\/(dist\/|js\/behaviours)/;
  return {
    name: "app-prefix-rewrite",
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (req.url?.startsWith("/calendar/public/")) {
          req.url = req.url.slice("/calendar/public".length);
        }
        if (req.url && blocked.test(req.url)) {
          res.statusCode = 404;
          res.end();
          return;
        }
        next();
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

    plugins: [entcoreGlobalsPlugin(), appPrefixRewritePlugin()],
  });
};