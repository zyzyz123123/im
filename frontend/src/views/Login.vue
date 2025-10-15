<template>
    <div class="login-container">
      <el-card class="login-card">
        <template #header>
          <h2>IM 聊天系统</h2>
        </template>
        
        <el-form :model="form" @submit.prevent="handleLogin">
          <el-form-item label="用户ID">
            <el-input 
              v-model="form.userId" 
              placeholder="请输入用户ID（如：userA）"
              clearable
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
  
  const router = useRouter()
  const userStore = useUserStore()
  
  const form = ref({
    userId: ''
  })
  
  const loading = ref(false)
  const error = ref('')
  
  const handleLogin = () => {
    if (!form.value.userId.trim()) {
      error.value = '请输入用户ID'
      return
    }
    
    loading.value = true
    error.value = ''
    
    // 简单登录，直接保存userId
    setTimeout(() => {
      userStore.login(form.value.userId)
      ElMessage.success('登录成功')
      router.push('/chat')
      loading.value = false
    }, 500)
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