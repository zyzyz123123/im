import { request } from './request'

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

