import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import Components from 'unplugin-vue-components/vite'
import { defineConfig } from 'vite'

export default defineConfig({
  base: '/solrwayback/',
  build: {
    rollupOptions: {
      input: {
        custom: 'solrwayback_index_page.html'
      }
    }
  },
  root: '.',
  server: {
    open: 'solrwayback_index_page.html', // automatically open this in browser
  },
  preview: {
    open: 'solrwayback_index_page.html', // automatically open this in browser
  },
  plugins: [vue()],
  resolve: {
    alias: {
      extensions: [".mjs", ".js", ".ts", ".jsx", ".tsx", ".json", ".vue"],
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
});