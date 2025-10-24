import axios from 'axios'
import { ElMessage } from 'element-plus'

const BASE_URL = 'http://localhost:8080'

const request = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  withCredentials: true  // 携带 Cookie（Session）
})

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const result = response.data
    
    if (result && typeof result.code !== 'undefined') {
      if (result.code === 200) {
        return { ...response, data: result.data }
      } else {
        ElMessage.error(result.message || '请求失败')
        return Promise.reject(new Error(result.message || '请求失败'))
      }
    }
    
    return response
  },
  (error) => {
    console.error('请求错误:', error)
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

// 群组 API
export const groupApi = {
  // 创建群组
  createGroup(data) {
    return request.post('/group/create', data)
  },
  
  // 获取用户的所有群组
  getUserGroups() {
    return request.get('/group/list')
  },
  
  // 获取群组成员
  getGroupMembers(groupId) {
    return request.get('/group/members', {
      params: { groupId }
    })
  }
}

