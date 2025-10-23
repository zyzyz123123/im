package com.zyzyz.im.manager;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.stereotype.Component;

@Component
public class WebsocketSessionManager {
    private final ConcurrentHashMap<String, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();

    public void addSession(String userId, WebSocketSession session) {
        // 直接替换旧连接，不主动关闭
        // 旧连接会在下次心跳检测或自然超时时关闭
        WebSocketSession oldSession = onlineUsers.put(userId, session);
        
        if (oldSession != null && oldSession.isOpen()) {
            System.out.println("用户 " + userId + " 已有连接，新连接已覆盖（旧连接将自然失效）");
        }
    }
    
    public void removeSession(String userId) {
        onlineUsers.remove(userId);
    }
    
    public WebSocketSession getSession(String userId) {
        return onlineUsers.get(userId);
    }

    public boolean isOnline(String userId) {
        return onlineUsers.containsKey(userId);
    }

    public List<String> getOnlineUsers() {
        return new ArrayList<>(onlineUsers.keySet());
    }
    
}
