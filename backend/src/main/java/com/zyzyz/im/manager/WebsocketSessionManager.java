package com.zyzyz.im.manager;

import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

@Component
public class WebsocketSessionManager {
    private final ConcurrentHashMap<String, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ONLINE_USERS_KEY = "im:online:users";

    public void addSession(String userId, WebSocketSession session) {
        // 直接替换旧连接，不主动关闭
        // 旧连接会在下次心跳检测或自然超时时关闭
        WebSocketSession oldSession = onlineUsers.put(userId, session);
        
        if (oldSession != null && oldSession.isOpen()) {
            System.out.println("用户 " + userId + " 已有连接，新连接已覆盖（旧连接将自然失效）");
        }

        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId);
    }
    
    public void removeSession(String userId) {
        onlineUsers.remove(userId);
        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);
    }
    
    public WebSocketSession getSession(String userId) {
        return onlineUsers.get(userId);
    }

    public boolean isOnline(String userId) {
        Boolean isMember = redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId);
        return Boolean.TRUE.equals(isMember);
    }

    public List<String> getOnlineUsers() {
        Set<Object> members = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
        if (members == null) {
            return Collections.emptyList();
        }
        return members.stream().map(Object::toString).collect(Collectors.toList());
    }

}
