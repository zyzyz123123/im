import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      // 代理文件上传接口
      '/file': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 代理 MinIO 文件访问
      '/minio': {
        target: 'http://localhost:9000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/minio/, '')
      }
    }
  }
})
