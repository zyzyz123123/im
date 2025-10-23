package com.zyzyz.im.handler;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.stereotype.Component;

import com.zyzyz.im.manager.WebsocketSessionManager;
import com.zyzyz.im.dto.ChatMessage;
import com.zyzyz.im.service.MessageService;
import com.zyzyz.im.entity.Message;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private WebsocketSessionManager websocketSessionManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageService messageService;
    
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        String userId = getUserIdFromSession(session);
        
        // 检查用户是否已经在线
        boolean wasOnline = websocketSessionManager.isOnline(userId);
        
        // 添加新连接（会关闭旧连接）
        websocketSessionManager.addSession(userId, session);
        
        // 只有当用户之前不在线时，才广播上线消息
        if (!wasOnline) {
            broadcastUserStatus(userId, "user_online");
            System.out.println("用户上线：" + userId);
        } else {
            System.out.println("用户重新连接：" + userId);
        }
    }
    
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        String userId = getUserIdFromSession(session);
        
        // 检查当前存储的 session 是否是这个 session
        // 如果不是，说明用户已经重新连接了，不要删除也不要广播下线
        WebSocketSession currentSession = websocketSessionManager.getSession(userId);
        if (currentSession == session) {
            // 是当前连接，真正下线
            websocketSessionManager.removeSession(userId);
            broadcastUserStatus(userId, "user_offline");
            System.out.println("用户下线：" + userId);
        } else {
            // 不是当前连接，说明是旧连接被关闭（用户已重连）
            System.out.println("旧连接关闭：" + userId + "（用户已重连，不广播下线）");
        }
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        String payload = message.getPayload();
        ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
        
        // 只处理聊天消息，系统消息不需要处理
        if (!"chat".equals(chatMessage.getType())) {
            return;
        }
        
        String toUserId = chatMessage.getToUserId();

        WebSocketSession targetUserSession = websocketSessionManager.getSession(toUserId);

        messageService.insert(Message.builder()
        .fromUserId(chatMessage.getFromUserId())
        .toUserId(chatMessage.getToUserId())
        .content(chatMessage.getMessage())
        .messageId(UUID.randomUUID().toString())
        .messageType(1)
        .status(0)
        .createdAt(LocalDateTime.now())
        .build());

        if (targetUserSession != null && targetUserSession.isOpen()) {
            targetUserSession.sendMessage(new TextMessage(payload));
        } else {
            System.out.println("targetUserSession is null or closed");
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
        String userId = getUserIdFromSession(session);
        websocketSessionManager.removeSession(userId);
    }

    /**
     * 从 session 的 attributes 中获取 userId
     * userId 是在 SessionHandshakeInterceptor 中从 HTTP Session 读取后放入的
     */
    private String getUserIdFromSession(WebSocketSession session) {
        return (String) session.getAttributes().get("userId");
    }
    
    /**
     * 广播用户上线/下线状态给所有在线用户
     */
    private void broadcastUserStatus(String userId, String statusType) {
        try {
            ChatMessage statusMessage = ChatMessage.builder()
                    .type(statusType)
                    .fromUserId(userId)
                    .toUserId(null)
                    .message(null)
                    .build();
            
            String messageJson = objectMapper.writeValueAsString(statusMessage);
            TextMessage textMessage = new TextMessage(messageJson);
            
            // 向所有在线用户广播
            for (String onlineUserId : websocketSessionManager.getOnlineUsers()) {
                // 不发给自己
                if (!onlineUserId.equals(userId)) {
                    WebSocketSession targetSession = websocketSessionManager.getSession(onlineUserId);
                    if (targetSession != null && targetSession.isOpen()) {
                        targetSession.sendMessage(textMessage);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("广播用户状态失败：" + e.getMessage());
        }
    }
    
}
