import axios from 'axios'
import router from '@/router' // 你的路由实例
import { ElMessage } from 'element-plus' // 想用美观提示就用这个

const request = axios.create({
    baseURL: '/api', // 转发到后端 http://localhost:8080
    timeout: 15000
})

// ======================
// 请求拦截器：自动带 token
// ======================
request.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) {
        // 关键：和后端 LoginInterceptor 取的 authorization 完全对应
        config.headers.Authorization = token
    }
    return config
})

// ======================
// 响应拦截器：捕获 401 → 强制跳登录
// ======================
request.interceptors.response.use(
    (response) => response.data,
    (error) => {
        // 🔥 核心：后端返回 401 时触发
        if (error.response?.status === 401) {
            // 1. 清除失效 token
            localStorage.removeItem('token')
            // 2. 提示用户
            ElMessage.error('登录已过期，请重新登录')
            // 3. 跳转到登录页（你登录页的路由 path 改成你自己的）
            router.push('/login')
        }
        return Promise.reject(error)
    }
)

export default request