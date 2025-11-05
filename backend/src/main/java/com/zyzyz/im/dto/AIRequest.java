package com.zyzyz.im.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI聊天请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AIRequest {
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户消息
     */
    private String message;
    
    /**
     * 是否清空对话历史
     */
    private Boolean clearHistory;
    
    /**
     * 图片URL（可选，用于图文对话）
     */
    private String imageUrl;
    
    /**
     * 文档ID（可选，用于文档对话，通义千问的file_id）
     */
    private String fileId;
}

