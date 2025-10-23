import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const userId = ref('')
  const nickname = ref('')
  const avatar = ref('')
  const isLoggedIn = ref(false)
  
  /**
   * 登录：保存用户信息（Session 在后端管理）
   */
  function login(loginResponse) {
    userId.value = loginResponse.userId
    nickname.value = loginResponse.nickname || loginResponse.userId
    avatar.value = loginResponse.avatar || ''
    isLoggedIn.value = true
    
    // 保存到 localStorage（用于页面刷新时恢复状态）
    localStorage.setItem('userId', loginResponse.userId)
    localStorage.setItem('nickname', loginResponse.nickname || loginResponse.userId)
    localStorage.setItem('avatar', loginResponse.avatar || '')
  }
  
  /**
   * 登出：清空所有信息
   */
  function logout() {
    userId.value = ''
    nickname.value = ''
    avatar.value = ''
    isLoggedIn.value = false
    
    // 清空 localStorage
    localStorage.removeItem('userId')
    localStorage.removeItem('nickname')
    localStorage.removeItem('avatar')
  }
  
  /**
   * 检查登录状态：从 localStorage 恢复
   */
  function checkLogin() {
    const savedUserId = localStorage.getItem('userId')
    
    if (savedUserId) {
      userId.value = savedUserId
      nickname.value = localStorage.getItem('nickname') || savedUserId
      avatar.value = localStorage.getItem('avatar') || ''
      isLoggedIn.value = true
      return true
    }
    return false
  }
  
  return { 
    userId, 
    nickname, 
    avatar, 
    isLoggedIn, 
    login, 
    logout, 
    checkLogin 
  }
})