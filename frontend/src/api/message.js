import axios from 'axios'

// 后端地址
const BASE_URL = 'http://localhost:8080'

// 创建axios实例
const request = axios.create({
  baseURL: BASE_URL,
  timeout: 10000
})

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
    return request.get('/message/isOnline', {
      params: { userId }
    })
  }
}