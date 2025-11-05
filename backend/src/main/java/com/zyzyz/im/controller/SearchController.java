package com.zyzyz.im.controller;

import com.zyzyz.im.common.Result;
import com.zyzyz.im.document.MessageDocument;
import com.zyzyz.im.repository.MessageSearchRepository;
import com.zyzyz.im.service.MessageSearchService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * 搜索控制器
 */
@RestController
@RequestMapping("/search")
public class SearchController {
    
    @Autowired
    private MessageSearchService messageSearchService;
    
    @Autowired
    private MessageSearchRepository searchRepository;
    
    /**
     * 测试 ES 连接和索引状态
     */
    @GetMapping("/test")
    public Result<Map<String, Object>> testElasticsearch(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        
        try {
            Map<String, Object> info = new HashMap<>();
            
            // 统计索引中的文档数量
            long totalCount = searchRepository.count();
            info.put("totalDocuments", totalCount);
            
            System.out.println("========================================");
            System.out.println("📊 ES 索引状态检查");
            System.out.println("========================================");
            System.out.println("总文档数: " + totalCount);
            
            if (totalCount == 0) {
                System.out.println("⚠️ 警告：ES 索引中没有任何文档！");
                System.out.println("可能原因：");
                System.out.println("  1. 数据库中没有消息");
                System.out.println("  2. 索引任务失败");
                System.out.println("  3. ES 连接问题");
                info.put("warning", "ES索引为空，请检查日志");
            } else {
                // 查询所有文档（限制10条）
                Iterable<MessageDocument> allDocs = searchRepository.findAll();
                List<MessageDocument> samples = StreamSupport.stream(allDocs.spliterator(), false)
                        .limit(10)
                        .toList();
                info.put("sampleDocuments", samples);
                
                System.out.println("\n示例文档（前10条）：");
                int index = 1;
                for (MessageDocument doc : samples) {
                    String preview = doc.getContent() != null && doc.getContent().length() > 40
                            ? doc.getContent().substring(0, 40) + "..."
                            : doc.getContent();
                    System.out.println("  [" + index++ + "] messageId=" + doc.getMessageId() + 
                                     ", type=" + doc.getMessageType() + 
                                     ", from=" + doc.getFromUserId() + 
                                     ", content=" + preview);
                }
                
                // 统计用户相关的文档数量
                long userRelatedCount = StreamSupport.stream(allDocs.spliterator(), false)
                        .filter(doc -> userId.equals(doc.getFromUserId()) || userId.equals(doc.getToUserId()))
                        .count();
                info.put("userRelatedDocuments", userRelatedCount);
                System.out.println("\n当前用户 (" + userId + ") 相关文档数: " + userRelatedCount);
            }
            
            System.out.println("========================================");
            
            return Result.success("ES连接正常", info);
        } catch (Exception e) {
            System.err.println("❌ ES连接测试失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("ES连接失败：" + e.getMessage());
        }
    }
    
    /**
     * 搜索消息
     */
    @GetMapping("/messages")
    public Result<List<MessageDocument>> searchMessages(
            @RequestParam String keyword,
            @RequestParam(required = false) String groupId,
            HttpSession session
    ) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return Result.error("未登录");
        }
        
        try {
            System.out.println("🔍 搜索请求 - userId: " + userId + ", keyword: " + keyword + ", groupId: " + groupId);
            
            List<MessageDocument> results;
            
            if (groupId != null && !groupId.isEmpty()) {
                // 搜索群组消息
                results = messageSearchService.searchGroupMessages(groupId, keyword);
                System.out.println("📊 群组搜索结果: " + results.size() + " 条");
            } else {
                // 搜索用户所有相关消息（包括私聊、群聊、AI对话）
                results = messageSearchService.searchAll(userId, keyword);
                System.out.println("📊 全局搜索结果: " + results.size() + " 条");
            }
            
            return Result.success("搜索成功", results);
        } catch (Exception e) {
            System.err.println("❌ 搜索失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("搜索失败：" + e.getMessage());
        }
    }
}

