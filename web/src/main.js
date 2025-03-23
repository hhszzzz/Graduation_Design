import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'

// 导入Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 如果需要中文支持
import zhCn from 'element-plus/es/locale/lang/zh-cn'

// 将store挂载到window对象，以便在拦截器等地方使用
window.$store = store

const app = createApp(App)

// 使用Element Plus，配置中文
app.use(ElementPlus, {
  locale: zhCn
})

app.use(store)
  .use(router)
  .mount('#app')
