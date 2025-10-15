import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const userId = ref('')
  const isLoggedIn = ref(false)
  
  function login(id) {
    userId.value = id
    isLoggedIn.value = true
    localStorage.setItem('userId', id)
  }
  
  function logout() {
    userId.value = ''
    isLoggedIn.value = false
    localStorage.removeItem('userId')
  }
  
  function checkLogin() {
    const savedUserId = localStorage.getItem('userId')
    if (savedUserId) {
      userId.value = savedUserId
      isLoggedIn.value = true
      return true
    }
    return false
  }
  
  return { userId, isLoggedIn, login, logout, checkLogin }
})