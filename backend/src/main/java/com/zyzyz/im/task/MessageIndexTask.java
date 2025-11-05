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
    
    private static final int BATCH_SIZE = 500; // 每批处理500条
    
    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("========================================");
            System.out.println("开始索引历史消息到 ElasticSearch...");
            System.out.println("========================================");
            
            // 查询所有历史消息
            List<Message> allMessages = messageMapper.selectAll();
            
            if (allMessages != null && !allMessages.isEmpty()) {
                int totalCount = allMessages.size();
                System.out.println("📊 总共需要索引 " + totalCount + " 条消息");
                
                // 显示前3条消息作为示例
                if (totalCount > 0) {
                    System.out.println("\n示例消息（前3条）:");
                    for (int i = 0; i < Math.min(3, totalCount); i++) {
                        Message msg = allMessages.get(i);
                        System.out.println("  [" + (i+1) + "] messageId=" + msg.getMessageId() + 
                                         ", type=" + msg.getMessageType() + 
                                         ", from=" + msg.getFromUserId() + 
                                         ", content=" + (msg.getContent() != null && msg.getContent().length() > 20 
                                                        ? msg.getContent().substring(0, 20) + "..." 
                                                        : msg.getContent()));
                    }
                    System.out.println();
                }
                
                // 分批索引
                int batchCount = 0;
                int indexedCount = 0;
                
                for (int i = 0; i < totalCount; i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, totalCount);
                    List<Message> batch = allMessages.subList(i, end);
                    
                    try {
                        messageSearchService.indexMessages(batch);
                        batchCount++;
                        indexedCount += batch.size();
                        
                        System.out.println("✅ 第 " + batchCount + " 批索引完成：" + 
                                         batch.size() + " 条 (总进度: " + indexedCount + "/" + totalCount + ")");
                    } catch (Exception e) {
                        System.err.println("❌ 第 " + (batchCount + 1) + " 批索引失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                    
                    // 稍微延迟，避免对 ES 造成压力
                    if (i + BATCH_SIZE < totalCount) {
                        Thread.sleep(100);
                    }
                }
                
                System.out.println("========================================");
                System.out.println("🎉 历史消息索引完成！共索引 " + indexedCount + " 条消息，分 " + batchCount + " 批完成");
                System.out.println("========================================");
            } else {
                System.out.println("⚠️ 没有历史消息需要索引");
            }
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("❌ 索引历史消息失败（不影响系统运行）");
            System.err.println("错误信息: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();
        }
    }
}

