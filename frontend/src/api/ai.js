import axios from 'axios'
import { ElMessage } from 'element-plus'

// 从环境变量读取 API 地址
// 生产环境使用相对路径（通过 Nginx 反向代理），开发环境使用 localhost
const BASE_URL = import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? 'http://localhost:8080' : '')

// 创建axios实例
const request = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,  // AI响应可能较慢，设置30秒超时
  withCredentials: true  // 携带 Cookie（Session）
})

// 响应拦截器：统一处理后端 Result 格式
request.interceptors.response.use(
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
    console.error('AI请求错误:', error)
    
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
    if (error.message.includes('timeout')) {
      ElMessage.error('AI响应超时，请稍后重试')
    } else {
      ElMessage.error(error.message || 'AI服务暂时不可用')
    }
    return Promise.reject(error)
  }
)

// AI聊天API
export const aiApi = {
  /**
   * 与AI聊天
   * @param {string} userId - 用户ID
   * @param {string} message - 用户消息
   * @param {boolean} clearHistory - 是否清空历史
   * @returns {Promise} AI响应
   */
  chat(userId, message, clearHistory = false) {
    return request.post('/ai/chat', {
      userId,
      message,
      clearHistory
    })
  },
  
  /**
   * 清空对话上下文（开始新话题，保留历史）
   * @param {string} userId - 用户ID
   */
  clearHistory(userId) {
    return request.post('/ai/clear', null, {
      params: { userId }
    })
  },
  
  /**
   * 删除所有对话记录（彻底删除）
   * @param {string} userId - 用户ID
   */
  deleteHistory(userId) {
    return request.post('/ai/delete', null, {
      params: { userId }
    })
  },
  
  /**
   * 获取对话轮次
   * @param {string} userId - 用户ID
   */
  getConversationRound(userId) {
    return request.get('/ai/round', {
      params: { userId }
    })
  },
  
  /**
   * 获取AI聊天历史
   * @param {string} userId - 用户ID
   */
  getHistory(userId) {
    return request.get('/ai/history', {
      params: { userId }
    })
  }
}

