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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/auth'

const router = useRouter()

const form = ref({
  userId: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  email: '',
  avatar: ''
})

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
</style>

