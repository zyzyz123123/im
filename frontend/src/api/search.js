import { requestWithResponse as request } from './request'

// 搜索 API
export const searchApi = {
  // 搜索消息
  searchMessages(keyword, groupId = null) {
    return request.get('/search/messages', {
      params: { keyword, groupId }
    })
  }
}

