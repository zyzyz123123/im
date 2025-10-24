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
        
        <!-- 搜索框 -->
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索消息内容"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #suffix>
              <el-icon @click="handleSearch" style="cursor: pointer;">
                <Search />
              </el-icon>
            </template>
          </el-input>
        </div>
        
        <div class="current-user">
          <el-tag type="success">{{ userStore.nickname }}</el-tag>
        </div>
        
        <!-- 标签页切换 -->
        <el-tabs v-model="activeTab" class="user-tabs">
          <el-tab-pane label="最近聊天" name="recent"></el-tab-pane>
          <el-tab-pane label="在线用户" name="online"></el-tab-pane>
          <el-tab-pane label="我的群组" name="group"></el-tab-pane>
        </el-tabs>
        
        <!-- 最近聊天列表（私聊 + 群聊）-->
        <div v-show="activeTab === 'recent'" class="online-users">
          <!-- 最近群聊 -->
          <div 
            v-for="group in recentGroups" 
            :key="'group-' + group.groupId"
            :class="['user-item', { active: chatType === 'group' && currentChatGroup === group.groupId }]"
            @click="selectGroup(group)"
          >
            <div class="user-avatar">
              <div class="avatar-circle group-avatar">群</div>
            </div>
            <div class="user-info">
              <span class="user-name">{{ group.groupName }}</span>
              <span class="group-member-count">({{ group.memberCount }}人)</span>
              <el-badge 
                v-if="unreadCount[group.groupId] > 0" 
                :value="unreadCount[group.groupId]" 
                :max="99"
                class="unread-badge"
              />
            </div>
          </div>
          
          <!-- 最近私聊 -->
          <div 
            v-for="user in recentContacts" 
            :key="'user-' + user"
            :class="['user-item', { active: chatType === 'user' && currentChatUser === user }]"
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
            v-if="recentContacts.length === 0 && recentGroups.length === 0" 
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
        
        <!-- 群组列表 -->
        <div v-show="activeTab === 'group'" class="online-users">
          <div class="create-group-btn">
            <el-button type="primary" size="small" @click="openCreateGroupDialog">
              创建群组
            </el-button>
          </div>
          
          <div 
            v-for="group in groupList" 
            :key="group.groupId"
            :class="['user-item', { active: chatType === 'group' && currentChatGroup === group.groupId }]"
            @click="selectGroup(group)"
          >
            <div class="user-avatar">
              <div class="avatar-circle group-avatar">群</div>
            </div>
            <div class="user-info">
              <span class="user-name">{{ group.groupName }}</span>
              <span class="group-member-count">({{ group.memberCount }}人)</span>
              <el-badge 
                v-if="unreadCount[group.groupId] > 0" 
                :value="unreadCount[group.groupId]" 
                :max="99"
                class="unread-badge"
              />
            </div>
          </div>
          
          <el-empty 
            v-if="groupList.length === 0" 
            description="暂无群组"
            :image-size="80"
          />
        </div>
      </div>
      
      <!-- 右侧：聊天窗口 -->
      <div class="chat-window">
        <!-- 搜索结果 -->
        <div v-if="showSearchResults" class="search-results-panel">
          <div class="search-header">
            <h3>搜索结果（{{ searchResults.length }}条）</h3>
            <el-button size="small" @click="closeSearchResults">关闭</el-button>
          </div>
          <div class="search-list">
            <div 
              v-for="msg in searchResults" 
              :key="msg.messageId"
              class="search-item"
            >
              <div class="search-item-header">
                <span class="from-user">{{ msg.fromUserId }}</span>
                <span class="to-user" v-if="msg.messageType === 1">→ {{ msg.toUserId }}</span>
                <span class="group-name" v-else>[群聊]</span>
                <span class="time">{{ formatTime(msg.createdAt) }}</span>
              </div>
              <div class="search-item-content" v-html="highlightKeyword(msg.content, searchKeyword)"></div>
            </div>
            
            <el-empty 
              v-if="searchResults.length === 0" 
              description="暂无搜索结果"
            />
          </div>
        </div>
        
        <!-- 正常聊天窗口 -->
        <div v-else-if="!currentChatUser && !currentChatGroup" class="no-chat">
          <el-empty description="请选择一个用户或群组开始聊天" />
        </div>
        
        <template v-else>
          <!-- 聊天头部 -->
          <div class="chat-header">
            <h3>{{ currentChatTitle }}</h3>
            <el-button 
              size="small" 
              type="info"
              @click="closeChat"
            >
              关闭
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
                <div class="message-sender" v-if="chatType === 'group' && msg.fromUserId !== userStore.userId">
                  {{ msg.fromUserId }}
                </div>
                <div class="message-content">
                  {{ msg.content || msg.message }}
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
    
    <!-- 创建群组对话框 -->
    <el-dialog 
      v-model="createGroupDialogVisible" 
      title="创建群组" 
      width="500px"
    >
      <el-form :model="createGroupForm" label-width="80px">
        <el-form-item label="群名称">
          <el-input v-model="createGroupForm.groupName" placeholder="请输入群名称" />
        </el-form-item>
        <el-form-item label="群描述">
          <el-input 
            v-model="createGroupForm.description" 
            type="textarea" 
            :rows="2"
            placeholder="请输入群描述(可选)"
          />
        </el-form-item>
        <el-form-item label="选择成员">
          <el-checkbox-group v-model="selectedMembers">
            <el-checkbox 
              v-for="user in onlineUsers" 
              :key="user" 
              :label="user"
            >
              {{ user }}
            </el-checkbox>
          </el-checkbox-group>
          <div v-if="onlineUsers.length === 0" style="color: #999;">
            暂无在线用户
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createGroupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateGroup">创建</el-button>
      </template>
    </el-dialog>
  </template>
  
  <script setup>
  import { ref, reactive, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
  import { useRouter } from 'vue-router'
  import { useUserStore } from '../stores/user'
  import { messageApi } from '../api/message'
  import { groupApi } from '../api/group'
  import { searchApi } from '../api/search'
  import { wsClient } from '../api/websocket'
  import { logout as logoutApi } from '../api/auth'
  import { ElMessage } from 'element-plus'
  import { Search } from '@element-plus/icons-vue'
  
  const router = useRouter()
  const userStore = useUserStore()
  
  // 状态
  const activeTab = ref('recent')  // 当前标签页：recent、online 或 group
  const onlineUsers = ref([])  // 在线用户列表
  const recentContacts = ref([])  // 最近联系人列表（私聊）
  const recentGroups = ref([])  // 最近群聊列表
  const groupList = ref([])  // 群组列表
  const currentChatUser = ref('')
  const currentChatGroup = ref('')  // 当前聊天的群组ID
  const chatType = ref('user')  // 聊天类型：user-私聊，group-群聊
  const messages = reactive({}) // { userId/groupId: [messages] }
  const unreadCount = reactive({}) // { userId/groupId: count }
  const inputMessage = ref('')
  const messageListRef = ref(null)
  
  // 创建群组对话框
  const createGroupDialogVisible = ref(false)
  const createGroupForm = reactive({
    groupName: '',
    description: '',
    memberIds: []
  })
  const selectedMembers = ref([])  // 选中的群成员
  
  // 搜索相关
  const searchKeyword = ref('')
  const searchResults = ref([])
  const showSearchResults = ref(false)
  
  // 当前聊天消息
  const currentMessages = computed(() => {
    const chatId = chatType.value === 'user' ? currentChatUser.value : currentChatGroup.value
    if (!chatId) return []
    return messages[chatId] || []
  })
  
  // 当前聊天标题
  const currentChatTitle = computed(() => {
    if (chatType.value === 'user') {
      return currentChatUser.value ? `与 ${currentChatUser.value} 聊天` : ''
    } else {
      const group = groupList.value.find(g => g.groupId === currentChatGroup.value)
      return group ? `${group.groupName}` : ''
    }
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
      
      // 加载在线用户、最近联系人和群组
      await loadOnlineUsers()
      await loadRecentContacts()
      await loadRecentGroups()
      await loadUserGroups()
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
  
  // 加载最近群聊列表
  const loadRecentGroups = async () => {
    try {
      const response = await groupApi.getUserGroups()
      recentGroups.value = response.data || []
      console.log('最近群聊列表:', recentGroups.value)
    } catch (error) {
      console.error('加载最近群聊失败:', error)
    }
  }
  
  // 加载用户的群组列表
  const loadUserGroups = async () => {
    try {
      const response = await groupApi.getUserGroups()
      groupList.value = response.data || []
      console.log('群组列表:', groupList.value)
    } catch (error) {
      console.error('加载群组列表失败:', error)
    }
  }
  
  // 选择用户（私聊）
  const selectUser = async (userId) => {
    // 关闭搜索结果（如果打开的话）
    showSearchResults.value = false
    
    chatType.value = 'user'
    currentChatUser.value = userId
    currentChatGroup.value = ''
    
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
    if (!inputMessage.value.trim()) return
    
    let success = false
    const chatId = chatType.value === 'user' ? currentChatUser.value : currentChatGroup.value
    
    if (!chatId) return
    
    // 发送私聊消息
    if (chatType.value === 'user') {
      success = wsClient.sendMessage(currentChatUser.value, inputMessage.value)
      
      if (success) {
        // 添加到本地消息列表
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
      }
    }
    // 发送群聊消息
    else if (chatType.value === 'group') {
      success = wsClient.sendGroupMessage(currentChatGroup.value, inputMessage.value)
      
      if (success) {
        // 添加到本地消息列表
        const msg = {
          fromUserId: userStore.userId,
          groupId: currentChatGroup.value,
          content: inputMessage.value,
          createdAt: new Date().toISOString()
        }
        
        if (!messages[currentChatGroup.value]) {
          messages[currentChatGroup.value] = []
        }
        messages[currentChatGroup.value].push(msg)
        
        // 更新最近群聊列表（将此群组移到最前面）
        const groupIndex = recentGroups.value.findIndex(g => g.groupId === currentChatGroup.value)
        if (groupIndex > 0) {  // 如果不在第一位，移到最前面
          const [group] = recentGroups.value.splice(groupIndex, 1)
          recentGroups.value.unshift(group)
        }
      }
    }
    
    if (success) {
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
    
    // 处理系统消息：创建群组通知
    if (message.type === 'group_created') {
      ElMessage.success(`您被邀请加入群组：${message.message}`)
      // 刷新群组列表
      loadRecentGroups()
      loadUserGroups()
      return
    }
    
    // 处理私聊消息
    if (message.type === 'chat') {
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
      
      // 更新最近联系人列表
      if (!recentContacts.value.includes(fromUser)) {
        recentContacts.value.unshift(fromUser)
      }
      
      // 如果不是当前聊天用户，增加未读数量
      if (chatType.value !== 'user' || fromUser !== currentChatUser.value) {
        if (!unreadCount[fromUser]) {
          unreadCount[fromUser] = 0
        }
        unreadCount[fromUser]++
      } else {
        // 如果是当前聊天用户，滚动到底部
        nextTick(() => scrollToBottom())
      }
    }
    
    // 处理群聊消息
    else if (message.type === 'group_chat') {
      const groupId = message.groupId
      
      console.log('收到群聊消息:', message)
      
      // 初始化消息数组
      if (!messages[groupId]) {
        messages[groupId] = []
      }
      
      // 添加消息（统一使用 content 字段）
      messages[groupId].push({
        fromUserId: message.fromUserId,
        groupId: message.groupId,
        content: message.message,  // WebSocket 的 message 字段映射为 content
        createdAt: new Date().toISOString()
      })
      
      // 更新最近群聊列表（将此群组移到最前面）
      const groupIndex = recentGroups.value.findIndex(g => g.groupId === groupId)
      if (groupIndex > -1) {
        // 已存在，移到最前面
        const [group] = recentGroups.value.splice(groupIndex, 1)
        recentGroups.value.unshift(group)
      } else {
        // 不存在，从群组列表中找到并添加
        const group = groupList.value.find(g => g.groupId === groupId)
        if (group) {
          recentGroups.value.unshift(group)
        }
      }
      
      // 如果不是当前聊天群组，增加未读数量
      if (chatType.value !== 'group' || groupId !== currentChatGroup.value) {
        if (!unreadCount[groupId]) {
          unreadCount[groupId] = 0
        }
        unreadCount[groupId]++
        console.log('群组未读消息 +1:', groupId, unreadCount[groupId])
      } else {
        // 如果是当前聊天群组，滚动到底部
        console.log('当前群组，滚动到底部')
        nextTick(() => scrollToBottom())
      }
    }
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
    
    // 获取日期部分（去掉时间）
    const dateOnly = new Date(date.getFullYear(), date.getMonth(), date.getDate())
    const nowOnly = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    
    // 计算日期差（天数）
    const dayDiff = Math.floor((nowOnly - dateOnly) / (1000 * 60 * 60 * 24))
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
  
  // 选择群组（群聊）
  const selectGroup = async (group) => {
    // 关闭搜索结果（如果打开的话）
    showSearchResults.value = false
    
    chatType.value = 'group'
    currentChatUser.value = ''
    currentChatGroup.value = group.groupId
    
    // 初始化消息数组
    if (!messages[group.groupId]) {
      messages[group.groupId] = []
    }
    
    // 清除未读数量
    unreadCount[group.groupId] = 0
    
    // 加载群聊历史消息
    try {
      const response = await messageApi.getGroupHistory(group.groupId)
      messages[group.groupId] = response.data || []
      console.log('群聊历史消息:', messages[group.groupId])
    } catch (error) {
      console.error('加载群聊历史失败:', error)
    }
    
    // 滚动到底部
    await nextTick()
    scrollToBottom()
  }
  
  // 打开创建群组对话框
  const openCreateGroupDialog = () => {
    createGroupDialogVisible.value = true
    selectedMembers.value = []
    createGroupForm.groupName = ''
    createGroupForm.description = ''
  }
  
  // 创建群组
  const handleCreateGroup = async () => {
    if (!createGroupForm.groupName.trim()) {
      ElMessage.warning('请输入群名称')
      return
    }
    
    if (selectedMembers.value.length === 0) {
      ElMessage.warning('请选择至少一个成员')
      return
    }
    
    try {
      const response = await groupApi.createGroup({
        groupName: createGroupForm.groupName,
        description: createGroupForm.description,
        memberIds: selectedMembers.value
      })
      
      ElMessage.success('创建成功')
      createGroupDialogVisible.value = false
      
      // 重新加载群组列表
      await loadRecentGroups()
      await loadUserGroups()
      
      // 自动选中新创建的群组
      const newGroup = response.data
      selectGroup(newGroup)
    } catch (error) {
      console.error('创建群组失败:', error)
      ElMessage.error('创建失败')
    }
  }
  
  // 搜索消息
  const handleSearch = async () => {
    if (!searchKeyword.value.trim()) {
      ElMessage.warning('请输入搜索关键词')
      return
    }
    
    try {
      const response = await searchApi.searchMessages(searchKeyword.value)
      searchResults.value = response.data || []
      showSearchResults.value = true
      
      if (searchResults.value.length === 0) {
        ElMessage.info('没有找到相关消息')
      } else {
        ElMessage.success(`找到 ${searchResults.value.length} 条消息`)
      }
    } catch (error) {
      console.error('搜索失败:', error)
      ElMessage.error('搜索失败')
    }
  }
  
  // 关闭搜索结果
  const closeSearchResults = () => {
    showSearchResults.value = false
    searchKeyword.value = ''
    searchResults.value = []
  }
  
  // 高亮关键词
  const highlightKeyword = (text, keyword) => {
    if (!text || !keyword) return text
    const regex = new RegExp(`(${keyword})`, 'gi')
    return text.replace(regex, '<span class="highlight">$1</span>')
  }
  
  // 关闭当前聊天
  const closeChat = () => {
    currentChatUser.value = ''
    currentChatGroup.value = ''
    chatType.value = 'user'
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
  
  /* 搜索框 */
  .search-box {
    padding: 10px 20px;
    border-bottom: 1px solid #e4e7ed;
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
  
  /* 创建群组按钮 */
  .create-group-btn {
    padding: 10px 15px;
    text-align: center;
  }
  
  /* 群组头像样式 */
  .group-avatar {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%) !important;
  }
  
  .group-member-count {
    font-size: 12px;
    color: #999;
    margin-left: 5px;
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
  
  .message-sender {
    font-size: 12px;
    color: #999;
    margin-bottom: 4px;
    padding: 0 4px;
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
  
  /* 搜索结果面板 */
  .search-results-panel {
    display: flex;
    flex-direction: column;
    height: 100%;
  }
  
  .search-header {
    padding: 20px;
    border-bottom: 1px solid #e4e7ed;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .search-header h3 {
    margin: 0;
  }
  
  .search-list {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
  }
  
  .search-item {
    padding: 15px;
    margin-bottom: 10px;
    background: white;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;
  }
  
  .search-item:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }
  
  .search-item-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
    font-size: 13px;
    color: #666;
  }
  
  .from-user {
    font-weight: 600;
    color: #409eff;
  }
  
  .to-user, .group-name {
    color: #999;
  }
  
  .time {
    margin-left: auto;
    font-size: 12px;
    color: #999;
  }
  
  .search-item-content {
    font-size: 14px;
    line-height: 1.6;
    color: #303133;
  }
  
  .highlight {
    background: #fff566;
    color: #d46b08;
    font-weight: 600;
    padding: 2px 4px;
    border-radius: 3px;
  }
  </style>