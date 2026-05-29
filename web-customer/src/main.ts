import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { setupVant } from './vant-plugin'
import 'vant/lib/index.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
setupVant(app)
app.mount('#app')
