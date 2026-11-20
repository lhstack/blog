import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import App from './App.vue'
import router from './router'
import magnetic from './directives/magnetic'
import 'ant-design-vue/dist/reset.css'
import './styles/main.css'
import './styles/effects.css'
import './styles/markdown.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(Antd)
app.directive('magnetic', magnetic)
app.mount('#app')
