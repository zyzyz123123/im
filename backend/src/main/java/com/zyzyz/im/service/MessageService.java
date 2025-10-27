package com.zyzyz.im.service;

import java.util.List;

import com.zyzyz.im.entity.Message;

public interface MessageService {
    void insert(Message message);
    Message selectByMessageId(String messageId);
    List<Message> selectByUsers(String fromUserId, String toUserId);
    List<Message> selectUnreadByUserId(String userId);
    void updateStatus(String messageId, Integer status);
    void batchUpdateStatusByUsers(String fromUserId, String toUserId);
    
    /**
     * 查询用户的最近联系人列表
     */
    List<String> selectRecentContactsByUserId(String userId);
    
    /**
     * 查询群聊历史消息
     */
    List<Message> selectByGroupId(String groupId);
    
    /**
     * 删除用户的AI对话记录
     */
    void deleteAIMessages(String userId);
}   