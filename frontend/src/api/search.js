import axios from 'axios'
import { ElMessage } from 'element-plus'

// 从环境变量读取 API 地址
const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

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

