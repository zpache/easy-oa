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
          // 后端 context-path 就是 /oa，无需 rewrite
          '/oa': {
            changeOrigin: true,
            target: 'http://127.0.0.1:8091',
            ws: true,
          },
        },
      },
    },
  };
});
