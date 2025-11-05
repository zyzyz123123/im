import { requestWithResponse as request } from './request'

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
  },
  
  /**
   * 与AI进行图文对话
   * @param {string} userId - 用户ID
   * @param {string} message - 用户消息
   * @param {string} imageUrl - 图片URL
   * @returns {Promise} AI响应
   */
  chatWithImage(userId, message, imageUrl) {
    return request.post('/ai/chat-with-image', {
      userId,
      message,
      imageUrl
    })
  },
  
  /**
   * 上传文档到通义千问，获取file_id
   * @param {File} file - 文件对象
   * @returns {Promise<string>} file_id
   */
  async uploadDocument(file) {
    const formData = new FormData()
    formData.append('file', file)
    
    const response = await request.post('/ai/upload-document', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response.data  // 返回file_id
  },
  
  /**
   * 与AI进行文档对话
   * @param {string} userId - 用户ID
   * @param {string} message - 用户消息
   * @param {string} fileId - 通义千问的file_id
   * @returns {Promise} AI响应
   */
  chatWithDocument(userId, message, fileId) {
    return request.post('/ai/chat-with-document', {
      userId,
      message,
      fileId
    })
  }
}

