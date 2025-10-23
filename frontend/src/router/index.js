import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Chat from '../views/Chat.vue'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: () => {
        // 如果已登录，跳转到聊天页；否则跳转到登录页
        const userStore = useUserStore()
        userStore.checkLogin()
        return userStore.isLoggedIn ? '/chat' : '/login'
      }
    },
    {
      path: '/login',
      name: 'Login',
      component: Login
    },
    {
      path: '/register',
      name: 'Register',
      component: Register
    },
    {
      path: '/chat',
      name: 'Chat',
      component: Chat,
      meta: { requiresAuth: true }
    }
  ]
})

// 路由守卫：检查登录状态
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  // 尝试从 localStorage 恢复登录状态
  if (!userStore.isLoggedIn) {
    userStore.checkLogin()
  }
  
  // 如果访问需要登录的页面
  if (to.meta.requiresAuth) {
    if (!userStore.isLoggedIn) {
      next('/login')
      return
    }
  }
  
  // 如果已登录用户访问登录/注册页，重定向到聊天页
  if ((to.path === '/login' || to.path === '/register') && userStore.isLoggedIn) {
    next('/chat')
    return
  }
  
  next()
})

export default router