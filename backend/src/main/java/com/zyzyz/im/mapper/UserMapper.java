package com.zyzyz.im.mapper;

import com.zyzyz.im.entity.User;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    User selectByUserId(String userId);
    void insert(User user);
    void updateUser(User user);
}
