package com.zyzyz.im.service;

import com.zyzyz.im.dto.RegisterRequest;
import com.zyzyz.im.dto.LoginRequest;
import com.zyzyz.im.dto.UpdateUserRequest;
import com.zyzyz.im.dto.UserInfoDTO;
import com.zyzyz.im.entity.User;
import com.zyzyz.im.dto.LoginResponse;

import java.util.List;

public interface UserService {
    void register(RegisterRequest registerRequest);
    LoginResponse login(LoginRequest loginRequest);
    User getUserByUserId(String userId);
    void updateUser(UpdateUserRequest updateUserRequest);

    /**
     * 获取用户信息
     */
    UserInfoDTO getUserInfoByUserId(String userId);
    /**
     * 批量查询用户信息
     */
    List<UserInfoDTO> getUserInfoByUserIds(List<String> userIds);
}
