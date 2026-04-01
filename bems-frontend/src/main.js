import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// 引入 Element Plus 及其样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 引入 Element Plus 的图标
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)

// 全局注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(router) // 挂载路由
app.use(ElementPlus) // 挂载 UI 库
app.mount('#app')