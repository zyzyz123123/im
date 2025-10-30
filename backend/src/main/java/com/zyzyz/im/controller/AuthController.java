package com.zyzyz.im.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zyzyz.im.dto.LoginResponse;
import com.zyzyz.im.dto.UpdateUserRequest;
import com.zyzyz.im.entity.User;
import com.zyzyz.im.service.UserService;
import com.zyzyz.im.dto.RegisterRequest;
import com.zyzyz.im.dto.LoginRequest;
import com.zyzyz.im.common.Result;


@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterRequest registerRequest) {
        try {
            // 昵称唯一性检查在 UserService 中进行
            userService.register(registerRequest);
            return Result.success("注册成功");
        } catch (RuntimeException e) {
            // 捕获服务层抛出的异常（如昵称已存在）
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest loginRequest, HttpSession session) {
        try {
            LoginResponse loginResponse = userService.login(loginRequest);
            // 登录成功，将用户信息存入 Session
            session.setAttribute("userId", loginResponse.getUserId());
            session.setAttribute("nickname", loginResponse.getNickname());
            System.out.println("用户登录成功：" + loginResponse.getUserId() + ", SessionID: " + session.getId());
            return Result.success("登录成功", loginResponse);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/logout")
    public Result<String> logout(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId != null) {
            System.out.println("用户登出：" + userId);
        }
        session.invalidate(); // 清除 Session
        return Result.success("登出成功");
    }
    
    @PostMapping("/updateProfile")
    public Result<User> updateProfile(@RequestBody UpdateUserRequest updateUserRequest, HttpSession session) {
        try {
            // 更新用户信息
            userService.updateUser(updateUserRequest);
            
            // 更新Session中的信息
            session.setAttribute("nickname", updateUserRequest.getNickname());
            
            // 返回更新后的用户信息
            User updatedUser = userService.getUserByUserId(updateUserRequest.getUserId());
            return Result.success("更新成功", updatedUser);
        } catch (Exception e) {
            return Result.error("更新失败：" + e.getMessage());
        }
    }
}
