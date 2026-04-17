// src/utils/request.js
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router' // 引入你的路由

// 1. 创建 axios 实例
const request = axios.create({
    // 🚀 这里填你 Spring Boot 后端的地址和端口（通常是 8080）
    baseURL: 'http://localhost:8080/api',
    timeout: 5000 // 超时时间
})

// 2. 请求拦截器：发车前，检查有没有 Token
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('bems-token')
        if (token) {
            // 如果有，就把它放进请求头里带给后端
            config.headers['Authorization'] = token
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 3. 响应拦截器：后端回信后，第一道安检
request.interceptors.response.use(
    response => {
        const res = response.data
        // 如果后端返回的 code 是 401，说明没登录或 Token 过期
        if (res.code === 401) {
            ElMessage.error(res.msg || '登录已过期，请重新登录')
            localStorage.removeItem('bems-token')
            router.push('/login')
            return Promise.reject(new Error(res.msg || 'Error'))
        }
        return res // 一切正常，把数据交给页面
    },
    error => {
        ElMessage.error('网络连接异常，请检查后端是否启动！')
        return Promise.reject(error)
    }
)

export default request