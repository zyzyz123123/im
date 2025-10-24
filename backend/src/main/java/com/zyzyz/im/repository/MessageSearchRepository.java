package com.zyzyz.im.repository;

import com.zyzyz.im.document.MessageDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 消息搜索 Repository
 */
@Repository
public interface MessageSearchRepository extends ElasticsearchRepository<MessageDocument, String> {
    
    /**
     * 根据内容搜索（全文搜索）
     */
    List<MessageDocument> findByContentContaining(String keyword);
    
    /**
     * 搜索用户相关的消息
     */
    List<MessageDocument> findByFromUserIdOrToUserIdAndContentContaining(
            String fromUserId, String toUserId, String keyword
    );
    
    /**
     * 搜索群组消息
     */
    List<MessageDocument> findByGroupIdAndContentContaining(String groupId, String keyword);
}

