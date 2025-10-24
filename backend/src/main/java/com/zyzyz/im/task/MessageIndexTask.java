package com.zyzyz.im.task;

import com.zyzyz.im.entity.Message;
import com.zyzyz.im.mapper.MessageMapper;
import com.zyzyz.im.service.MessageSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 消息索引初始化任务
 * 应用启动时，将所有历史消息索引到 ElasticSearch
 */
@Component
public class MessageIndexTask implements CommandLineRunner {
    
    @Autowired
    private MessageMapper messageMapper;
    
    @Autowired
    private MessageSearchService messageSearchService;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("开始索引历史消息到 ElasticSearch...");
            
            // 查询所有历史消息
            List<Message> allMessages = messageMapper.selectAll();
            
            if (allMessages != null && !allMessages.isEmpty()) {
                messageSearchService.indexMessages(allMessages);
                System.out.println("✅ 历史消息索引完成！共索引 " + allMessages.size() + " 条消息");
            } else {
                System.out.println("没有历史消息需要索引");
            }
        } catch (Exception e) {
            System.err.println("❌ 索引历史消息失败（不影响系统运行）: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

