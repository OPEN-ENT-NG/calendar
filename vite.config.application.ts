import { mergeConfig, type UserConfig } from "vite";
import sharedConfig from "./vite.config.shared";

export default mergeConfig(sharedConfig, {
  build: {
    cssCodeSplit: false,

    rollupOptions: {
      input: {
        application: "./src/main/resources/public/ts/app.ts",
      },
      output: {
        name: "calendar",
        assetFileNames: "css/calendar[extname]",
      },
    },
  },
} satisfies UserConfig);
