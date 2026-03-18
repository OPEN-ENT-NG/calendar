import { mergeConfig, type UserConfig } from "vite";
import sharedConfig from "./vite.config.shared";

export default mergeConfig(sharedConfig, {
  build: {
    rollupOptions: {
      input: {
        behaviours: "./src/main/resources/public/ts/behaviours.ts",
      },
      output: {
        entryFileNames: "js/[name].js",
      },
    },
  },
} satisfies UserConfig);
