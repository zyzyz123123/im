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
import com.zyzyz.im.service.GroupService;
import com.zyzyz.im.service.MessageSearchService;
import com.zyzyz.im.entity.Message;

import java.util.List;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private WebsocketSessionManager websocketSessionManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageService messageService;
    
    @Autowired
    private GroupService groupService;
    
    @Autowired
    private MessageSearchService messageSearchService;
    
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
        
        // 处理私聊消息
        if ("chat".equals(chatMessage.getType())) {
            handlePrivateChat(chatMessage, payload);
        }
        // 处理群聊消息
        else if ("group_chat".equals(chatMessage.getType())) {
            handleGroupChat(chatMessage, payload);
        }
    }
    
    /**
     * 处理私聊消息
     */
    private void handlePrivateChat(ChatMessage chatMessage, String payload) throws Exception {
        String toUserId = chatMessage.getToUserId();
        WebSocketSession targetUserSession = websocketSessionManager.getSession(toUserId);

        // 保存消息到数据库
        Message message = Message.builder()
                .fromUserId(chatMessage.getFromUserId())
                .toUserId(chatMessage.getToUserId())
                .content(chatMessage.getMessage())
                .messageId(UUID.randomUUID().toString())
                .messageType(1)  // 1-私聊
                .status(0)
                .groupId(null)
                .createdAt(LocalDateTime.now())
                .build();
        
        messageService.insert(message);
        
        // 索引到 ElasticSearch（异步，失败不影响主流程）
        try {
            messageSearchService.indexMessage(message);
            System.out.println("✅ 消息已索引到 ES: " + message.getMessageId());
        } catch (Exception e) {
            System.err.println("❌ 索引消息到 ES 失败: " + e.getMessage());
            e.printStackTrace();
        }

        // 发送给目标用户
        if (targetUserSession != null && targetUserSession.isOpen()) {
            targetUserSession.sendMessage(new TextMessage(payload));
        } else {
            System.out.println("targetUserSession is null or closed");
        }
    }
    
    /**
     * 处理群聊消息
     */
    private void handleGroupChat(ChatMessage chatMessage, String payload) throws Exception {
        String groupId = chatMessage.getGroupId();
        String fromUserId = chatMessage.getFromUserId();
        
        // 检查用户是否在群组中
        if (!groupService.isUserInGroup(groupId, fromUserId)) {
            System.out.println("用户不在群组中: " + fromUserId);
            return;
        }
        
        // 保存消息到数据库
        Message message = Message.builder()
                .fromUserId(chatMessage.getFromUserId())
                .toUserId(null)  // 群聊时 toUserId 为空
                .content(chatMessage.getMessage())
                .messageId(UUID.randomUUID().toString())
                .messageType(2)  // 2-群聊
                .status(0)
                .groupId(groupId)
                .createdAt(LocalDateTime.now())
                .build();
        
        messageService.insert(message);
        
        // 索引到 ElasticSearch
        try {
            messageSearchService.indexMessage(message);
            System.out.println("✅ 群消息已索引到 ES: " + message.getMessageId());
        } catch (Exception e) {
            System.err.println("❌ 索引群消息到 ES 失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 获取群组所有成员
        List<String> memberIds = groupService.getGroupMemberIds(groupId);
        System.out.println("群组成员列表: " + memberIds);
        
        // 广播给群组所有在线成员（除了发送者自己）
        int sentCount = 0;
        for (String memberId : memberIds) {
            if (!memberId.equals(fromUserId)) {  // 不发给自己
                WebSocketSession memberSession = websocketSessionManager.getSession(memberId);
                if (memberSession != null && memberSession.isOpen()) {
                    memberSession.sendMessage(new TextMessage(payload));
                    sentCount++;
                    System.out.println("已发送群消息给: " + memberId);
                } else {
                    System.out.println("成员不在线或Session已关闭: " + memberId);
                }
            }
        }
        
        System.out.println("群消息已广播: " + groupId + ", 总成员数: " + memberIds.size() + ", 实际发送: " + sentCount);
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
