package com.zyzyz.im.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.zyzyz.im.dto.RegisterRequest;
import com.zyzyz.im.dto.LoginRequest;
import com.zyzyz.im.dto.LoginResponse;
import com.zyzyz.im.dto.UpdateUserRequest;
import com.zyzyz.im.dto.UserInfoDTO;
import com.zyzyz.im.entity.User;
import com.zyzyz.im.mapper.UserMapper;
import com.zyzyz.im.service.UserService;

@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void register(RegisterRequest registerRequest) {
        // 检查昵称是否已存在
        User existingUser = userMapper.selectByNickname(registerRequest.getNickname());
        if (existingUser != null) {
            throw new RuntimeException("昵称已被使用，请换一个昵称");
        }
        
        // 生成UUID作为userId
        String userId = UUID.randomUUID().toString().replace("-", "");
        
        userMapper.insert(User.builder()
        .userId(userId)
        .password(passwordEncoder.encode(registerRequest.getPassword()))
        .nickname(registerRequest.getNickname())
        .avatar(registerRequest.getAvatar())
        .email(registerRequest.getEmail())
        .status(1)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build());
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        // 通过昵称查询用户
        User user = userMapper.selectByNickname(loginRequest.getNickname());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        // Session 将在 Controller 层设置
        return LoginResponse.builder()
        .userId(user.getUserId())
        .nickname(user.getNickname())
        .avatar(user.getAvatar())
        .email(user.getEmail())
        .build();
    }

    @Override
    public User getUserByUserId(String userId) {
        return userMapper.selectByUserId(userId);
    }
    
    @Override
    public void updateUser(UpdateUserRequest updateUserRequest) {
        User user = User.builder()
                .userId(updateUserRequest.getUserId())
                .nickname(updateUserRequest.getNickname())
                .avatar(updateUserRequest.getAvatar())
                .email(updateUserRequest.getEmail())
                .updatedAt(LocalDateTime.now())
                .build();
        userMapper.updateUser(user);
    }

    @Override
    public UserInfoDTO getUserInfoByUserId(String userId) {
        User user = userMapper.selectByUserId(userId);
        return UserInfoDTO.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .build();
    }
    
    @Override
    public List<UserInfoDTO> getUserInfoByUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<User> users = userMapper.selectByUserIds(userIds);
        return users.stream()
                .map(user -> UserInfoDTO.builder()
                        .userId(user.getUserId())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatar())
                        .email(user.getEmail())
                        .build())
                .collect(Collectors.toList());
    }
}
