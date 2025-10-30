package com.zyzyz.im.interceptor;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * WebSocket 握手拦截器 - 使用 Session 进行认证
 */
@Component
public class SessionHandshakeInterceptor implements HandshakeInterceptor {
    
    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes
    ) throws Exception {
        
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            
            String userId = null;
            
            // 方案1：优先从 URL 参数中获取 userId（用于 WebSocket 连接）
            userId = httpRequest.getParameter("userId");
            if (userId != null && !userId.isEmpty()) {
                attributes.put("userId", userId);
                System.out.println("WebSocket 连接鉴权成功（URL参数）：userId = " + userId);
                return true;
            }
            
            // 方案2：从 HTTP Session 中获取 userId（兼容旧方式）
            HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                userId = (String) session.getAttribute("userId");
                if (userId != null && !userId.isEmpty()) {
                    attributes.put("userId", userId);
                    System.out.println("WebSocket 连接鉴权成功（Session）：userId = " + userId + ", SessionID: " + session.getId());
                    return true;
                }
            }
            
            System.err.println("WebSocket 连接鉴权失败：未登录或 Session 已过期");
            return false;
        }
        
        return false;
    }
    
    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @Nullable Exception exception
    ) {
        // 握手完成后的回调，可以留空
    }
}

