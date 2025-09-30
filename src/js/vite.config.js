import { fileURLToPath, URL } from 'node:url';
import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig(({ command }) => ({
  base: '/solrwayback/',
  build: {
    rollupOptions: {
      input: {
        custom: 'solrwayback_index_page.html'
      }
    }
  },
  root: '.',
  server: command === 'serve'
    ? {
        open: 'solrwayback_index_page.html',
      proxy: {
          '^/solrwayback/services': {
            target: 'http://localhost:8080',
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/solrwayback\/services/, '/solrwayback/services'),
           },
          '/services': {
            target: 'http://localhost:8080',
            changeOrigin: true,
            rewrite: (path) => {
              const newPath = path.replace(/^\/?services/, '/solrwayback/services');
              return newPath;
            },
          },
        },
      }
    : undefined,
  preview: {
    open: 'solrwayback_index_page.html',
  },
  plugins: [vue()],
  resolve: {
    alias: {
      extensions: [".mjs", ".js", ".ts", ".jsx", ".tsx", ".json", ".vue"],
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
}));
