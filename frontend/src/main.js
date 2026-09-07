// main.js
import './global.css'
import { createApp } from 'vue'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router'
import { createPinia } from 'pinia'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { useUserStore } from './stores/user'

// 1. 创建 pinia 实例
const pinia = createPinia()
const app = createApp(App)

// 2. 注册插件
app.use(pinia)      // ✅ 必须先注册 pinia
app.use(ElementPlus)
app.use(router)

// 3. 全局注册图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 4. ✅ 初始化用户状态（必须在 app.use(pinia) 之后）
const userStore = useUserStore()
const isRestored = userStore.init()
console.log(`用户状态恢复: ${isRestored ? '成功' : '未登录'}`)

// 5. 挂载应用
app.mount('#app')