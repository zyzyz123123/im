package com.zyzyz.im.service;

import com.zyzyz.im.dto.RegisterRequest;
import com.zyzyz.im.dto.LoginRequest;
import com.zyzyz.im.entity.User;
import com.zyzyz.im.dto.LoginResponse;

public interface UserService {
    void register(RegisterRequest registerRequest);
    LoginResponse login(LoginRequest loginRequest);
    User getUserByUserId(String userId);
}
