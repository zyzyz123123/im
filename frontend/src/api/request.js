import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 统一的 axios 配置
 * 开发环境：http://localhost:8080
 * 生产环境：''（相对路径，通过 Nginx 代理）
 */
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? 'http://localhost:8080' : '')

/**
 * 创建 axios 实例（用于普通 API，返回 data）
 */
export const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  withCredentials: true
})

/**
 * 创建 axios 实例（用于需要完整 response 的 API）
 */
export const requestWithResponse = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  withCredentials: true
})

/**
 * 请求拦截器
 */
request.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器（只返回 data）
 */
request.interceptors.response.use(
  (response) => {
    const res = response.data
    
    // 如果返回的是 Result 对象
    if (res.code !== undefined) {
      if (res.code === 200) {
        return res.data  // 只返回 data 字段
      } else {
        console.error('业务错误:', res.message)
        ElMessage.error(res.message || '请求失败')
        return Promise.reject(new Error(res.message || '请求失败'))
      }
    }
    
    return response
  },
  (error) => {
    console.error('响应错误:', error)
    
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
    
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

/**
 * 响应拦截器（返回完整 response）
 */
requestWithResponse.interceptors.response.use(
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
    console.error('响应错误:', error)
    
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
    
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request

