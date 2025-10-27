package com.zyzyz.im.service;

import com.zyzyz.im.dto.AIResponse;

/**
 * AI聊天服务接口
 */
public interface AIService {
    
    /**
     * 与AI聊天
     * 
     * @param userId 用户ID
     * @param message 用户消息
     * @return AI响应
     */
    AIResponse chat(String userId, String message);
    
    /**
     * 清空用户的对话历史
     * 
     * @param userId 用户ID
     */
    void clearHistory(String userId);
    
    /**
     * 获取用户的对话轮次
     * 
     * @param userId 用户ID
     * @return 对话轮次
     */
    Integer getConversationRound(String userId);
}

