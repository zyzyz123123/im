package com.zyzyz.im.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI聊天消息（用于构建对话历史）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AIChatMessage {
    /**
     * 角色：system（系统提示）、user（用户）、assistant（AI助手）
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
}

