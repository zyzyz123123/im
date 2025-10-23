package com.zyzyz.im.interceptor;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.zyzyz.im.util.JwtUtil;

/**
 * WebSocket JWT 鉴权拦截器
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) throws Exception {
        
        String query = request.getURI().getQuery();
        
        if (query == null || query.isEmpty()) {
            System.out.println("WebSocket 连接被拒绝：缺少 token 参数");
            return false;
        }
        
        // 解析 token 参数
        String token = null;
        String[] params = query.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                token = keyValue[1];
                break;
            }
        }
        
        if (token == null || token.isEmpty()) {
            System.out.println("WebSocket 连接被拒绝：token 参数为空");
            return false;
        }
        
        // 验证 token
        if (!jwtUtil.validateToken(token)) {
            System.out.println("WebSocket 连接被拒绝：token 无效或已过期");
            return false;
        }
        
        // 从 token 中提取 userId
        String userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null || userId.isEmpty()) {
            System.out.println("WebSocket 连接被拒绝：无法从 token 中解析 userId");
            return false;
        }
        
        // 把 userId 放到 session attributes 中
        attributes.put("userId", userId);
        
        System.out.println("WebSocket 连接鉴权成功：userId = " + userId);
        return true;
    }
    
    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // 握手完成后的回调，可以留空
    }
}

