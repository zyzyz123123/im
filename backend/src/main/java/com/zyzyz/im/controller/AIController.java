package com.zyzyz.im.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
     * 清空对话上下文（开始新话题，保留历史）
     */
    @PostMapping("/clear")
    public Result<Void> clearHistory(@RequestParam String userId) {
        log.info("清空对话上下文（开始新话题），用户：{}", userId);
        
        // 只清空Redis中的上下文，数据库记录保留
        aiService.clearHistory(userId);
        
        return Result.success();
    }
    
    /**
     * 删除所有对话记录（彻底删除）
     */
    @PostMapping("/delete")
    public Result<Void> deleteHistory(@RequestParam String userId) {
        log.info("删除所有AI对话记录，用户：{}", userId);
        
        // 清空Redis中的上下文
        aiService.clearHistory(userId);
        
        // 删除数据库中的历史记录
        try {
            messageService.deleteAIMessages(userId);
            log.info("已删除数据库中的AI对话记录，用户：{}", userId);
        } catch (Exception e) {
            log.error("删除数据库AI对话记录失败，用户：{}，错误：{}", userId, e.getMessage());
            return Result.error("删除失败");
        }
        
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
    
    /**
     * 与AI进行图文对话
     */
    @PostMapping("/chat-with-image")
    public Result<AIResponse> chatWithImage(@RequestBody AIRequest request) {
        log.info("收到AI图文对话请求，用户：{}，消息：{}，图片：{}", 
                 request.getUserId(), request.getMessage(), request.getImageUrl());
        
        if (request.getImageUrl() == null || request.getImageUrl().trim().isEmpty()) {
            return Result.error("图片URL不能为空");
        }
        
        AIResponse response = aiService.chatWithImage(
            request.getUserId(), 
            request.getMessage(), 
            request.getImageUrl()
        );
        return Result.success(response);
    }
    
    /**
     * 上传文档到通义千问，获取file_id
     */
    @PostMapping("/upload-document")
    public Result<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        log.info("收到文档上传请求，文件名：{}，大小：{}", file.getOriginalFilename(), file.getSize());
        
        try {
            byte[] fileBytes = file.getBytes();
            String fileId = aiService.uploadDocument(fileBytes, file.getOriginalFilename());
            log.info("文档上传成功，file_id：{}", fileId);
            return Result.success(fileId);
        } catch (Exception e) {
            log.error("文档上传失败：{}", e.getMessage(), e);
            return Result.error("文档上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 与AI进行文档对话
     */
    @PostMapping("/chat-with-document")
    public Result<AIResponse> chatWithDocument(@RequestBody AIRequest request) {
        log.info("收到AI文档对话请求，用户：{}，消息：{}，fileId：{}", 
                 request.getUserId(), request.getMessage(), request.getFileId());
        
        if (request.getFileId() == null || request.getFileId().trim().isEmpty()) {
            return Result.error("文档ID不能为空");
        }
        
        AIResponse response = aiService.chatWithDocument(
            request.getUserId(), 
            request.getMessage(), 
            request.getFileId()
        );
        return Result.success(response);
    }
}

