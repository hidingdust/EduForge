// src/utils/request.js
import axios from 'axios'

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 生产环境走nginx反向代理
  timeout: 10000
})

// 请求拦截器：每次请求自动带上token和用户ID
service.interceptors.request.use(
  config => {
    // 1. 添加 token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = token
    }

    // 2. 添加用户ID（用于后端识别当前登录用户）
    const userInfoStr = localStorage.getItem('userInfo')
    if (userInfoStr) {
      try {
        const userInfo = JSON.parse(userInfoStr)
        // 兼容多种可能的 userId 字段名
        const userId = userInfo.userId || userInfo.id || userInfo.user_id
        if (userId) {
          config.headers['X-User-Id'] = userId
        }
      } catch (e) {
        console.error('解析用户信息失败:', e)
      }
    }

    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    // response.data = {code, msg, data}
    return response.data
  },
  error => {
    // HTTP状态码错误 500 /401 /403
    if (error.response) {
      const status = error.response.status
      // token失效、未认证，清除本地token，跳转登录页
      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        // 按需跳转登录
        // window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default service