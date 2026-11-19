import { defineConfig } from '@vben/vite-config';

import ElementPlus from 'unplugin-element-plus/vite';

export default defineConfig(async () => {
  return {
    application: {},
    vite: {
      plugins: [
        ElementPlus({
          format: 'esm',
        }),
      ],
      server: {
        proxy: {
          // 后端 context-path 已改为根路径 /，本地用 /oa 作代理前缀，rewrite 去掉前缀再转发
          '/oa': {
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/oa/, ''),
            target: 'http://127.0.0.1:8091',
            ws: true,
          },
        },
      },
    },
  };
});
