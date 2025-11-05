import { requestWithResponse as request } from './request'

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
  },
  
  // 添加成员到群组
  addMember(groupId, userId) {
    return request.post('/group/addMember', null, {
      params: { groupId, userId }
    })
  },
  
  // 从群组移除成员
  removeMember(groupId, userId) {
    return request.post('/group/removeMember', null, {
      params: { groupId, userId }
    })
  }
}

