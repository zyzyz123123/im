package com.zyzyz.im.mapper;

import com.zyzyz.im.entity.User;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User selectByUserId(String userId);
    User selectByNickname(String nickname);
    void insert(User user);
    void updateUser(User user);
    
    /**
     * 批量查询用户信息
     */
    List<User> selectByUserIds(@Param("userIds") List<String> userIds);
}
