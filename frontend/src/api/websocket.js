class WebSocketClient {
  constructor() {
    this.ws = null
    this.userId = ''
    this.messageHandlers = []
  }
  
  // 连接WebSocket
  connect(userId) {
    return new Promise((resolve, reject) => {
      this.userId = userId
      const wsUrl = `ws://localhost:8080/ws?userId=${userId}`
      
      this.ws = new WebSocket(wsUrl)
      
      this.ws.onopen = () => {
        console.log('WebSocket连接成功')
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
      }
    })
  }
  
  // 发送消息
  sendMessage(toUserId, content) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.error('WebSocket未连接')
      return false
    }
    
    const message = {
      fromUserId: this.userId,
      toUserId: toUserId,
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
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.messageHandlers = []
  }
}

// 导出单例
export const wsClient = new WebSocketClient()