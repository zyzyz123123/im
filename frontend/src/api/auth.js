import axios from 'axios'
import { ElMessage } from 'element-plus'

const API_BASE_URL = 'http://localhost:8080'

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
    userId: data.userId,
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
    userId: data.userId,
    password: data.password
  })
}

/**
 * 用户登出
 */
export function logout() {
  return authAPI.post('/auth/logout')
}

