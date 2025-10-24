package com.zyzyz.im.service;

import com.zyzyz.im.document.MessageDocument;
import com.zyzyz.im.entity.Message;

import java.util.List;

/**
 * 消息搜索服务
 */
public interface MessageSearchService {
    
    /**
     * 索引单条消息
     */
    void indexMessage(Message message);
    
    /**
     * 批量索引消息（用于初始化）
     */
    void indexMessages(List<Message> messages);
    
    /**
     * 搜索用户的消息
     */
    List<MessageDocument> searchUserMessages(String userId, String keyword);
    
    /**
     * 搜索群组消息
     */
    List<MessageDocument> searchGroupMessages(String groupId, String keyword);
    
    /**
     * 全局搜索（用户相关的所有消息）
     */
    List<MessageDocument> searchAll(String userId, String keyword);
}

