package com.zyzyz.im.service;

import com.zyzyz.im.dto.CreateGroupRequest;
import com.zyzyz.im.dto.GroupResponse;
import com.zyzyz.im.entity.Group;

import java.util.List;

public interface GroupService {
    
    /**
     * 创建群组
     */
    GroupResponse createGroup(String creatorId, CreateGroupRequest request);
    
    /**
     * 获取群组信息
     */
    Group getGroupById(String groupId);
    
    /**
     * 获取用户的所有群组
     */
    List<GroupResponse> getUserGroups(String userId);
    
    /**
     * 获取群组的所有成员ID
     */
    List<String> getGroupMemberIds(String groupId);
    
    /**
     * 检查用户是否在群组中
     */
    boolean isUserInGroup(String groupId, String userId);
    
    /**
     * 获取用户最近有消息的群组（按最后消息时间排序）
     */
    List<GroupResponse> getRecentGroupsWithMessages(String userId);
    
    /**
     * 添加成员到群组
     */
    void addMemberToGroup(String groupId, String userId, String operatorId);
    
    /**
     * 从群组中移除成员
     */
    void removeMemberFromGroup(String groupId, String userId, String operatorId);
    
    /**
     * 检查用户是否是群主或管理员
     */
    boolean isGroupAdmin(String groupId, String userId);
}

