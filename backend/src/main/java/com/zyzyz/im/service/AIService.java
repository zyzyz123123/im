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
    
    /**
     * 与AI进行图文对话
     * 
     * @param userId 用户ID
     * @param message 用户消息
     * @param imageUrl 图片URL
     * @return AI响应
     */
    AIResponse chatWithImage(String userId, String message, String imageUrl);
    
    /**
     * 上传文档到通义千问，获取file_id
     * 
     * @param fileBytes 文件字节数组
     * @param fileName 文件名
     * @return file_id
     */
    String uploadDocument(byte[] fileBytes, String fileName);
    
    /**
     * 与AI进行文档对话
     * 
     * @param userId 用户ID
     * @param message 用户消息
     * @param fileId 通义千问的file_id
     * @param fileName 文档名称
     * @return AI响应
     */
    AIResponse chatWithDocument(String userId, String message, String fileId, String fileName);
}

