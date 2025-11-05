import axios from 'axios'
import { ElMessage } from 'element-plus'

// 从环境变量读取 API 地址
// 生产环境使用相对路径（通过 Nginx 反向代理），开发环境使用 localhost
const BASE_URL = import.meta.env.VITE_API_BASE_URL || (import.meta.env.DEV ? 'http://localhost:8080' : '')

// 创建axios实例
const request = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
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

// 消息API
export const messageApi = {
  // 查询聊天历史
  getChatHistory(userId1, userId2) {
    return request.get('/message/history', {
      params: { fromUserId: userId1, toUserId: userId2 }
    })
  },
  
  // 查询未读消息
  getUnreadMessages(userId) {
    return request.get('/message/unread', {
      params: { userId }
    })
  },
  
  // 标记已读
  markAsRead(messageId) {
    return request.get('/message/read', {
      params: { messageId }
    })
  },
  
  // 批量标记已读
  batchMarkAsRead(fromUserId, toUserId) {
    return request.post('/message/batchRead', null, {
      params: { fromUserId, toUserId }
    })
  },
  
  // 获取在线用户
  getOnlineUsers() {
    return request.get('/message/online/users')
  },
  
  // 检查用户是否在线
  checkUserOnline(userId) {
    return request.get('/message/online/check', {
      params: { userId }
    })
  },
  
  // 获取最近联系人列表
  getRecentContacts(userId) {
    return request.get('/message/contacts', {
      params: { userId }
    })
  },
  
  // 获取群聊历史消息
  getGroupHistory(groupId) {
    return request.get('/message/group/history', {
      params: { groupId }
    })
  },
  
  // 批量获取用户信息
  batchGetUserInfo(userIds) {
    return request.post('/message/users/batch', userIds)
  }
}