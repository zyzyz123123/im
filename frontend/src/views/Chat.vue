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
          <div class="current-user-info">
            <img v-if="userStore.avatar" :src="userStore.avatar" class="current-user-avatar" alt="头像" />
            <div v-else class="current-user-avatar-placeholder">{{ userStore.nickname.charAt(0).toUpperCase() }}</div>
            <div class="current-user-name">
              <el-tag type="success">{{ userStore.nickname }}</el-tag>
            </div>
            <el-button 
              type="primary" 
              size="small" 
              @click="showProfileDialog = true"
              class="edit-profile-btn"
            >
              编辑资料
            </el-button>
          </div>
        </div>
        
        <!-- 标签页切换 -->
        <el-tabs v-model="activeTab" class="user-tabs">
          <el-tab-pane label="最近聊天" name="recent"></el-tab-pane>
          <el-tab-pane label="在线用户" name="online"></el-tab-pane>
          <el-tab-pane label="我的群组" name="group"></el-tab-pane>
        </el-tabs>
        
        <!-- 最近聊天列表（私聊 + 群聊）-->
        <div v-show="activeTab === 'recent'" class="online-users">
          <!-- AI智能助手（固定在最上方）-->
          <div 
            :class="['user-item', 'ai-assistant-item', { active: chatType === 'user' && currentChatUser === AI_ASSISTANT_ID }]"
            @click="selectAI"
          >
            <div class="user-avatar">
              <div class="avatar-circle ai-avatar">🤖</div>
            </div>
            <div class="user-info">
              <span class="user-name">AI智能助手</span>
              <span class="ai-tag">智能问答</span>
            </div>
          </div>
          
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
            :key="'user-' + user.userId"
            :class="['user-item', { active: chatType === 'user' && currentChatUser === user.userId }]"
            @click="selectUser(user.userId)"
          >
            <div class="user-avatar">
              <img :src="user.avatar || getUserAvatar(user.userId)" class="avatar-image" alt="头像" />
              <span :class="['status-dot', { online: isUserOnline(user.userId), offline: !isUserOnline(user.userId) }]"></span>
            </div>
            <div class="user-info">
              <span class="user-name">{{ user.nickname }}</span>
              <el-badge 
                v-if="unreadCount[user.userId] > 0" 
                :value="unreadCount[user.userId]" 
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
            :key="user.userId"
            :class="['user-item', { active: currentChatUser === user.userId }]"
            @click="selectUser(user.userId)"
          >
            <div class="user-avatar">
              <img :src="user.avatar || getUserAvatar(user.userId)" class="avatar-image" alt="头像" />
              <span class="status-dot online"></span>
            </div>
            <div class="user-info">
              <span class="user-name">{{ user.nickname }}</span>
              <el-badge 
                v-if="unreadCount[user.userId] > 0" 
                :value="unreadCount[user.userId]" 
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
                <span class="from-user">{{ getUserNickname(msg.fromUserId) }}</span>
                <span class="to-user" v-if="msg.messageType === 1">→ {{ getUserNickname(msg.toUserId) }}</span>
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
            <div class="chat-header-actions">
              <!-- AI对话特有的按钮 -->
              <template v-if="chatType === 'user' && currentChatUser === AI_ASSISTANT_ID">
                <el-button 
                  size="small" 
                  type="success"
                  :icon="Picture"
                  @click="showImageUploadDialog = true"
                >
                  发图片
                </el-button>
                <el-button 
                  size="small" 
                  type="warning"
                  :icon="Folder"
                  @click="showDocumentUploadDialog = true"
                >
                  发文档
                </el-button>
                <el-dropdown @command="handleAIAction">
                  <el-button size="small" type="primary">
                    对话管理<el-icon class="el-icon--right"><arrow-down /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="newTopic">
                        🔄 开始新话题（保留历史）
                      </el-dropdown-item>
                      <el-dropdown-item command="clearAll" divided>
                        🗑️ 删除所有记录
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
              <!-- 群聊特有的按钮 -->
              <template v-if="chatType === 'group'">
                <el-button 
                  size="small" 
                  type="primary"
                  @click="openManageMembersDialog"
                >
                  管理成员
                </el-button>
              </template>
              <el-button 
                size="small" 
                type="info"
                @click="closeChat"
              >
                关闭
              </el-button>
            </div>
          </div>
          
          <!-- 消息列表 -->
          <div class="message-list" ref="messageListRef">
            <div 
              v-for="msg in currentMessages" 
              :key="msg.id || msg.createdAt"
            >
              <!-- 对话分隔符 -->
              <div v-if="msg.type === 'divider'" class="chat-divider">
                <div class="divider-line"></div>
                <div class="divider-text">
                  <span class="divider-icon">🆕</span>
                  {{ msg.content }}
                  <span class="divider-icon">🆕</span>
                </div>
                <div class="divider-line"></div>
              </div>
              
              <!-- 普通消息 -->
              <div 
                v-else
                :class="['message-item', msg.fromUserId === userStore.userId ? 'sent' : 'received']"
              >
                <!-- 接收消息：头像在左侧 -->
                <img 
                  v-if="msg.fromUserId !== userStore.userId"
                  :src="getMessageAvatar(msg.fromUserId)"
                  class="message-avatar"
                  :alt="msg.fromUserId"
                />
                
                <div class="message-bubble">
                  <div class="message-sender" v-if="chatType === 'group' && msg.fromUserId !== userStore.userId">
                    {{ getUserNickname(msg.fromUserId) }}
                  </div>
                  
                  <!-- 文档消息（AI对话中的文档+文字）-->
                  <div v-if="msg.isDocumentMessage || (msg.content && msg.content.startsWith('{') && msg.content.includes('fileId'))" class="message-document">
                    <template v-if="parseDocumentMessage(msg.content)">
                      <div class="document-card">
                        <el-icon class="doc-icon" :size="32"><Document /></el-icon>
                        <div class="doc-info">
                          <div class="doc-filename">{{ parseDocumentMessage(msg.content).fileName }}</div>
                          <div class="doc-id">ID: {{ parseDocumentMessage(msg.content).fileId.substring(0, 20) }}...</div>
                        </div>
                      </div>
                      <div class="message-content" style="margin-top: 10px;">
                        {{ parseDocumentMessage(msg.content).text }}
                      </div>
                    </template>
                  </div>
                  
                  <!-- 图文消息（AI对话中的图片+文字）-->
                  <div v-else-if="msg.isImageMessage || (msg.content && msg.content.startsWith('{') && msg.content.includes('imageUrl'))" class="message-image-text">
                    <template v-if="parseImageMessage(msg.content)">
                      <div class="message-image">
                        <el-image
                          :src="parseImageMessage(msg.content).imageUrl"
                          :preview-src-list="[parseImageMessage(msg.content).imageUrl]"
                          fit="cover"
                          style="max-width: 300px; max-height: 300px; border-radius: 8px;"
                          lazy
                        >
                          <template #error>
                            <div class="image-error">
                              <el-icon><Picture /></el-icon>
                              <span>图片加载失败</span>
                            </div>
                          </template>
                        </el-image>
                      </div>
                      <div class="message-content" style="margin-top: 8px;">
                        {{ parseImageMessage(msg.content).text }}
                      </div>
                    </template>
                  </div>
                  
                  <!-- 普通文字消息 -->
                  <div v-else-if="!msg.messageType || msg.messageType <= 3" class="message-content">
                    {{ msg.content || msg.message }}
                  </div>
                  
                  <!-- 图片消息 -->
                  <div v-else-if="msg.messageType === 4" class="message-image">
                    <el-image
                      :src="msg.content"
                      :preview-src-list="[msg.content]"
                      fit="cover"
                      style="max-width: 300px; max-height: 300px; border-radius: 8px;"
                      lazy
                    >
                      <template #error>
                        <div class="image-error">
                          <el-icon><Picture /></el-icon>
                          <span>图片加载失败</span>
                        </div>
                      </template>
                    </el-image>
                  </div>
                  
                  <!-- 文件消息 -->
                  <div v-else-if="msg.messageType === 5" class="message-file">
                    <div class="file-card" @click="downloadFile(JSON.parse(msg.content).url)">
                      <el-icon class="file-icon"><Folder /></el-icon>
                      <div class="file-info">
                        <div class="file-name">{{ JSON.parse(msg.content).name }}</div>
                        <div class="file-size">{{ formatFileSize(JSON.parse(msg.content).size) }}</div>
                      </div>
                    </div>
                  </div>
                  
                  <div class="message-time">
                    <span v-if="msg.isPending" class="message-pending">
                      <el-icon class="is-loading"><Loading /></el-icon>
                      发送中...
                    </span>
                    <span v-else>{{ formatTime(msg.createdAt) }}</span>
                  </div>
                </div>
                
                <!-- 发送消息：头像在右侧 -->
                <img 
                  v-if="msg.fromUserId === userStore.userId"
                  :src="userStore.avatar || getUserAvatar(userStore.userId)"
                  class="message-avatar"
                  :alt="userStore.nickname"
                />
              </div>
            </div>
            
            <!-- AI正在思考（仅在AI聊天窗口显示）-->
            <div v-if="isAIThinking && chatType === 'user' && currentChatUser === AI_ASSISTANT_ID" class="message-item received">
              <img 
                :src="getMessageAvatar(AI_ASSISTANT_ID)"
                class="message-avatar"
                alt="AI助手"
              />
              <div class="message-bubble">
                <div class="message-content ai-thinking">
                  <span class="thinking-dot">●</span>
                  <span class="thinking-dot">●</span>
                  <span class="thinking-dot">●</span>
                  <span style="margin-left: 8px;">AI正在思考中...</span>
                </div>
              </div>
            </div>
            
            <el-empty 
              v-if="currentMessages.length === 0 && !(isAIThinking && chatType === 'user' && currentChatUser === AI_ASSISTANT_ID)" 
              description="暂无消息"
              :image-size="100"
            />
          </div>
          
          <!-- 输入框 -->
          <div class="message-input">
            <!-- 隐藏的图片文件输入 -->
            <input
              ref="fileInputRef"
              type="file"
              accept="image/*"
              style="display: none"
              @change="handleImageUpload"
            />
            
            <!-- 隐藏的文件输入 -->
            <input
              ref="docInputRef"
              type="file"
              style="display: none"
              @change="handleFileUpload"
            />
            
            <div class="input-actions">
              <el-button
                :icon="Picture"
                circle
                @click="handleSelectImage"
                :disabled="isUploading || isUploadingDoc"
                title="发送图片"
              />
              <el-button
                :icon="Folder"
                circle
                @click="handleSelectFile"
                :disabled="isUploading || isUploadingDoc"
                title="发送文件"
              />
              <el-button
                :icon="Emoji"
                circle
                @click="showEmojiPicker = !showEmojiPicker"
                title="选择表情"
              />
            </div>
            
            <!-- Emoji选择器 -->
            <div v-if="showEmojiPicker" class="emoji-picker">
              <div class="emoji-picker-header">
                <span>选择表情</span>
                <el-button text @click="showEmojiPicker = false">×</el-button>
              </div>
              <div class="emoji-grid">
                <span 
                  v-for="(emoji, index) in emojiList" 
                  :key="index"
                  class="emoji-item"
                  @click="insertEmoji(emoji)"
                  :title="emoji"
                >
                  {{ emoji }}
                </span>
              </div>
            </div>
            
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="3"
              placeholder="输入消息（Enter 发送，Shift+Enter 换行）"
              @keydown.enter="handleEnterKey"
              :disabled="isUploading || isUploadingDoc"
            />
            <el-button 
              type="primary" 
              @click="sendMessage"
              :disabled="isSendDisabled || isUploading || isUploadingDoc"
              :loading="isUploading || isUploadingDoc"
            >
              {{ isUploading || isUploadingDoc ? '上传中...' : '发送 (Enter)' }}
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
              :key="user.userId" 
              :label="user.userId"
            >
              {{ user.nickname }}
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
    
    <!-- 管理群成员对话框 -->
    <el-dialog 
      v-model="manageMembersDialogVisible" 
      title="管理群成员" 
      width="600px"
    >
      <div class="manage-members-content">
        <!-- 当前成员列表 -->
        <div class="member-section">
          <h4>当前成员 ({{ currentGroupMembers.length }}人)</h4>
          <div class="member-list">
            <div 
              v-for="memberId in currentGroupMembers" 
              :key="memberId"
              class="member-item"
            >
              <div class="member-info">
                <img :src="getUserAvatar(memberId)" class="member-avatar" alt="头像" />
                <span class="member-name">{{ getUserNickname(memberId) }}</span>
                <el-tag v-if="isGroupOwner(memberId)" type="danger" size="small">群主</el-tag>
              </div>
              <el-button 
                v-if="!isGroupOwner(memberId)"
                size="small" 
                type="danger" 
                @click="handleRemoveMember(memberId)"
              >
                踢出
              </el-button>
            </div>
          </div>
        </div>
        
        <!-- 添加成员 -->
        <div class="add-member-section">
          <h4>添加成员</h4>
          <div class="online-user-list">
            <div 
              v-for="user in availableUsers" 
              :key="user.userId"
              class="add-member-item"
            >
              <div class="member-info">
                <img :src="user.avatar || getUserAvatar(user.userId)" class="member-avatar" alt="头像" />
                <span class="member-name">{{ user.nickname }}</span>
              </div>
              <el-button 
                size="small" 
                type="primary" 
                @click="handleAddMember(user.userId)"
              >
                添加
              </el-button>
            </div>
            <el-empty 
              v-if="availableUsers.length === 0" 
              description="没有可添加的用户"
              :image-size="60"
            />
          </div>
        </div>
      </div>
    </el-dialog>
    
    <!-- 发送文档给AI对话框 -->
    <el-dialog 
      v-model="showDocumentUploadDialog" 
      title="发送文档给AI分析" 
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="document-upload-section">
        <!-- 文档信息显示 -->
        <div v-if="selectedDocumentForAI" class="document-info">
          <el-icon :size="48"><Document /></el-icon>
          <div class="doc-details">
            <div class="doc-name">{{ selectedDocumentForAI.name }}</div>
            <div class="doc-size">{{ formatFileSize(selectedDocumentForAI.size) }}</div>
          </div>
        </div>
        <div v-else class="document-placeholder">
          <el-icon :size="60"><Document /></el-icon>
          <p>请选择文档文件</p>
          <p class="supported-formats">支持：PDF、Word、Excel、TXT、Markdown</p>
        </div>
        
        <!-- 隐藏的文件输入 -->
        <input
          ref="aiDocInputRef"
          type="file"
          accept=".pdf,.doc,.docx,.xls,.xlsx,.txt,.md,.epub,.mobi"
          style="display: none"
          @change="handleAIDocumentSelect"
        />
        
        <!-- 选择文档按钮 -->
        <el-button 
          type="primary" 
          :icon="Upload"
          @click="aiDocInputRef.click()"
          style="margin-top: 15px; width: 100%"
        >
          选择文档
        </el-button>
        
        <!-- 问题输入 -->
        <el-input
          v-model="documentQuestion"
          type="textarea"
          :rows="3"
          placeholder="问AI关于这个文档的问题，例如：这篇文章讲了什么？帮我总结一下..."
          style="margin-top: 15px"
        />
      </div>
      
      <template #footer>
        <el-button @click="cancelDocumentUpload">取消</el-button>
        <el-button 
          type="primary" 
          @click="sendDocumentToAI"
          :loading="isSendingDocToAI"
          :disabled="!selectedDocumentForAI || !documentQuestion.trim()"
        >
          {{ isSendingDocToAI ? '发送中...' : '发送给AI' }}
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 发送图片给AI对话框 -->
    <el-dialog 
      v-model="showImageUploadDialog" 
      title="发送图片给AI分析" 
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="image-upload-section">
        <!-- 图片预览区 -->
        <div v-if="selectedImageForAI" class="image-preview">
          <img :src="selectedImagePreview" alt="预览" />
        </div>
        <div v-else class="image-placeholder">
          <el-icon :size="60"><Picture /></el-icon>
          <p>请选择图片</p>
        </div>
        
        <!-- 隐藏的文件输入 -->
        <input
          ref="aiImageInputRef"
          type="file"
          accept="image/*"
          style="display: none"
          @change="handleAIImageSelect"
        />
        
        <!-- 选择图片按钮 -->
        <el-button 
          type="primary" 
          :icon="Upload"
          @click="aiImageInputRef.click()"
          style="margin-top: 15px; width: 100%"
        >
          选择图片
        </el-button>
        
        <!-- 问题输入 -->
        <el-input
          v-model="imageQuestion"
          type="textarea"
          :rows="3"
          placeholder="问AI关于这张图片的问题，例如：这是什么？帮我分析一下这张图..."
          style="margin-top: 15px"
        />
      </div>
      
      <template #footer>
        <el-button @click="cancelImageUpload">取消</el-button>
        <el-button 
          type="primary" 
          @click="sendImageToAI"
          :loading="isSendingImageToAI"
          :disabled="!selectedImageForAI || !imageQuestion.trim()"
        >
          {{ isSendingImageToAI ? '发送中...' : '发送给AI' }}
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 编辑个人资料对话框 -->
    <el-dialog 
      v-model="showProfileDialog" 
      title="编辑个人资料" 
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="profileForm" label-width="100px">
        <el-form-item label="用户ID">
          <el-input v-model="profileForm.userId" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="头像">
          <!-- 隐藏的头像文件选择器 -->
          <input
            ref="avatarInputRef"
            type="file"
            accept="image/*"
            style="display: none"
            @change="handleAvatarUpload"
          />
          
          <div class="avatar-upload-section">
            <!-- 当前头像预览 -->
            <div class="current-avatar-preview">
              <img 
                :src="profileForm.avatar || generateAvatarUrl(profileForm.nickname, selectedProfileColor)" 
                alt="当前头像" 
              />
            </div>
            
            <!-- 上传按钮 -->
            <div class="avatar-actions">
              <el-button
                :icon="Upload"
                @click="handleSelectAvatar"
                :loading="isUploadingAvatar"
                :disabled="isUploadingAvatar"
              >
                {{ isUploadingAvatar ? '上传中...' : '上传头像' }}
              </el-button>
              <div class="upload-tip">支持 JPG、PNG，不超过2MB</div>
            </div>
          </div>
        </el-form-item>
        
        <el-form-item label="或选择颜色">
          <div class="avatar-preview-section">
            <!-- 颜色选择器 -->
            <div class="color-selector">
              <div 
                v-for="color in colorSchemes" 
                :key="color.bg"
                :class="['color-option', { selected: selectedProfileColor.bg === color.bg }]"
                :style="{ backgroundColor: '#' + color.bg }"
                @click="selectProfileColor(color)"
                :title="color.name"
              >
                <span v-if="selectedProfileColor.bg === color.bg" class="check-icon">✓</span>
              </div>
            </div>
          </div>
        </el-form-item>
        
        <el-form-item label="或输入URL">
          <el-input v-model="customAvatarUrl" placeholder="自定义头像URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showProfileDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateProfile">保存</el-button>
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
  import { logout as logoutApi, updateProfile as updateProfileApi } from '../api/auth'
  import { aiApi } from '../api/ai'
  import { fileApi } from '../api/file'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ArrowDown, Picture, Upload, Folder, Document, Loading } from '@element-plus/icons-vue'
import { IconsVue } from 'element-plus-x'
  
  const { Emoji } = IconsVue
  
  const router = useRouter()
  const userStore = useUserStore()
  
  // 状态
  const activeTab = ref('recent')  // 当前标签页：recent、online 或 group
  const onlineUsers = ref([])  // 在线用户列表 (UserInfoDTO[])
  const recentContacts = ref([])  // 最近联系人列表（私聊）(UserInfoDTO[])
  const recentGroups = ref([])  // 最近群聊列表
  const groupList = ref([])  // 群组列表
  const currentChatUser = ref('')  // 当前聊天用户的userId
  const currentChatGroup = ref('')  // 当前聊天的群组ID
  const chatType = ref('user')  // 聊天类型：user-私聊，group-群聊
  const messages = reactive({}) // { userId/groupId: [messages] }
  const unreadCount = reactive({}) // { userId/groupId: count }
  const inputMessage = ref('')
  const messageListRef = ref(null)
  const userInfoCache = reactive({}) // userId -> UserInfo 缓存
  
  // Emoji表情
  const showEmojiPicker = ref(false)
  const emojiList = [
    '😀','😃','😄','😁','😆','😅','🤣','😂','🙂','🙃',
    '😉','😊','😇','🥰','😍','🤩','😘','😗','☺️','😚',
    '😙','🥲','😋','😛','😜','🤪','😝','🤑','🤗','🤭',
    '🤫','🤔','🤐','🤨','😐','😑','😶','😏','😒','🙄',
    '😬','🤥','😌','😔','😪','🤤','😴','😷','🤒','🤕',
    '🤢','🤮','🤧','🥵','🥶','😶‍🌫️','😵','🤯','🤠','🥳',
    '😎','🤓','🧐','😕','😟','🙁','☹️','😮','😯','😲',
    '😳','🥺','😦','😧','😨','😰','😥','😢','😭','😱',
    '😖','😣','😞','😓','😩','😫','🥱','😤','😡','😠',
    '🤬','😈','👿','💀','☠️','💩','🤡','👹','👺','👻',
    '👽','👾','🤖','😺','😸','😹','😻','😼','😽','🙀',
    '😿','😾','🙈','🙉','🙊','💋','💌','💘','💝','💖',
    '💗','💓','💞','💕','💟','❣️','💔','❤️','🧡','💛',
    '💚','💙','💜','🤎','🖤','🤍','💯','💢','💥','💫',
    '💦','💨','🕳️','💣','💬','👁️','🗨️','🗯️','💭','💤',
    '👋','🤚','🖐️','✋','🖖','👌','🤏','✌️','🤞','🤟',
    '🤘','🤙','👈','👉','👆','🖕','👇','☝️','👍','👎',
    '✊','👊','🤛','🤜','👏','🙌','👐','🤲','🤝','🙏'
  ]
  
  // 创建群组对话框
  const createGroupDialogVisible = ref(false)
  const createGroupForm = reactive({
    groupName: '',
    description: '',
    memberIds: []
  })
  
  // 管理群成员对话框
  const manageMembersDialogVisible = ref(false)
  const currentGroupMembers = ref([])  // 当前群组成员ID列表
  
  // 个人资料编辑
  const showProfileDialog = ref(false)
  const profileForm = reactive({
    userId: userStore.userId,
    nickname: userStore.nickname,
    email: userStore.email || '',
    avatar: userStore.avatar || ''
  })
  const customAvatarUrl = ref('')
  
  // 颜色方案（与注册页面相同）
  const colorSchemes = [
    { bg: '667eea', fg: 'fff', name: '紫色' },
    { bg: 'f093fb', fg: 'fff', name: '粉色' },
    { bg: '4facfe', fg: 'fff', name: '蓝色' },
    { bg: '43e97b', fg: 'fff', name: '绿色' },
    { bg: 'fa709a', fg: 'fff', name: '玫红' },
    { bg: 'fee140', fg: '333', name: '黄色' },
    { bg: '30cfd0', fg: 'fff', name: '青色' },
    { bg: 'a8edea', fg: '333', name: '薄荷' }
  ]
  const selectedProfileColor = ref(colorSchemes[0])
  const selectedMembers = ref([])  // 选中的群成员
  
  // 搜索相关
  const searchKeyword = ref('')
  const searchResults = ref([])
  const showSearchResults = ref(false)
  
  // AI助手相关
  const AI_ASSISTANT_ID = 'AI_ASSISTANT'
  const isAIThinking = ref(false)
  
  // 图片上传相关
  const fileInputRef = ref(null)
  const isUploading = ref(false)
  
  // 文件上传相关
  const docInputRef = ref(null)
  const isUploadingDoc = ref(false)
  
// 头像上传相关
const avatarInputRef = ref(null)
const isUploadingAvatar = ref(false)

// AI图片上传相关
const showImageUploadDialog = ref(false)
const aiImageInputRef = ref(null)
const selectedImageForAI = ref(null)
const selectedImagePreview = ref('')
const imageQuestion = ref('')
const isSendingImageToAI = ref(false)

// AI文档上传相关
const showDocumentUploadDialog = ref(false)
const aiDocInputRef = ref(null)
const selectedDocumentForAI = ref(null)
const documentQuestion = ref('')
const isSendingDocToAI = ref(false)
  
  // 当前聊天消息
  const currentMessages = computed(() => {
    const chatId = chatType.value === 'user' ? currentChatUser.value : currentChatGroup.value
    if (!chatId) return []
    return messages[chatId] || []
  })
  
  // 当前聊天标题
  const currentChatTitle = computed(() => {
    if (chatType.value === 'user') {
      if (currentChatUser.value === AI_ASSISTANT_ID) {
        return 'AI智能助手 🤖'
      }
      const nickname = getUserNickname(currentChatUser.value)
      return currentChatUser.value ? `与 ${nickname} 聊天` : ''
    } else {
      const group = groupList.value.find(g => g.groupId === currentChatGroup.value)
      return group ? `${group.groupName}` : ''
    }
  })
  
  // 发送按钮是否禁用
  const isSendDisabled = computed(() => {
    return !inputMessage.value || inputMessage.value.trim().length === 0
  })
  
  // 可添加的用户（不在群组中的在线用户）
  const availableUsers = computed(() => {
    if (!currentChatGroup.value) return []
    return onlineUsers.value.filter(user => {
      return !currentGroupMembers.value.includes(user.userId)
    })
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
      // 先测试一个轻量级API，检查session是否有效
      await loadOnlineUsers()
      
      // session有效，继续初始化
      // 连接WebSocket（使用 Session 鉴权，Cookie 会自动发送）
      await wsClient.connect(userStore.userId)
      
      // 监听消息
      wsClient.onMessage(handleReceiveMessage)
      
      // 加载其他数据
      await loadRecentContacts()
      await loadRecentGroups()
      await loadUserGroups()
      
      // 所有初始化完成后才显示成功提示
      ElMessage.success('连接成功')
    } catch (error) {
      // 如果是未登录错误，拦截器已经处理了跳转，这里完全静默
      if (error.message === '未登录') {
        // 不打印错误，不做任何处理，静默退出
        return
      }
      
      // 只有非未登录错误才打印和处理
      console.error('初始化失败:', error)
      ElMessage.error('连接失败，请稍后重试')
      userStore.logout()
      setTimeout(() => {
        router.push('/login')
      }, 1500)
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
      onlineUsers.value = users.filter(u => u.userId !== userStore.userId)
      
      // 更新用户信息缓存
      users.forEach(user => {
        userInfoCache[user.userId] = user
      })
      
      console.log('在线用户列表:', onlineUsers.value)
    } catch (error) {
      // 如果是未登录错误，重新抛出让上层处理
      if (error.message === '未登录') {
        throw error
      }
      console.error('加载在线用户失败:', error)
    }
  }
  
  // 加载最近联系人列表
  const loadRecentContacts = async () => {
    try {
      const response = await messageApi.getRecentContacts(userStore.userId)
      // 拦截器已自动提取 Result.data
      recentContacts.value = response.data || []
      
      // 更新用户信息缓存
      recentContacts.value.forEach(user => {
        userInfoCache[user.userId] = user
      })
      
      console.log('最近联系人列表:', recentContacts.value)
    } catch (error) {
      // 如果是未登录错误，重新抛出让上层处理
      if (error.message === '未登录') {
        throw error
      }
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
      // 如果是未登录错误，重新抛出让上层处理
      if (error.message === '未登录') {
        throw error
      }
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
      // 如果是未登录错误，重新抛出让上层处理
      if (error.message === '未登录') {
        throw error
      }
      console.error('加载群组列表失败:', error)
    }
  }
  
  // 选择AI助手
  const selectAI = async () => {
    // 关闭搜索结果（如果打开的话）
    showSearchResults.value = false
    
    chatType.value = 'user'
    currentChatUser.value = AI_ASSISTANT_ID
    currentChatGroup.value = ''
    
    // 初始化AI消息数组
    if (!messages[AI_ASSISTANT_ID]) {
      messages[AI_ASSISTANT_ID] = []
    }
    
    // 清除未读数量
    unreadCount[AI_ASSISTANT_ID] = 0
    
    // 加载AI聊天历史（从数据库）
    try {
      const response = await aiApi.getHistory(userStore.userId)
      messages[AI_ASSISTANT_ID] = response.data || []
      console.log('加载AI聊天历史:', messages[AI_ASSISTANT_ID].length, '条')
    } catch (error) {
      console.error('加载AI聊天历史失败:', error)
    }
    
    // 滚动到底部
    await nextTick()
    scrollToBottom()
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
  
  // 插入emoji到输入框
  const insertEmoji = (emoji) => {
    inputMessage.value += emoji
    showEmojiPicker.value = false
  }
  
  // 触发文件选择
  const handleSelectImage = () => {
    fileInputRef.value.click()
  }
  
  // 处理图片上传
  const handleImageUpload = async (event) => {
    const file = event.target.files[0]
    if (!file) return
    
    // 验证文件类型
    if (!file.type.startsWith('image/')) {
      ElMessage.warning('请选择图片文件')
      return
    }
    
    // 验证文件大小（最大10MB）
    if (file.size > 10 * 1024 * 1024) {
      ElMessage.warning('图片大小不能超过10MB')
      return
    }
    
    try {
      isUploading.value = true
      
      // 上传图片到服务器（file.js 已经返回 URL 字符串）
      const imageUrl = await fileApi.uploadFile(file, 'image')
      
      // 发送图片消息
      await sendImageMessage(imageUrl)
      
      ElMessage.success('图片发送成功')
    } catch (error) {
      console.error('图片上传失败:', error)
      ElMessage.error('图片上传失败：' + (error.message || '未知错误'))
    } finally {
      isUploading.value = false
      // 清空文件选择
      event.target.value = ''
    }
  }
  
  // 发送图片消息
  const sendImageMessage = async (imageUrl) => {
    const chatId = chatType.value === 'user' ? currentChatUser.value : currentChatGroup.value
    if (!chatId) return
    
    let success = false
    
    // 发送私聊图片
    if (chatType.value === 'user') {
      success = wsClient.sendMessage(currentChatUser.value, imageUrl, 4) // messageType: 4 图片
      
      if (success) {
        // 添加到本地消息列表
        const msg = {
          fromUserId: userStore.userId,
          toUserId: currentChatUser.value,
          content: imageUrl,
          messageType: 4,
          createdAt: new Date().toISOString()
        }
        
        if (!messages[currentChatUser.value]) {
          messages[currentChatUser.value] = []
        }
        messages[currentChatUser.value].push(msg)
        
        // 更新最近联系人列表
        if (userInfoCache[currentChatUser.value]) {
          const existingIndex = recentContacts.value.findIndex(u => u.userId === currentChatUser.value)
          if (existingIndex === -1) {
            recentContacts.value.unshift(userInfoCache[currentChatUser.value])
          } else if (existingIndex > 0) {
            const [existingUser] = recentContacts.value.splice(existingIndex, 1)
            recentContacts.value.unshift(existingUser)
          }
        }
      }
    }
    // 发送群聊图片
    else if (chatType.value === 'group') {
      success = wsClient.sendGroupMessage(currentChatGroup.value, imageUrl, 4) // messageType: 4 图片
      
      if (success) {
        // 添加到本地消息列表
        const msg = {
          fromUserId: userStore.userId,
          groupId: currentChatGroup.value,
          content: imageUrl,
          messageType: 4,
          createdAt: new Date().toISOString()
        }
        
        if (!messages[currentChatGroup.value]) {
          messages[currentChatGroup.value] = []
        }
        messages[currentChatGroup.value].push(msg)
        
        // 更新最近群聊列表
        const groupIndex = recentGroups.value.findIndex(g => g.groupId === currentChatGroup.value)
        if (groupIndex > 0) {
          const [group] = recentGroups.value.splice(groupIndex, 1)
          recentGroups.value.unshift(group)
        }
      }
    }
    
    if (success) {
      // 滚动到底部
      nextTick(() => scrollToBottom())
    } else {
      ElMessage.error('发送失败，请检查连接')
    }
  }
  
  // 触发文件选择
  const handleSelectFile = () => {
    docInputRef.value.click()
  }
  
  // 处理文件上传
  const handleFileUpload = async (event) => {
    const file = event.target.files[0]
    if (!file) return
    
    // 🎯 智能识别：如果是图片，自动当作图片发送
    if (file.type.startsWith('image/')) {
      // 验证图片大小（最大10MB）
      if (file.size > 10 * 1024 * 1024) {
        ElMessage.warning('图片大小不能超过10MB')
        return
      }
      
      try {
        isUploadingDoc.value = true
        
        // 上传图片
        const imageUrl = await fileApi.uploadFile(file, 'image')
        
        // 发送图片消息
        await sendImageMessage(imageUrl)
        
        ElMessage.success('图片发送成功')
      } catch (error) {
        console.error('图片上传失败:', error)
        ElMessage.error('图片上传失败：' + (error.message || '未知错误'))
      } finally {
        isUploadingDoc.value = false
        event.target.value = ''
      }
      return
    }
    
    // 非图片文件：正常文件上传流程
    // 验证文件大小（最大50MB）
    if (file.size > 50 * 1024 * 1024) {
      ElMessage.warning('文件大小不能超过50MB')
      return
    }
    
    try {
      isUploadingDoc.value = true
      
      // 上传文件到服务器
      const fileUrl = await fileApi.uploadFile(file, 'file')
      
      console.log('文件上传成功:', fileUrl)
      
      // 发送文件消息（带文件名和大小）
      const fileInfo = {
        url: fileUrl,
        name: file.name,
        size: file.size
      }
      await sendFileMessage(fileInfo)
      
      ElMessage.success('文件发送成功')
    } catch (error) {
      console.error('文件上传失败:', error)
      ElMessage.error('文件上传失败：' + (error.message || '未知错误'))
    } finally {
      isUploadingDoc.value = false
      // 清空文件选择
      event.target.value = ''
    }
  }
  
  // 发送文件消息
  const sendFileMessage = async (fileInfo) => {
    const chatId = chatType.value === 'user' ? currentChatUser.value : currentChatGroup.value
    if (!chatId) return
    
    // 将文件信息编码为 JSON 字符串
    const content = JSON.stringify(fileInfo)
    let success = false
    
    // 发送私聊文件
    if (chatType.value === 'user') {
      success = wsClient.sendMessage(currentChatUser.value, content, 5) // messageType: 5 文件
      
      if (success) {
        // 添加到本地消息列表
        const msg = {
          fromUserId: userStore.userId,
          toUserId: currentChatUser.value,
          content: content,
          messageType: 5,
          createdAt: new Date().toISOString()
        }
        
        if (!messages[currentChatUser.value]) {
          messages[currentChatUser.value] = []
        }
        messages[currentChatUser.value].push(msg)
        
        // 更新最近联系人列表
        if (userInfoCache[currentChatUser.value]) {
          const existingIndex = recentContacts.value.findIndex(u => u.userId === currentChatUser.value)
          if (existingIndex === -1) {
            recentContacts.value.unshift(userInfoCache[currentChatUser.value])
          } else if (existingIndex > 0) {
            const [existingUser] = recentContacts.value.splice(existingIndex, 1)
            recentContacts.value.unshift(existingUser)
          }
        }
      }
    }
    // 发送群聊文件
    else if (chatType.value === 'group') {
      success = wsClient.sendGroupMessage(currentChatGroup.value, content, 5) // messageType: 5 文件
      
      if (success) {
        // 添加到本地消息列表
        const msg = {
          fromUserId: userStore.userId,
          groupId: currentChatGroup.value,
          content: content,
          messageType: 5,
          createdAt: new Date().toISOString()
        }
        
        if (!messages[currentChatGroup.value]) {
          messages[currentChatGroup.value] = []
        }
        messages[currentChatGroup.value].push(msg)
        
        // 更新最近群聊列表
        const groupIndex = recentGroups.value.findIndex(g => g.groupId === currentChatGroup.value)
        if (groupIndex > 0) {
          const [group] = recentGroups.value.splice(groupIndex, 1)
          recentGroups.value.unshift(group)
        }
      }
    }
    
    if (success) {
      // 滚动到底部
      nextTick(() => scrollToBottom())
    } else {
      ElMessage.error('发送失败，请检查连接')
    }
  }
  
  // 格式化文件大小
  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
  }
  
  // 下载文件
  const downloadFile = (url) => {
    window.open(url, '_blank')
  }
  
  // 发送消息
  const sendMessage = async () => {
    if (!inputMessage.value.trim()) return
    
    const chatId = chatType.value === 'user' ? currentChatUser.value : currentChatGroup.value
    
    if (!chatId) return
    
    // 如果是AI助手
    if (chatType.value === 'user' && currentChatUser.value === AI_ASSISTANT_ID) {
      await sendMessageToAI()
      return
    }
    
    let success = false
    
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
        
        // 更新最近联系人列表（将当前聊天用户移到最前面）
        if (userInfoCache[currentChatUser.value]) {
          const existingIndex = recentContacts.value.findIndex(u => u.userId === currentChatUser.value)
          if (existingIndex === -1) {
            // 不存在，添加到最前面
            recentContacts.value.unshift(userInfoCache[currentChatUser.value])
          } else if (existingIndex > 0) {
            // 如果已存在但不在第一位，移到最前面
            const [existingUser] = recentContacts.value.splice(existingIndex, 1)
            recentContacts.value.unshift(existingUser)
          }
        }
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
      if (!isUserOnline(newUserId) && newUserId !== userStore.userId) {
        // 重新加载在线用户列表
        loadOnlineUsers()
        // 直接使用后端传来的昵称
        const nickname = message.nickname || newUserId
        ElMessage.success(`${nickname} 上线了`)
        console.log('用户上线:', nickname, newUserId)
      }
      return
    }
    
    // 处理系统消息：用户下线
    if (message.type === 'user_offline') {
      const offlineUserId = message.fromUserId
      const index = onlineUsers.value.findIndex(u => u.userId === offlineUserId)
      if (index > -1) {
        const offlineUser = onlineUsers.value[index]
        onlineUsers.value.splice(index, 1)
        ElMessage.info(`${offlineUser.nickname} 下线了`)
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
    
    // 处理系统消息：成员被添加通知
    if (message.type === 'member_added') {
      ElMessage.success(message.message)
      // 刷新群组列表
      loadRecentGroups()
      loadUserGroups()
      return
    }
    
    // 处理系统消息：成员被移除通知
    if (message.type === 'member_removed') {
      ElMessage.warning(message.message)
      // 刷新群组列表
      loadRecentGroups()
      loadUserGroups()
      // 如果当前正在这个群组的聊天窗口，关闭它
      if (chatType.value === 'group' && currentChatGroup.value === message.groupId) {
        closeChat()
      }
      return
    }
    
    // 处理私聊消息
    if (message.type === 'chat') {
      const fromUser = message.fromUserId
      
      // 如果缓存中没有发送者信息，异步加载（修复白屏问题）
      if (fromUser && !userInfoCache[fromUser]) {
        messageApi.batchGetUserInfo([fromUser]).then(response => {
          const users = response.data || []
          users.forEach(user => {
            userInfoCache[user.userId] = user
            
            // 加载完用户信息后，更新最近联系人列表
            const existingIndex = recentContacts.value.findIndex(u => u.userId === user.userId)
            if (existingIndex === -1) {
              recentContacts.value.unshift(user)
            } else {
              // 如果已存在，移到最前面
              const [existingUser] = recentContacts.value.splice(existingIndex, 1)
              recentContacts.value.unshift(existingUser)
            }
          })
        }).catch(error => {
          console.error('加载发送者信息失败:', error)
        })
      } else if (userInfoCache[fromUser]) {
        // 如果缓存中有发送者信息，直接更新最近联系人列表
        const existingIndex = recentContacts.value.findIndex(u => u.userId === fromUser)
        if (existingIndex === -1) {
          recentContacts.value.unshift(userInfoCache[fromUser])
        } else if (existingIndex > 0) {
          // 如果已存在但不在第一位，移到最前面
          const [existingUser] = recentContacts.value.splice(existingIndex, 1)
          recentContacts.value.unshift(existingUser)
        }
      }
      
      // 初始化消息数组
      if (!messages[fromUser]) {
        messages[fromUser] = []
      }
      
      // 添加消息
      messages[fromUser].push({
        ...message,
        content: message.message,
        messageType: message.messageType || 1  // 保留 messageType 字段
      })
      
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
      const fromUserId = message.fromUserId
      
      console.log('收到群聊消息:', message)
      
      // 如果缓存中没有发送者信息，异步加载
      if (fromUserId && !userInfoCache[fromUserId]) {
        messageApi.batchGetUserInfo([fromUserId]).then(response => {
          const users = response.data || []
          users.forEach(user => {
            userInfoCache[user.userId] = user
          })
        }).catch(error => {
          console.error('加载发送者信息失败:', error)
        })
      }
      
      // 初始化消息数组
      if (!messages[groupId]) {
        messages[groupId] = []
      }
      
      // 添加消息（统一使用 content 字段）
      messages[groupId].push({
        fromUserId: message.fromUserId,
        groupId: message.groupId,
        content: message.message,  // WebSocket 的 message 字段映射为 content
        messageType: message.messageType || 2,  // 保留 messageType 字段
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
      
      // 提取所有发送者的userId，批量加载用户信息
      const userIds = new Set()
      messages[group.groupId].forEach(msg => {
        if (msg.fromUserId && !userInfoCache[msg.fromUserId]) {
          userIds.add(msg.fromUserId)
        }
      })
      
      if (userIds.size > 0) {
        try {
          const userResponse = await messageApi.batchGetUserInfo(Array.from(userIds))
          const users = userResponse.data || []
          users.forEach(user => {
            userInfoCache[user.userId] = user
          })
          console.log('批量加载群聊成员信息:', users.length, '个用户')
        } catch (error) {
          console.error('加载群聊成员信息失败:', error)
        }
      }
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
  // 生成头像URL（与注册页面相同）
  const generateAvatarUrl = (nickname, colorScheme) => {
    const name = nickname || '?'
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=${colorScheme.bg}&color=${colorScheme.fg}&size=128`
  }
  
  // 选择颜色
  const selectProfileColor = (colorScheme) => {
    selectedProfileColor.value = colorScheme
    if (profileForm.nickname) {
      profileForm.avatar = generateAvatarUrl(profileForm.nickname, colorScheme)
    }
  }
  
  // 打开个人资料对话框时初始化
  watch(showProfileDialog, (newVal) => {
    if (newVal) {
      // 填充当前用户信息
      profileForm.userId = userStore.userId
      profileForm.nickname = userStore.nickname
      profileForm.email = userStore.email || ''
      profileForm.avatar = userStore.avatar || ''
      
      // 检测当前头像是否是通过UI Avatars生成的
      if (userStore.avatar && userStore.avatar.includes('ui-avatars.com')) {
        // 如果是UI Avatars生成的，提取颜色方案
        try {
          const urlParams = new URLSearchParams(userStore.avatar.split('?')[1])
          const avatarBg = urlParams.get('background')
          const matchedColor = colorSchemes.find(c => c.bg === avatarBg)
          
          if (matchedColor) {
            selectedProfileColor.value = matchedColor
            customAvatarUrl.value = '' // 清空自定义URL
          } else {
            // 找不到匹配的颜色，默认第一个
            selectedProfileColor.value = colorSchemes[0]
            customAvatarUrl.value = userStore.avatar // 放到自定义URL中
          }
        } catch (e) {
          selectedProfileColor.value = colorSchemes[0]
          customAvatarUrl.value = userStore.avatar
        }
      } else if (userStore.avatar) {
        // 如果是自定义头像URL，填到自定义输入框
        customAvatarUrl.value = userStore.avatar
        selectedProfileColor.value = colorSchemes[0]
      } else {
        // 没有头像，默认第一个颜色
        customAvatarUrl.value = ''
        selectedProfileColor.value = colorSchemes[0]
      }
    }
  })
  
  // 触发头像选择
  const handleSelectAvatar = () => {
    avatarInputRef.value.click()
  }
  
  // 处理头像上传
  const handleAvatarUpload = async (event) => {
    const file = event.target.files[0]
    if (!file) return
    
    // 验证文件类型
    if (!file.type.startsWith('image/')) {
      ElMessage.warning('请选择图片文件')
      return
    }
    
    // 验证文件大小（最大2MB）
    if (file.size > 2 * 1024 * 1024) {
      ElMessage.warning('头像大小不能超过2MB')
      return
    }
    
    try {
      isUploadingAvatar.value = true
      
      // 上传头像到服务器（file.js 已经返回 URL 字符串）
      const avatarUrl = await fileApi.uploadFile(file, 'avatar')
      
      // 更新表单中的头像
      profileForm.avatar = avatarUrl
      customAvatarUrl.value = '' // 清空自定义URL
      
      ElMessage.success('头像上传成功')
    } catch (error) {
      console.error('头像上传失败:', error)
      ElMessage.error('头像上传失败：' + (error.message || '未知错误'))
    } finally {
      isUploadingAvatar.value = false
      // 清空文件选择
      event.target.value = ''
    }
  }
  
  // 更新个人资料
  const handleUpdateProfile = async () => {
    if (!profileForm.nickname.trim()) {
      ElMessage.warning('昵称不能为空')
      return
    }
    
    try {
      // 优先使用当前头像（可能是上传的），其次自定义URL，最后使用颜色方案生成
      let finalAvatar = profileForm.avatar
      
      if (customAvatarUrl.value.trim()) {
        finalAvatar = customAvatarUrl.value.trim()
      } else if (!profileForm.avatar || profileForm.avatar.includes('ui-avatars.com')) {
        // 如果当前头像是UI Avatars生成的，重新生成
        finalAvatar = generateAvatarUrl(profileForm.nickname, selectedProfileColor.value)
      }
      
      const response = await updateProfileApi({
        userId: profileForm.userId,
        nickname: profileForm.nickname,
        avatar: finalAvatar,
        email: profileForm.email
      })
      
      // 更新本地store和localStorage
      userStore.updateProfile({
        nickname: profileForm.nickname,
        avatar: finalAvatar,
        email: profileForm.email
      })
      
      ElMessage.success('更新成功')
      showProfileDialog.value = false
    } catch (error) {
      console.error('更新失败:', error)
      ElMessage.error('更新失败：' + (error.message || '未知错误'))
    }
  }
  
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
      
      // 提取所有涉及的用户ID并加载到缓存
      if (searchResults.value.length > 0) {
        const userIds = new Set()
        searchResults.value.forEach(msg => {
          if (msg.fromUserId) userIds.add(msg.fromUserId)
          if (msg.toUserId && msg.messageType === 1) userIds.add(msg.toUserId)
        })
        
        // 过滤掉已经在缓存中的用户
        const uncachedUserIds = Array.from(userIds).filter(id => !userInfoCache[id])
        
        // 批量加载用户信息
        if (uncachedUserIds.length > 0) {
          try {
            const response = await messageApi.batchGetUserInfo(uncachedUserIds)
            const users = response.data || []
            users.forEach(user => {
              userInfoCache[user.userId] = user
            })
            console.log('批量加载用户信息:', users.length, '个用户')
          } catch (error) {
            console.error('加载用户信息失败:', error)
          }
        }
      }
      
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
  
  // 发送消息给AI
  const sendMessageToAI = async () => {
    const userMessage = inputMessage.value.trim()
    if (!userMessage) return
    
    // 添加用户消息到列表
    const userMsg = {
      fromUserId: userStore.userId,
      toUserId: AI_ASSISTANT_ID,
      content: userMessage,
      createdAt: new Date().toISOString()
    }
    
    if (!messages[AI_ASSISTANT_ID]) {
      messages[AI_ASSISTANT_ID] = []
    }
    messages[AI_ASSISTANT_ID].push(userMsg)
    
    // 清空输入框
    inputMessage.value = ''
    
    // 显示AI正在思考
    isAIThinking.value = true
    
    // 滚动到底部
    await nextTick()
    scrollToBottom()
    
    try {
      // 调用AI API
      const response = await aiApi.chat(userStore.userId, userMessage, false)
      
      // 添加AI回复到消息列表
      const aiMsg = {
        fromUserId: AI_ASSISTANT_ID,
        toUserId: userStore.userId,
        content: response.data.reply,
        createdAt: new Date().toISOString(),
        tokensUsed: response.data.tokensUsed
      }
      
      messages[AI_ASSISTANT_ID].push(aiMsg)
      
      // 滚动到底部
      await nextTick()
      scrollToBottom()
      
    } catch (error) {
      console.error('AI聊天失败:', error)
      ElMessage.error('AI暂时无法回复，请稍后重试')
    } finally {
      isAIThinking.value = false
    }
  }
  
  // 处理AI对话管理操作
  const handleAIAction = async (command) => {
    if (command === 'newTopic') {
      // 开始新话题（只清空上下文，不删除历史）
      await startNewTopic()
    } else if (command === 'clearAll') {
      // 删除所有记录（需要确认）
      await deleteAllHistory()
    }
  }
  
  // 开始新话题（只清空上下文，保留历史）
  const startNewTopic = async () => {
    try {
      // 只清空Redis中的上下文，不删除数据库
      await aiApi.clearHistory(userStore.userId)
      
      // 在聊天界面添加分隔符
      if (messages[AI_ASSISTANT_ID]) {
        const now = new Date()
        messages[AI_ASSISTANT_ID].push({
          type: 'divider',
          content: `新对话开始 · ${now.toLocaleString('zh-CN', { 
            month: '2-digit', 
            day: '2-digit', 
            hour: '2-digit', 
            minute: '2-digit' 
          })}`,
          createdAt: now.toISOString(),
          id: 'divider-' + Date.now()
        })
        
        // 滚动到底部
        await nextTick()
        scrollToBottom()
      }
      
      ElMessage.success({
        message: '✅ 已开始新话题！AI不会记得之前的内容，但历史记录保留',
        duration: 3000
      })
      console.log('已开始新话题')
    } catch (error) {
      console.error('开始新话题失败:', error)
      ElMessage.error('操作失败，请稍后重试')
    }
  }
  
  // 删除所有历史记录（需要确认）
  const deleteAllHistory = async () => {
    try {
      await ElMessageBox.confirm(
        '确定要删除所有AI对话记录吗？此操作不可恢复！',
        '警告',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
          center: true
        }
      )
      
      // 调用后端API删除所有记录
      await aiApi.deleteHistory(userStore.userId)
      
      // 清空前端显示
      messages[AI_ASSISTANT_ID] = []
      
      ElMessage.success('所有对话记录已删除')
      console.log('AI对话记录已删除')
    } catch (error) {
      if (error === 'cancel') {
        console.log('用户取消删除')
      } else {
        console.error('删除对话记录失败:', error)
        ElMessage.error('删除失败，请稍后重试')
      }
    }
  }
  
  // 检查用户是否在线
  const isUserOnline = (userId) => {
    return onlineUsers.value.some(u => u.userId === userId)
  }
  
  // 根据userId获取用户昵称
  const getUserNickname = (userId) => {
    if (!userId) return '未知用户'  // ← 修复：处理 undefined
    if (userInfoCache[userId] && userInfoCache[userId].nickname) {
      return userInfoCache[userId].nickname
    }
    return userId // 如果缓存中没有，返回userId
  }
  
  // 获取用户头像（使用UI Avatars生成）
  const getUserAvatar = (userId) => {
    if (!userId) return 'https://ui-avatars.com/api/?name=Unknown&background=cccccc&color=fff&size=128'  // ← 修复：处理 undefined
    // 优先从缓存中获取用户头像
    if (userInfoCache[userId] && userInfoCache[userId].avatar) {
      return userInfoCache[userId].avatar
    }
    // 使用UI Avatars API根据用户ID生成头像
    const colors = ['667eea', 'f093fb', '4facfe', '43e97b', 'fa709a', 'fee140', '30cfd0', 'a8edea']
    const colorIndex = userId.charCodeAt(0) % colors.length
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(userId)}&background=${colors[colorIndex]}&color=fff&size=128`
  }
  
  // 获取消息发送者的头像
  const getMessageAvatar = (fromUserId) => {
    // AI助手使用特殊的emoji头像
    if (fromUserId === AI_ASSISTANT_ID) {
      return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTI4IiBoZWlnaHQ9IjEyOCIgZmlsbD0iIzY2N2VlYSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LXNpemU9IjcwIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIj7wn6SWPC90ZXh0Pjwvc3ZnPg=='
    }
    // 其他用户使用默认头像生成
    return getUserAvatar(fromUserId)
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
  
  // 打开管理成员对话框
  const openManageMembersDialog = async () => {
    if (!currentChatGroup.value) return
    
    try {
      // 加载群组成员列表
      const response = await groupApi.getGroupMembers(currentChatGroup.value)
      currentGroupMembers.value = response.data || []
      
      // 批量加载成员信息到缓存
      const uncachedUserIds = currentGroupMembers.value.filter(id => !userInfoCache[id])
      if (uncachedUserIds.length > 0) {
        try {
          const userResponse = await messageApi.batchGetUserInfo(uncachedUserIds)
          const users = userResponse.data || []
          users.forEach(user => {
            userInfoCache[user.userId] = user
          })
        } catch (error) {
          console.error('加载成员信息失败:', error)
        }
      }
      
      manageMembersDialogVisible.value = true
    } catch (error) {
      console.error('加载群组成员失败:', error)
      ElMessage.error('加载群组成员失败')
    }
  }
  
  // 添加成员到群组
  const handleAddMember = async (userId) => {
    if (!currentChatGroup.value) return
    
    try {
      await groupApi.addMember(currentChatGroup.value, userId)
      ElMessage.success('添加成功')
      
      // 刷新成员列表
      const response = await groupApi.getGroupMembers(currentChatGroup.value)
      currentGroupMembers.value = response.data || []
      
      // 刷新群组列表（更新成员数量）
      await loadUserGroups()
      await loadRecentGroups()
    } catch (error) {
      console.error('添加成员失败:', error)
      ElMessage.error('添加失败：' + (error.message || '未知错误'))
    }
  }
  
  // 从群组移除成员
  const handleRemoveMember = async (userId) => {
    if (!currentChatGroup.value) return
    
    try {
      await ElMessageBox.confirm(
        `确定要将 ${getUserNickname(userId)} 移出群组吗？`,
        '确认',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      
      await groupApi.removeMember(currentChatGroup.value, userId)
      ElMessage.success('移除成功')
      
      // 刷新成员列表
      const response = await groupApi.getGroupMembers(currentChatGroup.value)
      currentGroupMembers.value = response.data || []
      
      // 刷新群组列表（更新成员数量）
      await loadUserGroups()
      await loadRecentGroups()
    } catch (error) {
      if (error === 'cancel') {
        console.log('用户取消移除')
      } else {
        console.error('移除成员失败:', error)
        ElMessage.error('移除失败：' + (error.message || '未知错误'))
      }
    }
  }
  
  // 判断是否是群主
  const isGroupOwner = (userId) => {
    if (!currentChatGroup.value) return false
    const group = groupList.value.find(g => g.groupId === currentChatGroup.value)
    return group && group.creatorId === userId
  }
  
  // 解析图文消息
  const parseImageMessage = (content) => {
    if (!content) return null
    try {
      const parsed = JSON.parse(content)
      if (parsed.text && parsed.imageUrl) {
        return parsed
      }
      return null
    } catch (e) {
      return null
    }
  }
  
  // 解析文档消息
  const parseDocumentMessage = (content) => {
    if (!content) return null
    try {
      const parsed = JSON.parse(content)
      if (parsed.text && parsed.fileName && parsed.fileId) {
        return parsed
      }
      return null
    } catch (e) {
      return null
    }
  }
  
  // 处理AI图片选择
  const handleAIImageSelect = (event) => {
    const file = event.target.files[0]
    if (!file) return
    
    // 验证文件类型
    if (!file.type.startsWith('image/')) {
      ElMessage.warning('请选择图片文件')
      return
    }
    
    // 验证文件大小（最大10MB）
    if (file.size > 10 * 1024 * 1024) {
      ElMessage.warning('图片大小不能超过10MB')
      return
    }
    
    // 保存文件并生成预览
    selectedImageForAI.value = file
    selectedImagePreview.value = URL.createObjectURL(file)
  }
  
  // 取消图片上传
  const cancelImageUpload = () => {
    showImageUploadDialog.value = false
    selectedImageForAI.value = null
    selectedImagePreview.value = ''
    imageQuestion.value = ''
    
    // 清空文件输入
    if (aiImageInputRef.value) {
      aiImageInputRef.value.value = ''
    }
  }
  
  // 处理AI文档选择
  const handleAIDocumentSelect = (event) => {
    const file = event.target.files[0]
    if (!file) return
    
    // 验证文件大小（最大50MB）
    if (file.size > 50 * 1024 * 1024) {
      ElMessage.warning('文档大小不能超过50MB')
      return
    }
    
    selectedDocumentForAI.value = file
  }
  
  // 取消文档上传
  const cancelDocumentUpload = () => {
    showDocumentUploadDialog.value = false
    selectedDocumentForAI.value = null
    documentQuestion.value = ''
    
    // 清空文件输入
    if (aiDocInputRef.value) {
      aiDocInputRef.value.value = ''
    }
  }
  
  // 发送文档给AI
  const sendDocumentToAI = async () => {
    if (!selectedDocumentForAI.value || !documentQuestion.value.trim()) {
      ElMessage.warning('请选择文档并输入问题')
      return
    }
    
    try {
      isSendingDocToAI.value = true
      
      // 1. 上传文档到通义千问，获取file_id
      ElMessage.info('正在上传文档...')
      const fileId = await aiApi.uploadDocument(selectedDocumentForAI.value)
      console.log('文档上传成功，file_id:', fileId)
      
      // 2. 添加用户消息到聊天列表（文档消息）
      const userMsg = {
        fromUserId: userStore.userId,
        toUserId: AI_ASSISTANT_ID,
        content: JSON.stringify({
          text: documentQuestion.value,
          fileName: selectedDocumentForAI.value.name,
          fileId: fileId
        }),
        createdAt: new Date().toISOString(),
        isDocumentMessage: true  // 标记为文档消息
      }
      
      if (!messages[AI_ASSISTANT_ID]) {
        messages[AI_ASSISTANT_ID] = []
      }
      messages[AI_ASSISTANT_ID].push(userMsg)
      
      // 3. 关闭对话框
      showDocumentUploadDialog.value = false
      
      // 4. 显示AI正在思考
      isAIThinking.value = true
      
      // 5. 滚动到底部
      await nextTick()
      scrollToBottom()
      
      // 6. 调用AI文档对话API
      ElMessage.info('AI正在分析文档...')
      const response = await aiApi.chatWithDocument(
        userStore.userId,
        documentQuestion.value,
        fileId,
        selectedDocumentForAI.value.name
      )
      
      // 7. 添加AI回复到消息列表
      const aiMsg = {
        fromUserId: AI_ASSISTANT_ID,
        toUserId: userStore.userId,
        content: response.data.reply,
        createdAt: new Date().toISOString()
      }
      
      messages[AI_ASSISTANT_ID].push(aiMsg)
      
      // 8. 清空表单
      selectedDocumentForAI.value = null
      documentQuestion.value = ''
      
      if (aiDocInputRef.value) {
        aiDocInputRef.value.value = ''
      }
      
      ElMessage.success('AI已回复')
      
      // 9. 滚动到底部
      await nextTick()
      scrollToBottom()
      
    } catch (error) {
      console.error('发送失败:', error)
      ElMessage.error('发送失败：' + (error.message || '未知错误'))
    } finally {
      isAIThinking.value = false
      isSendingDocToAI.value = false
    }
  }
  
  // 发送图片给AI
  const sendImageToAI = async () => {
    if (!selectedImageForAI.value || !imageQuestion.value.trim()) {
      ElMessage.warning('请选择图片并输入问题')
      return
    }
    
    try {
      isSendingImageToAI.value = true
      
      // 1. 上传图片到服务器
      const imageUrl = await fileApi.uploadFile(selectedImageForAI.value, 'image')
      console.log('图片上传成功:', imageUrl)
      
      // 2. 添加用户消息到聊天列表（图文消息）
      const userMsg = {
        fromUserId: userStore.userId,
        toUserId: AI_ASSISTANT_ID,
        content: JSON.stringify({
          text: imageQuestion.value,
          imageUrl: imageUrl
        }),
        createdAt: new Date().toISOString(),
        isImageMessage: true  // 标记为图文消息
      }
      
      if (!messages[AI_ASSISTANT_ID]) {
        messages[AI_ASSISTANT_ID] = []
      }
      messages[AI_ASSISTANT_ID].push(userMsg)
      
      // 3. 关闭对话框
      showImageUploadDialog.value = false
      
      // 4. 显示AI正在思考
      isAIThinking.value = true
      
      // 5. 滚动到底部
      await nextTick()
      scrollToBottom()
      
      // 6. 调用AI图文对话API
      const response = await aiApi.chatWithImage(
        userStore.userId,
        imageQuestion.value,
        imageUrl
      )
      
      // 7. 添加AI回复到消息列表
      const aiMsg = {
        fromUserId: AI_ASSISTANT_ID,
        toUserId: userStore.userId,
        content: response.data.reply,
        createdAt: new Date().toISOString()
      }
      
      messages[AI_ASSISTANT_ID].push(aiMsg)
      
      // 8. 清空表单
      selectedImageForAI.value = null
      selectedImagePreview.value = ''
      imageQuestion.value = ''
      
      if (aiImageInputRef.value) {
        aiImageInputRef.value.value = ''
      }
      
      ElMessage.success('AI已回复')
      
      // 9. 滚动到底部
      await nextTick()
      scrollToBottom()
      
    } catch (error) {
      console.error('发送失败:', error)
      ElMessage.error('发送失败：' + (error.message || '未知错误'))
    } finally {
      isAIThinking.value = false
      isSendingImageToAI.value = false
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
  border-bottom: 1px solid #e4e7ed;
}

.current-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.current-user-avatar {
  width: 45px;
  height: 45px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #67c23a;
}

.current-user-avatar-placeholder {
  width: 45px;
  height: 45px;
  border-radius: 50%;
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  border: 2px solid #67c23a;
}

.current-user-name {
  flex: 1;
}

.edit-profile-btn {
  margin-left: auto;
}

/* 个人资料编辑对话框样式 */
.profile-avatar-preview {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid #e4e7ed;
  flex-shrink: 0;
}

.profile-avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-preview-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* 头像上传区域 */
.avatar-upload-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.current-avatar-preview {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid #409eff;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.current-avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #909399;
  font-size: 12px;
  text-align: center;
  padding: 5px;
}

.color-selector {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  flex: 1;
}

.color-option {
  width: 45px;
  height: 45px;
  border-radius: 50%;
  cursor: pointer;
  border: 3px solid transparent;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.color-option:hover {
  transform: scale(1.15);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.color-option.selected {
  border-color: #fff;
  box-shadow: 0 0 0 2px #409eff;
}

.check-icon {
  color: white;
  font-size: 24px;
  font-weight: bold;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
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

.avatar-image {
  width: 45px;
  height: 45px;
  border-radius: 50%;
  object-fit: cover;
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
  
  .chat-header-actions {
    display: flex;
    gap: 10px;
  }
  
  .message-list {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    background: #f5f7fa;
  }
  
  /* 对话分隔符样式 */
  .chat-divider {
    display: flex;
    align-items: center;
    margin: 30px 0;
    width: 100%;
  }
  
  .divider-line {
    flex: 1;
    height: 1px;
    background: linear-gradient(to right, transparent, #d0d0d0, transparent);
  }
  
  .divider-text {
    padding: 0 20px;
    color: #909399;
    font-size: 13px;
    white-space: nowrap;
    background: #f5f7fa;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  .divider-icon {
    font-size: 14px;
    animation: pulse 2s ease-in-out infinite;
  }
  
  @keyframes pulse {
    0%, 100% {
      opacity: 1;
      transform: scale(1);
    }
    50% {
      opacity: 0.6;
      transform: scale(1.1);
    }
  }
  
  .message-item {
    margin-bottom: 16px;
    display: flex;
    align-items: flex-end;
    gap: 10px;
    animation: messageSlideIn 0.3s ease-out;
  }
  
  .message-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
    flex-shrink: 0;
    border: 2px solid #e4e7ed;
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
    max-width: calc(100vw - 450px);
    min-width: 80px;
  }
  
  /* 气泡内子元素对齐 */
  .message-item.sent .message-bubble {
    align-items: flex-end;
  }
  
  .message-item.received .message-bubble {
    align-items: flex-start;
  }
  
  .message-sender {
    font-size: 12px;
    color: #999;
    margin-bottom: 4px;
    padding: 0 4px;
    width: fit-content;
  }
  
  .message-content {
    padding: 12px 16px;
    border-radius: 18px;
    word-break: break-word;
    word-wrap: break-word;
    overflow-wrap: break-word;
    white-space: pre-wrap;
    line-height: 1.6;
    font-size: 15px;
    position: relative;
    width: fit-content;
    max-width: 100%;
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
    width: fit-content;
  }
  
  .message-item.sent .message-time {
    text-align: right;
    align-self: flex-end;
  }
  
  .message-item.received .message-time {
    text-align: left;
    align-self: flex-start;
  }
  
  .message-input {
    padding: 20px;
    border-top: 1px solid #e4e7ed;
    display: flex;
    gap: 10px;
    align-items: flex-start;
    position: relative;
  }
  
  .input-actions {
    display: flex;
    gap: 5px;
  }
  
  .message-input :deep(.el-textarea) {
    flex: 1;
  }
  
  /* Emoji选择器样式 */
  .emoji-picker {
    position: absolute;
    bottom: 100%;
    left: 20px;
    margin-bottom: 10px;
    width: 340px;
    max-height: 380px;
    background: white;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
    z-index: 1000;
  }
  
  .emoji-picker-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 14px;
    border-bottom: 1px solid #e4e7ed;
    font-weight: 500;
    font-size: 14px;
    color: #303133;
  }
  
  .emoji-picker-header .el-button {
    font-size: 20px;
    color: #909399;
    padding: 0;
  }
  
  .emoji-grid {
    display: grid;
    grid-template-columns: repeat(7, 36px);
    gap: 8px;
    padding: 12px;
    max-height: 300px;
    overflow-y: auto;
    overflow-x: hidden;
    justify-content: center;
  }
  
  .emoji-item {
    font-size: 28px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: 6px;
    transition: all 0.2s;
  }
  
  .emoji-item:hover {
    background: #f0f2f5;
    transform: scale(1.15);
  }
  
  /* 图片消息样式 */
  .message-image {
    cursor: pointer;
  }
  
  /* 文件消息样式 */
  .message-file {
    width: 100%;
  }
  
  .file-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 18px;
    background: #f5f7fa;
    border: 1px solid #e4e7ed;
    border-radius: 10px;
    transition: all 0.3s;
    cursor: pointer;
    user-select: none;
    min-width: 280px;
    max-width: 400px;
  }
  
  .file-card:hover {
    background: #ecf5ff;
    border-color: #409eff;
    transform: translateY(-2px);
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
  }
  
  .file-card:active {
    transform: translateY(0);
  }
  
  .file-icon {
    font-size: 36px;
    color: #409eff;
    flex-shrink: 0;
  }
  
  .file-info {
    flex: 1;
    min-width: 0;
  }
  
  .file-name {
    font-size: 15px;
    font-weight: 500;
    color: #303133;
    word-break: break-all;
    margin-bottom: 5px;
  }
  
  .file-size {
    font-size: 13px;
    color: #909399;
  }
  .message-image {
    cursor: pointer;
  }
  
  .image-error {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 20px;
    background: #f5f7fa;
    border-radius: 8px;
    color: #909399;
  }
  
  .image-error .el-icon {
    font-size: 32px;
    margin-bottom: 8px;
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
  
  /* AI助手样式 */
  .ai-assistant-item {
    border: 2px solid #f0f0f0;
    margin-bottom: 10px !important;
  }
  
  .ai-assistant-item:hover {
    border-color: #67c23a;
  }
  
  .ai-assistant-item.active {
    background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
    border-color: #67c23a;
  }
  
  .ai-avatar {
    background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%) !important;
    font-size: 22px;
  }
  
  .ai-tag {
    font-size: 11px;
    color: #67c23a;
    margin-left: 8px;
    padding: 2px 8px;
    background: #f0f9ff;
    border-radius: 10px;
  }
  
  /* AI思考动画 */
  .ai-thinking {
    display: flex;
    align-items: center;
  }
  
  .thinking-dot {
    display: inline-block;
    animation: thinking 1.4s infinite;
    margin: 0 2px;
    color: #67c23a;
  }
  
  .thinking-dot:nth-child(1) {
    animation-delay: 0s;
  }
  
  .thinking-dot:nth-child(2) {
    animation-delay: 0.2s;
  }
  
  .thinking-dot:nth-child(3) {
    animation-delay: 0.4s;
  }
  
  @keyframes thinking {
    0%, 60%, 100% {
      opacity: 0.3;
      transform: scale(0.8);
    }
    30% {
      opacity: 1;
      transform: scale(1.2);
    }
  }
  
  /* 管理群成员对话框样式 */
  .manage-members-content {
    max-height: 600px;
    overflow-y: auto;
  }
  
  .member-section, .add-member-section {
    margin-bottom: 20px;
  }
  
  .member-section h4, .add-member-section h4 {
    margin: 0 0 15px 0;
    font-size: 16px;
    color: #303133;
    padding-bottom: 10px;
    border-bottom: 2px solid #409eff;
  }
  
  .member-list, .online-user-list {
    max-height: 300px;
    overflow-y: auto;
  }
  
  .member-item, .add-member-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px;
    margin-bottom: 8px;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    transition: all 0.3s;
  }
  
  .member-item:hover, .add-member-item:hover {
    background: #f5f7fa;
    border-color: #409eff;
    transform: translateX(2px);
  }
  
  .member-info {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;
  }
  
  .member-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
    border: 2px solid #e4e7ed;
  }
  
  .member-name {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
  }
  
  /* AI图片上传对话框样式 */
  .image-upload-section {
    display: flex;
    flex-direction: column;
  }
  
  .image-preview {
    width: 100%;
    max-height: 300px;
    border: 2px solid #e4e7ed;
    border-radius: 8px;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f5f7fa;
  }
  
  .image-preview img {
    max-width: 100%;
    max-height: 300px;
    object-fit: contain;
  }
  
  .image-placeholder {
    width: 100%;
    height: 200px;
    border: 2px dashed #dcdfe6;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: #fafafa;
    color: #909399;
  }
  
  .image-placeholder p {
    margin-top: 10px;
    font-size: 14px;
  }
  
  /* 图文消息样式 */
  .message-image-text {
    display: flex;
    flex-direction: column;
    width: fit-content;
    max-width: 100%;
  }
  
  .message-item.sent .message-image-text {
    align-items: flex-end;
  }
  
  .message-item.received .message-image-text {
    align-items: flex-start;
  }
  
  /* 消息发送中状态 */
  .message-pending {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: #909399;
    font-size: 12px;
  }
  
  /* AI文档上传对话框样式 */
  .document-upload-section {
    display: flex;
    flex-direction: column;
  }
  
  .document-info {
    display: flex;
    align-items: center;
    gap: 15px;
    padding: 20px;
    background: #f5f7fa;
    border: 2px solid #e4e7ed;
    border-radius: 8px;
  }
  
  .doc-details {
    flex: 1;
  }
  
  .doc-name {
    font-size: 15px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 5px;
    word-break: break-all;
  }
  
  .doc-size {
    font-size: 13px;
    color: #909399;
  }
  
  .document-placeholder {
    width: 100%;
    height: 200px;
    border: 2px dashed #dcdfe6;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: #fafafa;
    color: #909399;
  }
  
  .document-placeholder p {
    margin-top: 10px;
    font-size: 14px;
  }
  
  .supported-formats {
    font-size: 12px !important;
    color: #c0c4cc !important;
  }
  
  /* 文档消息样式 */
  .message-document {
    display: flex;
    flex-direction: column;
    width: fit-content;
    max-width: 100%;
  }
  
  .message-item.sent .message-document {
    align-items: flex-end;
  }
  
  .message-item.received .message-document {
    align-items: flex-start;
  }
  
  .document-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    background: #f5f7fa;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
  }
  
  .doc-icon {
    color: #409eff;
    flex-shrink: 0;
  }
  
  .doc-info {
    flex: 1;
    min-width: 0;
  }
  
  .doc-filename {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 4px;
    word-break: break-all;
  }
  
  .doc-id {
    font-size: 12px;
    color: #909399;
    font-family: monospace;
  }
  </style>