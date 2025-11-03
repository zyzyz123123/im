import axios from 'axios'
import { ElMessage } from 'element-plus'

// 从环境变量读取 API 地址
// 生产环境使用相对路径（通过 Nginx 反向代理），开发环境使用 localhost
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? 'http://localhost:8080' : '')

const authAPI = axios.create({
  baseURL: API_BASE_URL,
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true  // 携带 Cookie（Session）
})

// 响应拦截器：统一处理后端 Result 格式
authAPI.interceptors.response.use(
  (response) => {
    const result = response.data
    
    // 如果是 Result 格式 { code, message, data }
    if (result && typeof result.code !== 'undefined') {
      if (result.code === 200) {
        // 成功：直接返回 data 部分
        return { ...response, data: result.data }
      } else {
        // 失败：显示错误信息并抛出异常
        ElMessage.error(result.message || '请求失败')
        return Promise.reject(new Error(result.message || '请求失败'))
      }
    }
    
    // 如果不是 Result 格式，直接返回
    return response
  },
  (error) => {
    // 处理网络错误
    console.error('请求错误:', error)
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

/**
 * 用户注册
 */
export function register(data) {
  return authAPI.post('/auth/register', {
    // userId 由后端自动生成，不需要前端传递
    password: data.password,
    nickname: data.nickname,
    avatar: data.avatar || '',
    email: data.email || ''
  })
}

/**
 * 用户登录
 */
export function login(data) {
  return authAPI.post('/auth/login', {
    nickname: data.nickname,  // 使用昵称登录
    password: data.password
  })
}

/**
 * 用户登出
 */
export function logout() {
  return authAPI.post('/auth/logout')
}

/**
 * 更新用户信息
 */
export function updateProfile(data) {
  return authAPI.post('/auth/updateProfile', {
    userId: data.userId,
    nickname: data.nickname,
    avatar: data.avatar,
    email: data.email
  })
}

