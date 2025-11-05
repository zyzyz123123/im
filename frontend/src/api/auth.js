import { requestWithResponse as authAPI } from './request'

/**
 * 用户注册
 */
export function register(data) {
  return authAPI.post('/auth/register', {
    // userId 由后端自动生成，不需要前端传递
    password: data.password,
    nickname: data.nickname,
    avatar: data.avatar || '',
    email: data.email || ''
  })
}

/**
 * 用户登录
 */
export function login(data) {
  return authAPI.post('/auth/login', {
    nickname: data.nickname,  // 使用昵称登录
    password: data.password
  })
}

/**
 * 用户登出
 */
export function logout() {
  return authAPI.post('/auth/logout')
}

/**
 * 更新用户信息
 */
export function updateProfile(data) {
  return authAPI.post('/auth/updateProfile', {
    userId: data.userId,
    nickname: data.nickname,
    avatar: data.avatar,
    email: data.email
  })
}

