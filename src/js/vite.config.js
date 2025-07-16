import { defineConfig } from "vite";
// vite.config.js
import vue from "@vitejs/plugin-vue";
import path from "path";
import { fileURLToPath, URL } from "node:url";
// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],

  resolve: {
    alias: {
      extensions: [".mjs", ".js", ".ts", ".jsx", ".tsx", ".json", ".vue"],
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
});