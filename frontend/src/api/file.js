import axios from 'axios'

// 创建 axios 实例（复用配置）
const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 30000,
  withCredentials: true
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    
    // 如果返回的是 Result 对象
    if (res.code !== undefined) {
      if (res.code === 200) {
        return res.data  // 只返回 data 字段（URL字符串）
      } else {
        console.error('业务错误:', res.message)
        return Promise.reject(new Error(res.message || '请求失败'))
      }
    }
    
    return response
  },
  (error) => {
    console.error('响应错误:', error)
    return Promise.reject(error)
  }
)

// 文件 API
export const fileApi = {
  /**
   * 上传文件
   * @param {File} file - 文件对象
   * @param {String} type - 文件类型 image/file/voice
   */
  uploadFile(file, type = 'image') {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('fileType', type)
    
    return request.post('/file/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}

