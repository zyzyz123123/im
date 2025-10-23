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
}   