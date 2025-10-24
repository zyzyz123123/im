package com.zyzyz.im.controller;

import com.zyzyz.im.common.Result;
import com.zyzyz.im.document.MessageDocument;
import com.zyzyz.im.service.MessageSearchService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 搜索控制器
 */
@RestController
@RequestMapping("/search")
public class SearchController {
    
    @Autowired
    private MessageSearchService messageSearchService;
    
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
            List<MessageDocument> results;
            
            if (groupId != null && !groupId.isEmpty()) {
                // 搜索群组消息
                results = messageSearchService.searchGroupMessages(groupId, keyword);
            } else {
                // 搜索用户所有消息
                results = messageSearchService.searchUserMessages(userId, keyword);
            }
            
            return Result.success("搜索成功", results);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("搜索失败：" + e.getMessage());
        }
    }
}

