<template>
  <div class="register-container">
    <el-card class="register-card">
      <template #header>
        <h2>用户注册</h2>
      </template>
      
      <el-form :model="form" @submit.prevent="handleRegister">
        <el-form-item label="用户ID">
          <el-input 
            v-model="form.userId" 
            placeholder="请输入用户ID（唯一标识）"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="昵称">
          <el-input 
            v-model="form.nickname" 
            placeholder="请输入昵称"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="密码">
          <el-input 
            v-model="form.password" 
            type="password"
            placeholder="请输入密码（至少6位）"
            clearable
            show-password
          />
        </el-form-item>
        
        <el-form-item label="确认密码">
          <el-input 
            v-model="form.confirmPassword" 
            type="password"
            placeholder="请再次输入密码"
            clearable
            show-password
          />
        </el-form-item>
        
        <el-form-item label="邮箱（可选）">
          <el-input 
            v-model="form.email" 
            placeholder="请输入邮箱"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="选择头像颜色">
          <div class="avatar-preview-section">
            <!-- 预览当前头像 -->
            <div class="avatar-preview">
              <img 
                v-if="form.nickname && selectedColor" 
                :src="generateAvatarUrl(form.nickname, selectedColor)" 
                alt="头像预览" 
              />
              <div v-else class="avatar-placeholder">
                输入昵称后预览
              </div>
            </div>
            
            <!-- 颜色选择器 -->
            <div class="color-selector">
              <div 
                v-for="color in colorSchemes" 
                :key="color.bg"
                :class="['color-option', { selected: selectedColor.bg === color.bg }]"
                :style="{ backgroundColor: '#' + color.bg }"
                @click="selectColor(color)"
                :title="color.name"
              >
                <span v-if="selectedColor.bg === color.bg" class="check-icon">✓</span>
              </div>
            </div>
          </div>
        </el-form-item>
        
        <el-form-item label="自定义头像URL（可选）">
          <el-input 
            v-model="form.avatar" 
            placeholder="或者输入头像图片链接"
            clearable
          />
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            @click="handleRegister"
            :loading="loading"
            style="width: 100%"
          >
            注册
          </el-button>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            @click="goToLogin"
            style="width: 100%"
          >
            已有账号？去登录
          </el-button>
        </el-form-item>
      </el-form>
      
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        :closable="false"
        style="margin-top: 10px"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/auth'

const router = useRouter()

// 预设颜色方案
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

const form = ref({
  userId: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  email: '',
  avatar: ''  // 存储选中的颜色代码
})

const selectedColor = ref(colorSchemes[0]) // 默认第一个颜色

// 根据昵称和颜色生成头像URL
const generateAvatarUrl = (nickname, colorScheme) => {
  const name = nickname || '?'
  return `https://ui-avatars.com/api/?name=${encodeURIComponent(name)}&background=${colorScheme.bg}&color=${colorScheme.fg}&size=128`
}

// 监听昵称变化，自动更新头像
watch(() => form.value.nickname, (newNickname) => {
  if (newNickname && selectedColor.value) {
    form.value.avatar = generateAvatarUrl(newNickname, selectedColor.value)
  }
})

// 选择颜色
const selectColor = (colorScheme) => {
  selectedColor.value = colorScheme
  if (form.value.nickname) {
    form.value.avatar = generateAvatarUrl(form.value.nickname, colorScheme)
  }
}

const loading = ref(false)
const error = ref('')

const handleRegister = async () => {
  // 表单验证
  if (!form.value.userId.trim()) {
    error.value = '请输入用户ID'
    return
  }
  
  if (!form.value.nickname.trim()) {
    error.value = '请输入昵称'
    return
  }
  
  if (!form.value.password.trim()) {
    error.value = '请输入密码'
    return
  }
  
  if (form.value.password.length < 6) {
    error.value = '密码至少需要6位'
    return
  }
  
  if (form.value.password !== form.value.confirmPassword) {
    error.value = '两次输入的密码不一致'
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    // 调用注册接口（拦截器已自动提取 Result.data）
    await register({
      userId: form.value.userId,
      nickname: form.value.nickname,
      password: form.value.password,
      email: form.value.email,
      avatar: form.value.avatar
    })
    
    // 注册成功
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (err) {
    console.error('注册失败:', err)
    error.value = err.message || '注册失败，请检查网络连接'
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.register-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-card {
  width: 450px;
  max-height: 90vh;
  overflow-y: auto;
}

.register-card :deep(.el-card__header) {
  text-align: center;
  background: #f5f7fa;
}

.register-card h2 {
  margin: 0;
  color: #303133;
}

.avatar-preview-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-preview {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid #e4e7ed;
  flex-shrink: 0;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  box-shadow: 0 0 0 2px #67c23a;
}

.check-icon {
  color: white;
  font-size: 24px;
  font-weight: bold;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}
</style>

