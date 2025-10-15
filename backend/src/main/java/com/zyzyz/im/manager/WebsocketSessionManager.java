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
        onlineUsers.put(userId, session);
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
