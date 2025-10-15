<template>
    <div class="chat-container">
      <!-- 左侧：在线用户列表 -->
      <div class="user-list">
        <div class="user-list-header">
          <h3>在线用户</h3>
          <el-button 
            type="danger" 
            size="small" 
            @click="handleLogout"
          >
            退出
          </el-button>
        </div>
        
        <div class="current-user">
          <el-tag type="success">当前用户：{{ userStore.userId }}</el-tag>
        </div>
        
        <el-divider />
        
        <div class="online-users">
          <div 
            v-for="user in onlineUsers" 
            :key="user"
            :class="['user-item', { active: currentChatUser === user }]"
            @click="selectUser(user)"
          >
            <el-badge 
              v-if="unreadCount[user] > 0" 
              :value="unreadCount[user]" 
              class="user-badge"
            >
              <span>{{ user }}</span>
            </el-badge>
            <span v-else>{{ user }}</span>
          </div>
          
          <el-empty 
            v-if="onlineUsers.length === 0" 
            description="暂无在线用户"
            :image-size="80"
          />
        </div>
      </div>
      
      <!-- 右侧：聊天窗口 -->
      <div class="chat-window">
        <div v-if="!currentChatUser" class="no-chat">
          <el-empty description="请选择一个用户开始聊天" />
        </div>
        
        <template v-else>
          <!-- 聊天头部 -->
          <div class="chat-header">
            <h3>与 {{ currentChatUser }} 聊天</h3>
            <el-button 
              size="small" 
              @click="loadHistory"
              :loading="loadingHistory"
            >
              刷新历史
            </el-button>
          </div>
          
          <!-- 消息列表 -->
          <div class="message-list" ref="messageListRef">
            <div 
              v-for="msg in currentMessages" 
              :key="msg.id || msg.createdAt"
              :class="['message-item', msg.fromUserId === userStore.userId ? 'sent' : 'received']"
            >
              <div class="message-info">
                <span class="sender">{{ msg.fromUserId }}</span>
                <span class="time">{{ formatTime(msg.createdAt) }}</span>
              </div>
              <div class="message-content">
                {{ msg.content }}
              </div>
            </div>
            
            <el-empty 
              v-if="currentMessages.length === 0" 
              description="暂无消息"
              :image-size="100"
            />
          </div>
          
          <!-- 输入框 -->
          <div class="message-input">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              placeholder="输入消息..."
              @keydown.ctrl.enter="sendMessage"
            />
            <el-button 
              type="primary" 
              @click="sendMessage"
              :disabled="!inputMessage.trim()"
            >
              发送 (Ctrl+Enter)
            </el-button>
          </div>
        </template>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, reactive, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
  import { useRouter } from 'vue-router'
  import { useUserStore } from '../stores/user'
  import { messageApi } from '../api/message'
  import { wsClient } from '../api/websocket'
  import { ElMessage } from 'element-plus'
  
  const router = useRouter()
  const userStore = useUserStore()
  
  // 状态
  const onlineUsers = ref([])
  const currentChatUser = ref('')
  const messages = reactive({}) // { userId: [messages] }
  const unreadCount = reactive({}) // { userId: count }
  const inputMessage = ref('')
  const messageListRef = ref(null)
  const loadingHistory = ref(false)
  
  // 当前聊天消息
  const currentMessages = computed(() => {
    if (!currentChatUser.value) return []
    return messages[currentChatUser.value] || []
  })
  
  // 初始化
  onMounted(async () => {
    // 检查登录状态
    if (!userStore.checkLogin()) {
      ElMessage.error('请先登录')
      router.push('/login')
      return
    }
    
    try {
      // 连接WebSocket
      await wsClient.connect(userStore.userId)
      ElMessage.success('连接成功')
      
      // 监听消息
      wsClient.onMessage(handleReceiveMessage)
      
      // 加载在线用户
      await loadOnlineUsers()
    } catch (error) {
      console.error('初始化失败:', error)
      ElMessage.error('连接失败，请重新登录')
      router.push('/login')
    }
  })
  
  // 清理
  onUnmounted(() => {
    wsClient.disconnect()
  })
  
  // 加载在线用户列表
  const loadOnlineUsers = async () => {
    try {
      const response = await messageApi.getOnlineUsers()
      const users = response.data
      
      // 过滤掉自己
      onlineUsers.value = users.filter(u => u !== userStore.userId)
    } catch (error) {
      console.error('加载在线用户失败:', error)
    }
  }
  
  // 选择用户
  const selectUser = async (userId) => {
    currentChatUser.value = userId
    
    // 初始化消息数组
    if (!messages[userId]) {
      messages[userId] = []
    }
    
    // 清除未读数量
    unreadCount[userId] = 0
    
    // 加载历史消息
    await loadHistory()
    
    // 批量标记已读
    try {
      await messageApi.batchMarkAsRead(userStore.userId, userId)
    } catch (error) {
      console.error('标记已读失败:', error)
    }
    
    // 滚动到底部
    scrollToBottom()
  }
  
  // 加载历史消息
  const loadHistory = async () => {
    if (!currentChatUser.value) return
    
    loadingHistory.value = true
    try {
      const response = await messageApi.getChatHistory(
        userStore.userId, 
        currentChatUser.value
      )
      
      messages[currentChatUser.value] = response.data || []
      
      // 滚动到底部
      await nextTick()
      scrollToBottom()
    } catch (error) {
      console.error('加载历史消息失败:', error)
      ElMessage.error('加载历史消息失败')
    } finally {
      loadingHistory.value = false
    }
  }
  
  // 发送消息
  const sendMessage = () => {
    if (!inputMessage.value.trim() || !currentChatUser.value) return
    
    const success = wsClient.sendMessage(currentChatUser.value, inputMessage.value)
    
    if (success) {
      // 添加到本地消息列表（显示为已发送）
      const msg = {
        fromUserId: userStore.userId,
        toUserId: currentChatUser.value,
        content: inputMessage.value,
        createdAt: new Date().toISOString()
      }
      
      if (!messages[currentChatUser.value]) {
        messages[currentChatUser.value] = []
      }
      messages[currentChatUser.value].push(msg)
      
      inputMessage.value = ''
      
      // 滚动到底部
      nextTick(() => scrollToBottom())
    } else {
      ElMessage.error('发送失败，请检查连接')
    }
  }
  
  // 接收消息
  const handleReceiveMessage = (message) => {
    const fromUser = message.fromUserId
    
    // 初始化消息数组
    if (!messages[fromUser]) {
      messages[fromUser] = []
    }
    
    // 添加消息
    messages[fromUser].push({
      ...message,
      content: message.message
    })
    
    // 如果不是当前聊天用户，增加未读数量
    if (fromUser !== currentChatUser.value) {
      if (!unreadCount[fromUser]) {
        unreadCount[fromUser] = 0
      }
      unreadCount[fromUser]++
    } else {
      // 如果是当前聊天用户，滚动到底部
      nextTick(() => scrollToBottom())
    }
    
    // 播放提示音（可选）
    // playNotificationSound()
  }
  
  // 滚动到底部
  const scrollToBottom = () => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  }
  
  // 格式化时间
  const formatTime = (dateString) => {
    if (!dateString) return ''
    const date = new Date(dateString)
    const now = new Date()
    
    const isToday = date.toDateString() === now.toDateString()
    
    if (isToday) {
      return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    } else {
      return date.toLocaleString('zh-CN', { 
        month: '2-digit', 
        day: '2-digit', 
        hour: '2-digit', 
        minute: '2-digit' 
      })
    }
  }
  
  // 退出登录
  const handleLogout = () => {
    wsClient.disconnect()
    userStore.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  }
  
  // 监听在线用户变化（可选：定时刷新）
  const refreshInterval = setInterval(() => {
    loadOnlineUsers()
  }, 30000) // 每30秒刷新一次
  
  onUnmounted(() => {
    clearInterval(refreshInterval)
  })
  </script>
  
  <style scoped>
  .chat-container {
    display: flex;
    height: 100vh;
    background: #f5f7fa;
  }
  
  /* 左侧用户列表 */
  .user-list {
    width: 280px;
    background: white;
    border-right: 1px solid #e4e7ed;
    display: flex;
    flex-direction: column;
  }
  
  .user-list-header {
    padding: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #e4e7ed;
  }
  
  .user-list-header h3 {
    margin: 0;
  }
  
  .current-user {
    padding: 15px 20px;
  }
  
  .online-users {
    flex: 1;
    overflow-y: auto;
    padding: 10px;
  }
  
  .user-item {
    padding: 15px;
    margin-bottom: 5px;
    cursor: pointer;
    border-radius: 8px;
    transition: all 0.3s;
  }
  
  .user-item:hover {
    background: #f5f7fa;
  }
  
  .user-item.active {
    background: #ecf5ff;
    color: #409eff;
    font-weight: bold;
  }
  
  .user-badge {
    width: 100%;
  }
  
  /* 右侧聊天窗口 */
  .chat-window {
    flex: 1;
    display: flex;
    flex-direction: column;
    background: white;
  }
  
  .no-chat {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: center;
  }
  
  .chat-header {
    padding: 20px;
    border-bottom: 1px solid #e4e7ed;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .chat-header h3 {
    margin: 0;
  }
  
  .message-list {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    background: #f5f7fa;
  }
  
  .message-item {
    margin-bottom: 20px;
    max-width: 60%;
  }
  
  .message-item.sent {
    margin-left: auto;
  }
  
  .message-item.received {
    margin-right: auto;
  }
  
  .message-info {
    display: flex;
    justify-content: space-between;
    margin-bottom: 5px;
    font-size: 12px;
    color: #909399;
  }
  
  .message-content {
    padding: 10px 15px;
    border-radius: 8px;
    word-break: break-word;
    line-height: 1.5;
  }
  
  .message-item.sent .message-content {
    background: #409eff;
    color: white;
  }
  
  .message-item.received .message-content {
    background: white;
    border: 1px solid #e4e7ed;
  }
  
  .message-input {
    padding: 20px;
    border-top: 1px solid #e4e7ed;
    display: flex;
    gap: 10px;
  }
  
  .message-input :deep(.el-textarea) {
    flex: 1;
  }
  </style>