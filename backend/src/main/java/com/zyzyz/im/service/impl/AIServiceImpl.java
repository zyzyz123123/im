package com.zyzyz.im.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyzyz.im.dto.AIChatMessage;
import com.zyzyz.im.dto.AIResponse;
import com.zyzyz.im.entity.Message;
import com.zyzyz.im.service.AIService;
import com.zyzyz.im.service.MessageService;

import lombok.extern.slf4j.Slf4j;

/**
 * AI聊天服务实现（通义千问）
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {
    
    // 通义千问API配置
    @Value("${ai.apiKey}")
    private String apiKey;
    
    @Value("${ai.apiUrl:https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${ai.model:qwen-plus}")
    private String model;
    
    @Value("${ai.maxHistory:10}")
    private Integer maxHistory;
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    private MessageService messageService;
    
    // Redis key前缀
    private static final String REDIS_KEY_PREFIX = "ai:chat:history:";
    private static final long HISTORY_EXPIRE_HOURS = 24; // 对话历史保留24小时
    private static final String AI_ASSISTANT_ID = "AI_ASSISTANT";
    
    public AIServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public AIResponse chat(String userId, String message) {
        try {
            // 1. 从Redis获取历史对话
            List<AIChatMessage> history = getHistory(userId);
            
            // 2. 添加用户消息
            history.add(AIChatMessage.builder()
                    .role("user")
                    .content(message)
                    .build());
            
            // 3. 构建请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", history);
            
            // 4. 调用通义千问API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("调用AI API，用户：{}，消息：{}", userId, message);
            
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            // 5. 解析响应
            JsonNode root = objectMapper.readTree(response.getBody());
            String aiReply = root.path("choices").get(0).path("message").path("content").asText();
            int tokensUsed = root.path("usage").path("total_tokens").asInt(0);
            
            // 6. 保存AI回复到历史
            history.add(AIChatMessage.builder()
                    .role("assistant")
                    .content(aiReply)
                    .build());
            
            // 7. 限制历史长度（只保留最近N轮对话）
            if (history.size() > maxHistory * 2) {
                history = history.subList(history.size() - maxHistory * 2, history.size());
            }
            
            // 8. 保存到Redis
            saveHistory(userId, history);
            
            // 9. 保存到数据库（持久化）
            saveToDatabase(userId, message, aiReply, tokensUsed);
            
            log.info("AI回复成功，用户：{}，tokens：{}", userId, tokensUsed);
            
            return AIResponse.builder()
                    .reply(aiReply)
                    .tokensUsed(tokensUsed)
                    .conversationRound(history.size() / 2)
                    .build();
                    
        } catch (Exception e) {
            log.error("AI聊天失败，用户：{}，错误：{}", userId, e.getMessage(), e);
            throw new RuntimeException("AI服务暂时不可用，请稍后重试");
        }
    }
    
    @Override
    public void clearHistory(String userId) {
        String key = REDIS_KEY_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("清空对话历史，用户：{}", userId);
    }
    
    @Override
    public Integer getConversationRound(String userId) {
        List<AIChatMessage> history = getHistory(userId);
        return history.size() / 2;
    }
    
    /**
     * 从Redis获取对话历史
     */
    @SuppressWarnings("unchecked")
    private List<AIChatMessage> getHistory(String userId) {
        String key = REDIS_KEY_PREFIX + userId;
        Object obj = redisTemplate.opsForValue().get(key);
        
        if (obj instanceof List) {
            return (List<AIChatMessage>) obj;
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 保存对话历史到Redis
     */
    private void saveHistory(String userId, List<AIChatMessage> history) {
        String key = REDIS_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, history, HISTORY_EXPIRE_HOURS, TimeUnit.HOURS);
    }
    
    /**
     * 保存AI对话到数据库
     * messageType = 3 表示AI对话
     */
    private void saveToDatabase(String userId, String userMessage, String aiReply, int tokensUsed) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 保存用户消息
            Message userMsg = Message.builder()
                    .messageId(UUID.randomUUID().toString())
                    .fromUserId(userId)
                    .toUserId(AI_ASSISTANT_ID)
                    .content(userMessage)
                    .messageType(3)  // 3 = AI对话
                    .status(1)  // 已读（AI消息不需要未读状态）
                    .createdAt(now)
                    .build();
            messageService.insert(userMsg);
            
            // 保存AI回复
            Message aiMsg = Message.builder()
                    .messageId(UUID.randomUUID().toString())
                    .fromUserId(AI_ASSISTANT_ID)
                    .toUserId(userId)
                    .content(aiReply)
                    .messageType(3)  // 3 = AI对话
                    .status(1)  // 已读
                    .createdAt(now)
                    .build();
            messageService.insert(aiMsg);
            
            log.info("AI对话已保存到数据库，用户：{}，tokens：{}", userId, tokensUsed);
        } catch (Exception e) {
            // 数据库保存失败不影响主流程
            log.error("保存AI对话到数据库失败，用户：{}，错误：{}", userId, e.getMessage(), e);
        }
    }
}

