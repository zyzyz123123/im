package com.zyzyz.im.service.impl;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.zyzyz.im.dto.RegisterRequest;
import com.zyzyz.im.dto.LoginRequest;
import com.zyzyz.im.dto.LoginResponse;
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
        userMapper.insert(User.builder()
        .userId(registerRequest.getUserId())
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
        User user = userMapper.selectByUserId(loginRequest.getUserId());
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
        .build();
    }

    @Override
    public User getUserByUserId(String userId) {
        return userMapper.selectByUserId(userId);
    }
}
