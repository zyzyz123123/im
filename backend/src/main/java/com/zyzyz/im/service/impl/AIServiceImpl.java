package com.zyzyz.im.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
    
    @Value("${ai.visionModel:qwen-vl-max}")
    private String visionModel;
    
    @Value("${ai.documentModel:qwen-long}")
    private String documentModel;
    
    @Value("${ai.fileUploadUrl:https://dashscope.aliyuncs.com/compatible-mode/v1/files}")
    private String fileUploadUrl;
    
    @Value("${ai.maxHistory:10}")
    private Integer maxHistory;
    
    @Value("${ai.minioEndpoint:http://localhost:9000}")
    private String minioEndpoint;
    
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
    
    @Override
    public AIResponse chatWithImage(String userId, String message, String imageUrl) {
        try {
            log.info("开始图文对话，用户：{}，消息：{}，图片：{}", userId, message, imageUrl);
            
            // 1. 立即保存用户消息到数据库
            saveUserImageMessage(userId, message, imageUrl);
            
            // 2. 从Redis获取历史对话
            List<AIChatMessage> history = getHistory(userId);
            
            // 3. 下载图片并转换为Base64（通义千问支持data:image格式）
            String imageBase64 = downloadAndEncodeImage(imageUrl);
            
            // 3. 构造包含图片的消息
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            
            // 构造content数组（包含文本和图片）
            List<Map<String, Object>> content = new ArrayList<>();
            
            // 添加文本内容
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", message);
            content.add(textContent);
            
            // 添加图片内容（使用Base64格式）
            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image_url");
            Map<String, String> imageUrlMap = new HashMap<>();
            imageUrlMap.put("url", imageBase64);  // data:image/jpeg;base64,xxxxx
            imageContent.put("image_url", imageUrlMap);
            content.add(imageContent);
            
            userMessage.put("content", content);
            
            // 3. 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", visionModel);  // 使用视觉模型
            
            List<Object> messages = new ArrayList<>();
            // 添加历史对话（纯文本）
            for (AIChatMessage msg : history) {
                Map<String, Object> historyMsg = new HashMap<>();
                historyMsg.put("role", msg.getRole());
                historyMsg.put("content", msg.getContent());
                messages.add(historyMsg);
            }
            // 添加当前消息（包含图片）
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            
            // 4. 调用通义千问视觉API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("调用AI Vision API，用户：{}", userId);
            
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
            
            log.info("AI Vision回复成功，用户：{}，tokens：{}", userId, tokensUsed);
            
            // 6. 保存到历史（简化为文本，方便后续对话）
            history.add(AIChatMessage.builder()
                    .role("user")
                    .content(message + " [图片]")
                    .build());
            
            history.add(AIChatMessage.builder()
                    .role("assistant")
                    .content(aiReply)
                    .build());
            
            // 7. 限制历史长度
            if (history.size() > maxHistory * 2) {
                history = history.subList(history.size() - maxHistory * 2, history.size());
            }
            
            // 8. 保存到Redis
            saveHistory(userId, history);
            
            // 9. 保存AI回复到数据库
            saveAIReply(userId, aiReply, tokensUsed);
            
            return AIResponse.builder()
                    .reply(aiReply)
                    .tokensUsed(tokensUsed)
                    .conversationRound(history.size() / 2)
                    .build();
                    
        } catch (Exception e) {
            log.error("AI图文对话失败，用户：{}，错误：{}", userId, e.getMessage(), e);
            throw new RuntimeException("AI图像理解服务暂时不可用，请稍后重试");
        }
    }
    
    /**
     * 保存用户的图文消息到数据库
     */
    private void saveUserImageMessage(String userId, String text, String imageUrl) {
        try {
            // 构造图文消息的JSON格式
            Map<String, Object> imageMessage = new HashMap<>();
            imageMessage.put("text", text);
            imageMessage.put("imageUrl", imageUrl);
            String imageMessageJson = objectMapper.writeValueAsString(imageMessage);
            
            // 保存用户消息（包含文本和图片）
            Message userMsg = Message.builder()
                    .messageId(UUID.randomUUID().toString())
                    .fromUserId(userId)
                    .toUserId(AI_ASSISTANT_ID)
                    .content(imageMessageJson)  // JSON格式：{"text":"...", "imageUrl":"..."}
                    .messageType(3)  // 3 = AI对话
                    .status(1)
                    .createdAt(LocalDateTime.now())
                    .build();
            messageService.insert(userMsg);
            
            log.info("用户图文消息已保存到数据库，用户：{}", userId);
        } catch (Exception e) {
            log.error("保存用户图文消息失败，用户：{}，错误：{}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 保存AI回复到数据库
     */
    private void saveAIReply(String userId, String aiReply, int tokensUsed) {
        try {
            Message aiMsg = Message.builder()
                    .messageId(UUID.randomUUID().toString())
                    .fromUserId(AI_ASSISTANT_ID)
                    .toUserId(userId)
                    .content(aiReply)
                    .messageType(3)
                    .status(1)
                    .createdAt(LocalDateTime.now())
                    .build();
            messageService.insert(aiMsg);
            
            log.info("AI回复已保存到数据库，用户：{}，tokens：{}", userId, tokensUsed);
        } catch (Exception e) {
            log.error("保存AI回复失败，用户：{}，错误：{}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 保存用户的文档消息到数据库
     */
    private void saveUserDocumentMessage(String userId, String text, String fileId, String fileName) {
        try {
            // 构造文档消息的JSON格式
            Map<String, Object> documentMessage = new HashMap<>();
            documentMessage.put("text", text);
            documentMessage.put("fileId", fileId);
            documentMessage.put("fileName", fileName != null ? fileName : "文档");
            String documentMessageJson = objectMapper.writeValueAsString(documentMessage);
            
            // 保存用户消息（包含文本和文档信息）
            Message userMsg = Message.builder()
                    .messageId(UUID.randomUUID().toString())
                    .fromUserId(userId)
                    .toUserId(AI_ASSISTANT_ID)
                    .content(documentMessageJson)  // JSON格式：{"text":"...", "fileId":"...", "fileName":"..."}
                    .messageType(3)  // 3 = AI对话
                    .status(1)
                    .createdAt(LocalDateTime.now())
                    .build();
            messageService.insert(userMsg);
            
            log.info("用户文档消息已保存到数据库，用户：{}", userId);
        } catch (Exception e) {
            log.error("保存用户文档消息失败，用户：{}，错误：{}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 下载图片并转换为Base64格式
     * 通义千问支持 data:image/jpeg;base64,xxx 格式
     */
    private String downloadAndEncodeImage(String imageUrl) {
        try {
            log.info("开始下载图片: {}", imageUrl);
            
            // 处理相对路径：将 /minio/bucket/file 转换为完整的 MinIO URL
            String actualUrl = imageUrl;
            if (imageUrl.startsWith("/minio/")) {
                // 去掉 /minio/ 前缀，使用配置的 MinIO 访问地址
                actualUrl = minioEndpoint + imageUrl.substring(6);
                log.info("转换相对路径为完整URL: {} -> {}", imageUrl, actualUrl);
            }
            
            // 下载图片
            URL url = new URL(actualUrl);
            InputStream inputStream = url.openStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            byte[] imageBytes = outputStream.toByteArray();
            inputStream.close();
            outputStream.close();
            
            // 转换为Base64
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            
            // 检测图片类型（简单判断）
            String mimeType = "image/jpeg";  // 默认jpeg
            if (imageUrl.toLowerCase().endsWith(".png")) {
                mimeType = "image/png";
            } else if (imageUrl.toLowerCase().endsWith(".gif")) {
                mimeType = "image/gif";
            } else if (imageUrl.toLowerCase().endsWith(".webp")) {
                mimeType = "image/webp";
            }
            
            // 构造data URL格式
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;
            
            log.info("图片转换为Base64成功，大小: {} bytes", imageBytes.length);
            
            return dataUrl;
            
        } catch (Exception e) {
            log.error("下载并转换图片失败: {}", e.getMessage(), e);
            throw new RuntimeException("图片处理失败，请确保图片URL可访问");
        }
    }
    
    @Override
    public String uploadDocument(byte[] fileBytes, String fileName) {
        try {
            log.info("开始上传文档到通义千问，文件名：{}，大小：{} bytes", fileName, fileBytes.length);
            
            // 构造multipart/form-data请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(apiKey);
            
            // 创建文件资源
            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
            
            // 构造请求body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);
            body.add("purpose", "file-extract");
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = 
                new HttpEntity<>(body, headers);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(
                fileUploadUrl,
                requestEntity,
                String.class
            );
            
            // 解析返回的file_id
            JsonNode root = objectMapper.readTree(response.getBody());
            String fileId = root.path("id").asText();
            
            if (fileId == null || fileId.isEmpty()) {
                throw new RuntimeException("上传文档失败，未获取到file_id");
            }
            
            log.info("文档上传成功，file_id：{}", fileId);
            return fileId;
            
        } catch (Exception e) {
            log.error("上传文档到通义千问失败：{}", e.getMessage(), e);
            throw new RuntimeException("文档上传失败：" + e.getMessage());
        }
    }
    
    @Override
    public AIResponse chatWithDocument(String userId, String message, String fileId, String fileName) {
        try {
            log.info("开始文档对话，用户：{}，消息：{}，fileId：{}，fileName：{}", userId, message, fileId, fileName);
            
            // 1. 立即保存用户消息到数据库
            saveUserDocumentMessage(userId, message, fileId, fileName);
            
            // 2. 从Redis获取历史对话
            List<AIChatMessage> history = getHistory(userId);
            
            // 2. 构建请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", documentModel);  // 使用qwen-long模型
            
            List<Object> messages = new ArrayList<>();
            
            // 添加system message（引用文档）
            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", "You are a helpful assistant.");
            messages.add(systemMsg);
            
            // 添加fileId引用
            Map<String, Object> fileRefMsg = new HashMap<>();
            fileRefMsg.put("role", "system");
            fileRefMsg.put("content", "fileid://" + fileId);
            messages.add(fileRefMsg);
            
            // 添加历史对话（纯文本）
            for (AIChatMessage msg : history) {
                Map<String, Object> historyMsg = new HashMap<>();
                historyMsg.put("role", msg.getRole());
                historyMsg.put("content", msg.getContent());
                messages.add(historyMsg);
            }
            
            // 添加用户消息
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", message);
            messages.add(userMsg);
            
            requestBody.put("messages", messages);
            
            // 3. 调用通义千问API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("调用qwen-long API，用户：{}", userId);
            
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            // 4. 解析响应
            JsonNode root = objectMapper.readTree(response.getBody());
            String aiReply = root.path("choices").get(0).path("message").path("content").asText();
            int tokensUsed = root.path("usage").path("total_tokens").asInt(0);
            
            log.info("文档对话成功，用户：{}，tokens：{}", userId, tokensUsed);
            
            // 5. 保存到历史
            history.add(AIChatMessage.builder()
                    .role("user")
                    .content(message + " [文档]")
                    .build());
            
            history.add(AIChatMessage.builder()
                    .role("assistant")
                    .content(aiReply)
                    .build());
            
            // 6. 限制历史长度
            if (history.size() > maxHistory * 2) {
                history = history.subList(history.size() - maxHistory * 2, history.size());
            }
            
            // 7. 保存到Redis
            saveHistory(userId, history);
            
            // 8. 保存AI回复到数据库
            saveAIReply(userId, aiReply, tokensUsed);
            
            return AIResponse.builder()
                    .reply(aiReply)
                    .tokensUsed(tokensUsed)
                    .conversationRound(history.size() / 2)
                    .build();
                    
        } catch (Exception e) {
            log.error("文档对话失败，用户：{}，错误：{}", userId, e.getMessage(), e);
            throw new RuntimeException("文档理解服务暂时不可用，请稍后重试");
        }
    }
}

