package com.zyzyz.im.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI聊天响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AIResponse {
    /**
     * AI回复内容
     */
    private String reply;
    
    /**
     * 本次消耗的tokens（可选）
     */
    private Integer tokensUsed;
    
    /**
     * 对话轮次
     */
    private Integer conversationRound;
}

