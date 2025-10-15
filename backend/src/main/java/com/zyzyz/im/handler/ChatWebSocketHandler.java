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
        websocketSessionManager.addSession(userId, session);
    }
    
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        String userId = getUserIdFromSession(session);
        websocketSessionManager.removeSession(userId);
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        String payload = message.getPayload();
        ChatMessage chatMessage = objectMapper.readValue(payload, ChatMessage.class);
        
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

    private String getUserIdFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query == null) {
            return null;
        }
        
        // 按 & 分割参数
        String[] params = query.split("&");
        for (String param : params) {
            if (param.startsWith("userId=")) {
                return param.substring(7);  // 去掉 "userId="
            }
        }
        
        return null;
    }
    
}
