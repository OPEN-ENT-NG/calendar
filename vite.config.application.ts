import path = require("path");
import type { UserConfig } from "vite";

export default {
  root: path.resolve(__dirname, "./src/main/resources/public/"),

  build: {
    outDir: ".",
    sourcemap: true,
    minify: false,
    emptyOutDir: false,
    cssCodeSplit: false,

    rollupOptions: {
      input: {
        application: "./src/main/resources/public/ts/app.ts",
      },
      output: {
        name: "calendar",
        entryFileNames: "dist/[name].js",
        assetFileNames: "css/calendar[extname]",
        format: "umd",
        globals: {
          entcore: "entcore",
        },
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

  resolve: {
    extensions: [".ts", ".js"],
  },

  server: {
    port: 4200,
  },

  optimizeDeps: {
    exclude: ["entcore", "entcore-toolkit"],
  },
} satisfies UserConfig;
