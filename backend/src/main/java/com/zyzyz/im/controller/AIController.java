package com.zyzyz.im.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zyzyz.im.common.Result;
import com.zyzyz.im.dto.AIRequest;
import com.zyzyz.im.dto.AIResponse;
import com.zyzyz.im.entity.Message;
import com.zyzyz.im.service.AIService;
import com.zyzyz.im.service.MessageService;

import lombok.extern.slf4j.Slf4j;

/**
 * AI聊天控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AIController {
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private MessageService messageService;
    
    /**
     * 与AI聊天
     */
    @PostMapping("/chat")
    public Result<AIResponse> chat(@RequestBody AIRequest request) {
        log.info("收到AI聊天请求，用户：{}，消息：{}", request.getUserId(), request.getMessage());
        
        // 如果需要清空历史
        if (Boolean.TRUE.equals(request.getClearHistory())) {
            aiService.clearHistory(request.getUserId());
        }
        
        AIResponse response = aiService.chat(request.getUserId(), request.getMessage());
        return Result.success(response);
    }
    
    /**
     * 清空对话历史
     */
    @PostMapping("/clear")
    public Result<Void> clearHistory(@RequestParam String userId) {
        log.info("清空对话历史，用户：{}", userId);
        aiService.clearHistory(userId);
        return Result.success();
    }
    
    /**
     * 获取对话轮次
     */
    @GetMapping("/round")
    public Result<Integer> getConversationRound(@RequestParam String userId) {
        Integer round = aiService.getConversationRound(userId);
        return Result.success(round);
    }
    
    /**
     * 获取AI聊天历史记录
     */
    @GetMapping("/history")
    public Result<List<Message>> getHistory(@RequestParam String userId) {
        log.info("查询AI聊天历史，用户：{}", userId);
        // 查询与AI的对话历史 (messageType = 3)
        List<Message> messages = messageService.selectByUsers(userId, "AI_ASSISTANT");
        return Result.success(messages);
    }
}

