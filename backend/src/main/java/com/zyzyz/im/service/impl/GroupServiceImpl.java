package com.zyzyz.im.service.impl;

import com.zyzyz.im.dto.CreateGroupRequest;
import com.zyzyz.im.dto.GroupResponse;
import com.zyzyz.im.entity.Group;
import com.zyzyz.im.entity.GroupMember;
import com.zyzyz.im.mapper.GroupMapper;
import com.zyzyz.im.mapper.GroupMemberMapper;
import com.zyzyz.im.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl implements GroupService {
    
    @Autowired
    private GroupMapper groupMapper;
    
    @Autowired
    private GroupMemberMapper groupMemberMapper;
    
    @Override
    @Transactional
    public GroupResponse createGroup(String creatorId, CreateGroupRequest request) {
        // 生成群组ID
        String groupId = "group_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        
        // 创建群组
        Group group = Group.builder()
                .groupId(groupId)
                .groupName(request.getGroupName())
                .creatorId(creatorId)
                .avatar("")
                .description(request.getDescription() != null ? request.getDescription() : "")
                .memberCount(0)
                .status(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        groupMapper.insert(group);
        
        // 添加群主
        GroupMember owner = GroupMember.builder()
                .groupId(groupId)
                .userId(creatorId)
                .role("owner")
                .nickname("")
                .joinedAt(LocalDateTime.now())
                .build();
        groupMemberMapper.insert(owner);
        
        // 添加初始成员
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            List<GroupMember> members = new ArrayList<>();
            for (String memberId : request.getMemberIds()) {
                if (!memberId.equals(creatorId)) {  // 避免重复添加群主
                    members.add(GroupMember.builder()
                            .groupId(groupId)
                            .userId(memberId)
                            .role("member")
                            .nickname("")
                            .joinedAt(LocalDateTime.now())
                            .build());
                }
            }
            if (!members.isEmpty()) {
                groupMemberMapper.batchInsert(members);
            }
        }
        
        // 更新成员数量
        int memberCount = groupMemberMapper.countByGroupId(groupId);
        groupMapper.updateMemberCount(groupId, memberCount);
        
        // 获取所有成员ID
        List<String> allMemberIds = groupMemberMapper.selectMemberIdsByGroupId(groupId);
        
        // 返回结果
        return GroupResponse.builder()
                .groupId(groupId)
                .groupName(group.getGroupName())
                .creatorId(creatorId)
                .avatar(group.getAvatar())
                .description(group.getDescription())
                .memberCount(memberCount)
                .createdAt(group.getCreatedAt())
                .memberIds(allMemberIds)  // 返回成员ID列表
                .build();
    }
    
    @Override
    public Group getGroupById(String groupId) {
        return groupMapper.selectByGroupId(groupId);
    }
    
    @Override
    public List<GroupResponse> getUserGroups(String userId) {
        List<Group> groups = groupMapper.selectByUserId(userId);
        return groups.stream()
                .map(g -> GroupResponse.builder()
                        .groupId(g.getGroupId())
                        .groupName(g.getGroupName())
                        .creatorId(g.getCreatorId())
                        .avatar(g.getAvatar())
                        .description(g.getDescription())
                        .memberCount(g.getMemberCount())
                        .createdAt(g.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<String> getGroupMemberIds(String groupId) {
        return groupMemberMapper.selectMemberIdsByGroupId(groupId);
    }
    
    @Override
    public boolean isUserInGroup(String groupId, String userId) {
        GroupMember member = groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        return member != null;
    }
    
    @Override
    public List<GroupResponse> getRecentGroupsWithMessages(String userId) {
        // 获取用户的所有群组，按创建时间排序（简化版，实际应该按最后消息时间）
        List<Group> groups = groupMapper.selectByUserId(userId);
        return groups.stream()
                .map(g -> GroupResponse.builder()
                        .groupId(g.getGroupId())
                        .groupName(g.getGroupName())
                        .creatorId(g.getCreatorId())
                        .avatar(g.getAvatar())
                        .description(g.getDescription())
                        .memberCount(g.getMemberCount())
                        .createdAt(g.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void addMemberToGroup(String groupId, String userId, String operatorId) {
        // 检查操作者权限
        if (!isGroupAdmin(groupId, operatorId)) {
            throw new RuntimeException("您没有权限添加成员");
        }
        
        // 检查用户是否已经在群组中
        if (isUserInGroup(groupId, userId)) {
            throw new RuntimeException("该用户已经是群成员");
        }
        
        // 添加成员
        GroupMember member = GroupMember.builder()
                .groupId(groupId)
                .userId(userId)
                .role("member")
                .nickname("")
                .joinedAt(LocalDateTime.now())
                .build();
        groupMemberMapper.insert(member);
        
        // 更新群成员数量
        int memberCount = groupMemberMapper.countByGroupId(groupId);
        groupMapper.updateMemberCount(groupId, memberCount);
    }
    
    @Override
    @Transactional
    public void removeMemberFromGroup(String groupId, String userId, String operatorId) {
        // 检查操作者权限
        if (!isGroupAdmin(groupId, operatorId)) {
            throw new RuntimeException("您没有权限移除成员");
        }
        
        // 检查被移除的用户是否在群组中
        if (!isUserInGroup(groupId, userId)) {
            throw new RuntimeException("该用户不在群组中");
        }
        
        // 不能移除群主
        GroupMember member = groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        if ("owner".equals(member.getRole())) {
            throw new RuntimeException("不能移除群主");
        }
        
        // 移除成员
        groupMemberMapper.deleteByGroupIdAndUserId(groupId, userId);
        
        // 更新群成员数量
        int memberCount = groupMemberMapper.countByGroupId(groupId);
        groupMapper.updateMemberCount(groupId, memberCount);
    }
    
    @Override
    public boolean isGroupAdmin(String groupId, String userId) {
        GroupMember member = groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        if (member == null) {
            return false;
        }
        // 群主和管理员都有管理权限
        return "owner".equals(member.getRole()) || "admin".equals(member.getRole());
    }
}

