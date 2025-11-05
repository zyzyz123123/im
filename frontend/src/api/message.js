import { requestWithResponse as request } from './request'

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