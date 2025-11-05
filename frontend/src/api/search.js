import axios from 'axios'
import { ElMessage } from 'element-plus'

// 从环境变量读取 API 地址
// 生产环境使用相对路径（通过 Nginx 反向代理），开发环境使用 localhost
const BASE_URL = import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? 'http://localhost:8080' : '')

const request = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  withCredentials: true
})

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const result = response.data
    
    if (result && typeof result.code !== 'undefined') {
      if (result.code === 200) {
        return { ...response, data: result.data }
      } else {
        ElMessage.error(result.message || '请求失败')
        return Promise.reject(new Error(result.message || '请求失败'))
      }
    }
    
    return response
  },
  (error) => {
    console.error('请求错误:', error)
    
    // 处理401未授权错误：session过期
    if (error.response && error.response.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      
      // 清除前端登录状态
      localStorage.removeItem('userId')
      localStorage.removeItem('nickname')
      localStorage.removeItem('avatar')
      localStorage.removeItem('email')
      
      // 跳转到登录页
      setTimeout(() => {
        window.location.href = '/login'
      }, 1500)
      
      return Promise.reject(new Error('未登录或登录已过期'))
    }
    
    // 处理其他网络错误
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

// 搜索 API
export const searchApi = {
  // 搜索消息
  searchMessages(keyword, groupId = null) {
    return request.get('/search/messages', {
      params: { keyword, groupId }
    })
  }
}

