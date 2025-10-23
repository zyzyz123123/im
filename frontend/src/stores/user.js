import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const userId = ref('')
  const nickname = ref('')
  const avatar = ref('')
  const isLoggedIn = ref(false)
  
  /**
   * 登录：保存用户信息和 token
   */
  function login(loginResponse) {
    token.value = loginResponse.token
    userId.value = loginResponse.userId
    nickname.value = loginResponse.nickname || loginResponse.userId
    avatar.value = loginResponse.avatar || ''
    isLoggedIn.value = true
    
    // 保存到 localStorage
    localStorage.setItem('token', loginResponse.token)
    localStorage.setItem('userId', loginResponse.userId)
    localStorage.setItem('nickname', loginResponse.nickname || loginResponse.userId)
    localStorage.setItem('avatar', loginResponse.avatar || '')
  }
  
  /**
   * 登出：清空所有信息
   */
  function logout() {
    token.value = ''
    userId.value = ''
    nickname.value = ''
    avatar.value = ''
    isLoggedIn.value = false
    
    // 清空 localStorage
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('nickname')
    localStorage.removeItem('avatar')
  }
  
  /**
   * 检查登录状态：从 localStorage 恢复
   */
  function checkLogin() {
    const savedToken = localStorage.getItem('token')
    const savedUserId = localStorage.getItem('userId')
    
    if (savedToken && savedUserId) {
      token.value = savedToken
      userId.value = savedUserId
      nickname.value = localStorage.getItem('nickname') || savedUserId
      avatar.value = localStorage.getItem('avatar') || ''
      isLoggedIn.value = true
      return true
    }
    return false
  }
  
  return { 
    token, 
    userId, 
    nickname, 
    avatar, 
    isLoggedIn, 
    login, 
    logout, 
    checkLogin 
  }
})