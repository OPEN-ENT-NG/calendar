import path = require("path");
import type { UserConfig } from "vite";

export default {
  root: path.resolve(__dirname, "./src/main/resources/public/"),

  build: {
    outDir: "./dist",
    sourcemap: true,
    minify: false,
    emptyOutDir: false,

    rollupOptions: {
      input: {
        behaviours: "./src/main/resources/public/ts/behaviours.ts",
      },
      output: {
        entryFileNames: "[name].js",
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

  optimizeDeps: {
    exclude: ["entcore", "entcore-toolkit"],
  },
} satisfies UserConfig;
