class WebSocketClient {
  constructor() {
    this.ws = null
    this.userId = ''
    this.messageHandlers = []
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000  // 3秒
    this.isManualClose = false
  }
  
  // 连接WebSocket（使用 Session 鉴权，Cookie 会自动发送）
  connect(userId) {
    return new Promise((resolve, reject) => {
      this.userId = userId
      this.isManualClose = false
      
      const wsUrl = `ws://localhost:8080/ws`
      
      this.ws = new WebSocket(wsUrl)
      
      this.ws.onopen = () => {
        console.log('WebSocket连接成功')
        this.reconnectAttempts = 0  // 重置重连次数
        resolve()
      }
      
      this.ws.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data)
          console.log('收到消息:', message)
          
          // 通知所有监听器
          this.messageHandlers.forEach(handler => handler(message))
        } catch (e) {
          console.error('解析消息失败:', e)
        }
      }
      
      this.ws.onerror = (error) => {
        console.error('WebSocket错误:', error)
        reject(error)
      }
      
      this.ws.onclose = () => {
        console.log('WebSocket连接关闭')
        
        // 如果不是手动关闭，尝试重连
        if (!this.isManualClose) {
          this.attemptReconnect()
        }
      }
    })
  }
  
  // 尝试重连
  attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('WebSocket重连失败，已达最大重连次数')
      return
    }
    
    this.reconnectAttempts++
    console.log(`正在尝试重连... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
    
    setTimeout(() => {
      this.connect(this.userId)
        .then(() => {
          console.log('重连成功')
        })
        .catch((error) => {
          console.error('重连失败:', error)
        })
    }, this.reconnectDelay)
  }
  
  // 发送私聊消息
  sendMessage(toUserId, content) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.error('WebSocket未连接')
      return false
    }
    
    const message = {
      type: 'chat',  // 私聊
      fromUserId: this.userId,
      toUserId: toUserId,
      message: content
    }
    
    this.ws.send(JSON.stringify(message))
    return true
  }
  
  // 发送群聊消息
  sendGroupMessage(groupId, content) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.error('WebSocket未连接')
      return false
    }
    
    const message = {
      type: 'group_chat',  // 群聊
      fromUserId: this.userId,
      groupId: groupId,
      message: content
    }
    
    this.ws.send(JSON.stringify(message))
    return true
  }
  
  // 监听消息
  onMessage(handler) {
    this.messageHandlers.push(handler)
  }
  
  // 移除监听器
  offMessage(handler) {
    const index = this.messageHandlers.indexOf(handler)
    if (index > -1) {
      this.messageHandlers.splice(index, 1)
    }
  }
  
  // 断开连接
  disconnect() {
    this.isManualClose = true  // 标记为手动关闭，不要重连
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.messageHandlers = []
  }
  
  // 检查连接状态
  isConnected() {
    return this.ws && this.ws.readyState === WebSocket.OPEN
  }
}

// 导出单例
export const wsClient = new WebSocketClient()