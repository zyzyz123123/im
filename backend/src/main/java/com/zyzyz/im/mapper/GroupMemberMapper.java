package com.zyzyz.im.mapper;

import com.zyzyz.im.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupMemberMapper {
    
    // 插入群成员
    void insert(GroupMember groupMember);
    
    // 批量插入群成员
    void batchInsert(@Param("list") List<GroupMember> groupMembers);
    
    // 查询群组的所有成员ID
    List<String> selectMemberIdsByGroupId(@Param("groupId") String groupId);
    
    // 查询群组的所有成员
    List<GroupMember> selectMembersByGroupId(@Param("groupId") String groupId);
    
    // 查询用户是否在群组中
    GroupMember selectByGroupIdAndUserId(@Param("groupId") String groupId, @Param("userId") String userId);
    
    // 统计群成员数量
    int countByGroupId(@Param("groupId") String groupId);
    
    // 删除群成员
    void deleteByGroupIdAndUserId(@Param("groupId") String groupId, @Param("userId") String userId);
}

