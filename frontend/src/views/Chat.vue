<template>
    <div class="chat-container">
      <!-- 左侧：在线用户列表 -->
      <div class="user-list">
        <div class="user-list-header">
          <h3>消息</h3>
          <el-button 
            type="danger" 
            size="small" 
            @click="handleLogout"
          >
            退出
          </el-button>
        </div>
        
        <div class="current-user">
          <el-tag type="success">{{ userStore.nickname }}</el-tag>
        </div>
        
        <!-- 标签页切换 -->
        <el-tabs v-model="activeTab" class="user-tabs">
          <el-tab-pane label="最近聊天" name="recent"></el-tab-pane>
          <el-tab-pane label="在线用户" name="online"></el-tab-pane>
        </el-tabs>
        
        <!-- 最近聊天列表 -->
        <div v-show="activeTab === 'recent'" class="online-users">
          <div 
            v-for="user in recentContacts" 
            :key="user"
            :class="['user-item', { active: currentChatUser === user }]"
            @click="selectUser(user)"
          >
            <div class="user-avatar">
              <div class="avatar-circle">{{ user.charAt(0).toUpperCase() }}</div>
              <span :class="['status-dot', { online: onlineUsers.includes(user), offline: !onlineUsers.includes(user) }]"></span>
            </div>
            <div class="user-info">
              <span class="user-name">{{ user }}</span>
              <el-badge 
                v-if="unreadCount[user] > 0" 
                :value="unreadCount[user]" 
                :max="99"
                class="unread-badge"
              />
            </div>
          </div>
          
          <el-empty 
            v-if="recentContacts.length === 0" 
            description="暂无聊天记录"
            :image-size="80"
          />
        </div>
        
        <!-- 在线用户列表 -->
        <div v-show="activeTab === 'online'" class="online-users">
          <div 
            v-for="user in onlineUsers" 
            :key="user"
            :class="['user-item', { active: currentChatUser === user }]"
            @click="selectUser(user)"
          >
            <div class="user-avatar">
              <div class="avatar-circle">{{ user.charAt(0).toUpperCase() }}</div>
              <span class="status-dot online"></span>
            </div>
            <div class="user-info">
              <span class="user-name">{{ user }}</span>
              <el-badge 
                v-if="unreadCount[user] > 0" 
                :value="unreadCount[user]" 
                :max="99"
                class="unread-badge"
              />
            </div>
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
              <div class="message-bubble">
                <div class="message-content">
                  {{ msg.content }}
                </div>
                <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
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
              placeholder="输入消息（Enter 发送，Shift+Enter 换行）"
              @keydown.enter="handleEnterKey"
            />
            <el-button 
              type="primary" 
              @click="sendMessage"
              :disabled="!inputMessage.trim()"
            >
              发送 (Enter)
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
  import { logout as logoutApi } from '../api/auth'
  import { ElMessage } from 'element-plus'
  
  const router = useRouter()
  const userStore = useUserStore()
  
  // 状态
  const activeTab = ref('recent')  // 当前标签页：recent 或 online
  const onlineUsers = ref([])  // 在线用户列表
  const recentContacts = ref([])  // 最近联系人列表
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
      // 连接WebSocket（使用 Session 鉴权，Cookie 会自动发送）
      await wsClient.connect(userStore.userId)
      ElMessage.success('连接成功')
      
      // 监听消息
      wsClient.onMessage(handleReceiveMessage)
      
      // 加载在线用户和最近联系人
      await loadOnlineUsers()
      await loadRecentContacts()
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
      // 拦截器已自动提取 Result.data
      const users = response.data || []
      
      // 过滤掉自己
      onlineUsers.value = users.filter(u => u !== userStore.userId)
      
      console.log('在线用户列表:', onlineUsers.value)
    } catch (error) {
      console.error('加载在线用户失败:', error)
    }
  }
  
  // 加载最近联系人列表
  const loadRecentContacts = async () => {
    try {
      const response = await messageApi.getRecentContacts(userStore.userId)
      // 拦截器已自动提取 Result.data
      recentContacts.value = response.data || []
      
      console.log('最近联系人列表:', recentContacts.value)
    } catch (error) {
      console.error('加载最近联系人失败:', error)
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
      
      // 拦截器已自动提取 Result.data
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
  
  // 处理 Enter 键
  const handleEnterKey = (event) => {
    // 如果按了 Shift+Enter，允许换行（不阻止默认行为）
    if (event.shiftKey) {
      return
    }
    
    // 否则阻止默认换行，发送消息
    event.preventDefault()
    sendMessage()
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
    // 处理系统消息：用户上线
    if (message.type === 'user_online') {
      const newUserId = message.fromUserId
      if (!onlineUsers.value.includes(newUserId) && newUserId !== userStore.userId) {
        onlineUsers.value.push(newUserId)
        ElMessage.success(`${newUserId} 上线了`)
        console.log('用户上线:', newUserId)
      }
      return
    }
    
    // 处理系统消息：用户下线
    if (message.type === 'user_offline') {
      const offlineUserId = message.fromUserId
      const index = onlineUsers.value.indexOf(offlineUserId)
      if (index > -1) {
        onlineUsers.value.splice(index, 1)
        ElMessage.info(`${offlineUserId} 下线了`)
        console.log('用户下线:', offlineUserId)
      }
      return
    }
    
    // 处理聊天消息
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
    
    // 更新最近联系人列表（如果不在列表中，添加到最前面）
    if (!recentContacts.value.includes(fromUser)) {
      recentContacts.value.unshift(fromUser)
    }
    
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
    
    // 计算时间差（天）
    const dayDiff = Math.floor((now - date) / (1000 * 60 * 60 * 24))
    const timeStr = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    
    if (dayDiff === 0) {
      // 今天：只显示时间
      return timeStr
    } else if (dayDiff === 1) {
      // 昨天
      return `昨天 ${timeStr}`
    } else if (dayDiff < 7) {
      // 一周内：显示星期
      const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
      return `${weekdays[date.getDay()]} ${timeStr}`
    } else if (date.getFullYear() === now.getFullYear()) {
      // 今年：月-日 时:分
      return date.toLocaleString('zh-CN', { 
        month: '2-digit', 
        day: '2-digit', 
        hour: '2-digit', 
        minute: '2-digit' 
      })
    } else {
      // 往年：年-月-日 时:分
      return date.toLocaleString('zh-CN', { 
        year: 'numeric',
        month: '2-digit', 
        day: '2-digit', 
        hour: '2-digit', 
        minute: '2-digit' 
      })
    }
  }
  
  // 退出登录
  const handleLogout = async () => {
    try {
      // 调用后端登出接口（清除 Session）
      await logoutApi()
      
      // 断开 WebSocket
      wsClient.disconnect()
      
      // 清除前端状态
      userStore.logout()
      
      // 跳转到登录页
      router.push('/login')
      
      ElMessage.success('已退出登录')
    } catch (error) {
      console.error('登出失败:', error)
      // 即使后端失败，也清除前端状态
      wsClient.disconnect()
      userStore.logout()
      router.push('/login')
    }
  }
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
  
  .user-tabs {
    padding: 0 10px;
  }
  
  .user-tabs :deep(.el-tabs__header) {
    margin: 0;
  }
  
  .user-tabs :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
  }
  
  .online-users {
    flex: 1;
    overflow-y: auto;
    padding: 10px;
  }
  
  .user-item {
    padding: 12px 15px;
    margin-bottom: 5px;
    cursor: pointer;
    border-radius: 12px;
    transition: all 0.3s;
    display: flex;
    align-items: center;
    gap: 12px;
  }
  
  .user-item:hover {
    background: #f5f7fa;
    transform: translateX(2px);
  }
  
  .user-item.active {
    background: #ecf5ff;
    color: #409eff;
  }
  
  .user-avatar {
    position: relative;
    flex-shrink: 0;
  }
  
  .avatar-circle {
    width: 45px;
    height: 45px;
    border-radius: 50%;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    font-weight: bold;
  }
  
  .status-dot {
    position: absolute;
    bottom: 2px;
    right: 2px;
    width: 12px;
    height: 12px;
    border: 2px solid white;
    border-radius: 50%;
  }
  
  .status-dot.online {
    background: #67c23a;
    animation: pulse 2s ease-in-out infinite;
  }
  
  .status-dot.offline {
    background: #909399;
  }
  
  @keyframes pulse {
    0%, 100% {
      transform: scale(1);
      opacity: 1;
    }
    50% {
      transform: scale(1.1);
      opacity: 0.8;
    }
  }
  
  .user-info {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-width: 0;
  }
  
  .user-name {
    font-size: 15px;
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .unread-badge {
    flex-shrink: 0;
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
    margin-bottom: 16px;
    max-width: 70%;
    display: flex;
    animation: messageSlideIn 0.3s ease-out;
  }
  
  @keyframes messageSlideIn {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
  
  .message-item.sent {
    margin-left: auto;
    justify-content: flex-end;
  }
  
  .message-item.received {
    margin-right: auto;
    justify-content: flex-start;
  }
  
  .message-bubble {
    display: inline-flex;
    flex-direction: column;
    max-width: 100%;
  }
  
  .message-content {
    padding: 12px 16px;
    border-radius: 18px;
    word-break: break-word;
    line-height: 1.6;
    font-size: 15px;
    position: relative;
  }
  
  .message-item.sent .message-content {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border-bottom-right-radius: 4px;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
  }
  
  .message-item.received .message-content {
    background: white;
    color: #303133;
    border-bottom-left-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
  
  .message-time {
    font-size: 11px;
    color: #909399;
    margin-top: 4px;
    padding: 0 4px;
  }
  
  .message-item.sent .message-time {
    text-align: right;
  }
  
  .message-item.received .message-time {
    text-align: left;
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