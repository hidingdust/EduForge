import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // ESModule 兼容写法，消除__dirname警告
      '@': path.resolve(import.meta.dirname, './src')
    }
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      // 新增user接口代理，匹配后端/user路由
      '/user': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/images': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/mydownload': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  },
  // vite 8 默认 lightningcss 不认 Vue scoped 里的 @keyframes，暂时关闭 CSS 压缩
  build: {
    cssMinify: false
  }
})