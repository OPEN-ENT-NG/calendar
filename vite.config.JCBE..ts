/// <reference types="vitest/config" />
import { resolve } from "node:path";
import { defineConfig, loadEnv, ProxyOptions } from "vite";
import tsconfigPaths from "vite-tsconfig-paths";

// https://vitejs.dev/config/
export default ({ mode }: { mode: string }) => {
  // Checking environement files
  const envFile = loadEnv(mode, process.cwd(), ["vite"]);
  const envs = { ...process.env, ...envFile };
  const hasEnvFile = Object.keys(envFile).length;

  // Proxy variables
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
            proxyReq.setHeader("X-XSRF-TOKEN", envs.VITE_XSRF_TOKEN || "");
          });
        },
      }
    : {
        target: "http://localhost:8090",
        changeOrigin: false,
      };

  // When VITE_MOCK is true, bypass proxy for i18n and actualites endpoints to let MSW handle them
  const isMockMode = envs.VITE_MOCK === "true";

  // Common proxy configuration
  const commonProxyConfig = {
    "/applications-list": proxyObj,
    "/conf/public": proxyObj,
    "^/(?=help-1d|help-2d)": proxyObj,
    "^/(?=assets)": proxyObj,
    "^/(?=theme|locale|i18n|skin)": proxyObj,
    "^/(?=auth|appregistry|cas|userbook|directory|communication|conversation|portal|session|timeline|workspace|infra)":
      proxyObj,
    "/explorer": proxyObj,
  };

  // In mock mode, don't proxy /actualites and /i18n - MSW will handle them
  const proxyConfig = isMockMode
    ? commonProxyConfig
    : {
        ...commonProxyConfig,
        "/actualites": proxyObj,
      };

  return defineConfig({
    base: mode === "production" ? "/calendar" : "",
    root: __dirname,
    cacheDir: "./node_modules/.vite/calendar",

    resolve: {
      alias: {
        "@images": resolve(
          __dirname,
          "node_modules/@edifice.io/bootstrap/dist/images"
        ),
      },
    },

    server: {
      fs: {
        /**
         * Allow the server to access the node_modules folder (for the images)
         * This is a solution to allow the server to access the images and fonts of the bootstrap package for 1D theme
         */
        allow: ["../../"],
      },
      proxy: proxyConfig,
      port: 4200,
      headers,
      host: "localhost",
    },

    preview: {
      port: 4300,
      headers,
      host: "localhost",
    },

    plugins: [tsconfigPaths()],

    optimizeDeps: {
      include: [
        /*"vite-tsconfig-paths"*/
      ],
    },

    build: {
      outDir: "./dist",
      sourcemap: true,
      emptyOutDir: true,
      reportCompressedSize: true,
      commonjsOptions: {
        transformMixedEsModules: true,
        include: [/*/vite-tsconfig-paths/, */ /node_modules/],
      },
      assetsDir: "public",
      chunkSizeWarningLimit: 4000,
      rollupOptions: {
        input: {
          application: "./src/main/resources/public/ts/app.ts",
          behaviours: "./src/main/resources/public/ts/behaviours.ts",
        },
        output: {
          entryFileNames: "[name].js",
          inlineDynamicImports: true,
        },
        external: [
          "entcore/entcore",
          "entcore",
          "moment",
          "underscore",
          "jquery",
          "angular",
        ],
      },
    },

    // test: {
    //   watch: false,
    //   globals: true,
    //   environment: "jsdom",
    //   include: ["src/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}"],
    //   setupFiles: ["./src/mocks/setup.ts"],
    //   reporters: ["default"],
    //   coverage: {
    //     reportsDirectory: "./coverage/actualites",
    //     provider: "v8",
    //   },
    //   server: {
    //     deps: {
    //       inline: ["@edifice.io/react"],
    //     },
    //   },
    // },
  });
};
