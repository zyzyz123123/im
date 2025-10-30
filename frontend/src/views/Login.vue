<template>
    <div class="login-container">
      <el-card class="login-card">
        <template #header>
          <h2>IM 聊天系统</h2>
        </template>
        
        <el-form :model="form" @submit.prevent="handleLogin">
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
              placeholder="请输入密码"
              clearable
              show-password
            />
          </el-form-item>
          
          <el-form-item>
            <el-button 
              type="primary" 
              @click="handleLogin"
              :loading="loading"
              style="width: 100%"
            >
              登录
            </el-button>
          </el-form-item>
          
          <el-form-item>
            <el-button 
              @click="goToRegister"
              style="width: 100%"
            >
              没有账号？去注册
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
  import { useUserStore } from '../stores/user'
  import { ElMessage } from 'element-plus'
  import { login } from '../api/auth'
  
  const router = useRouter()
  const userStore = useUserStore()
  
  const form = ref({
    nickname: '',
    password: ''
  })
  
  const loading = ref(false)
  const error = ref('')
  
  const handleLogin = async () => {
    // 表单验证
    if (!form.value.nickname.trim()) {
      error.value = '请输入昵称'
      return
    }
    
    if (!form.value.password.trim()) {
      error.value = '请输入密码'
      return
    }
    
    loading.value = true
    error.value = ''
    
    try {
      // 调用登录接口（拦截器已自动提取 Result.data）
      const response = await login({
        nickname: form.value.nickname,
        password: form.value.password
      })
      
      // 登录成功，保存用户信息
      userStore.login(response.data)
      ElMessage.success('登录成功')
      router.push('/chat')
    } catch (err) {
      console.error('登录失败:', err)
      error.value = err.message || '登录失败，请检查网络连接'
    } finally {
      loading.value = false
    }
  }
  
  const goToRegister = () => {
    router.push('/register')
  }
  </script>
  
  <style scoped>
  .login-container {
    width: 100%;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }
  
  .login-card {
    width: 400px;
  }
  
  .login-card :deep(.el-card__header) {
    text-align: center;
    background: #f5f7fa;
  }
  
  .login-card h2 {
    margin: 0;
    color: #303133;
  }
  </style>