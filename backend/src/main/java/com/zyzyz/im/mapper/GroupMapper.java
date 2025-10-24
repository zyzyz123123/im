package com.zyzyz.im.mapper;

import com.zyzyz.im.entity.Group;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupMapper {
    
    // 插入群组
    void insert(Group group);
    
    // 根据群组ID查询
    Group selectByGroupId(@Param("groupId") String groupId);
    
    // 查询用户的所有群组
    List<Group> selectByUserId(@Param("userId") String userId);
    
    // 更新成员数量
    void updateMemberCount(@Param("groupId") String groupId, @Param("count") int count);
}

