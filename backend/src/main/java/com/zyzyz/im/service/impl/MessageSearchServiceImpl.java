package com.zyzyz.im.service.impl;

import com.zyzyz.im.document.MessageDocument;
import com.zyzyz.im.entity.Message;
import com.zyzyz.im.repository.MessageSearchRepository;
import com.zyzyz.im.service.MessageSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageSearchServiceImpl implements MessageSearchService {
    
    @Autowired
    private MessageSearchRepository searchRepository;
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    
    @Override
    public void indexMessage(Message message) {
        MessageDocument doc = convertToDocument(message);
        searchRepository.save(doc);
    }
    
    @Override
    public void indexMessages(List<Message> messages) {
        List<MessageDocument> docs = messages.stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());
        searchRepository.saveAll(docs);
    }
    
    @Override
    public List<MessageDocument> searchUserMessages(String userId, String keyword) {
        // 使用原生查询，利用 IK 分词的 match 查询
        Query query = Query.of(q -> q.bool(b -> b
                .must(m -> m.match(mt -> mt
                        .field("content")
                        .query(keyword)  // IK 分词的 match 查询
                ))
                .must(m -> m.term(t -> t
                        .field("messageType")
                        .value(1)  // 私聊
                ))
                .should(s -> s.term(t -> t
                        .field("fromUserId")
                        .value(userId)
                ))
                .should(s -> s.term(t -> t
                        .field("toUserId")
                        .value(userId)
                ))
                .minimumShouldMatch("1")  // 至少匹配一个 should 条件
        ));
        
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .build();
        
        SearchHits<MessageDocument> hits = elasticsearchOperations.search(nativeQuery, MessageDocument.class);
        
        return hits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<MessageDocument> searchGroupMessages(String groupId, String keyword) {
        // 使用原生查询，利用 IK 分词的 match 查询
        Query query = Query.of(q -> q.bool(b -> b
                .must(m -> m.match(mt -> mt
                        .field("content")
                        .query(keyword)  // IK 分词的 match 查询
                ))
                .must(m -> m.term(t -> t
                        .field("groupId")
                        .value(groupId)
                ))
                .must(m -> m.term(t -> t
                        .field("messageType")
                        .value(2)  // 群聊
                ))
        ));
        
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .build();
        
        return elasticsearchOperations.search(nativeQuery, MessageDocument.class).stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<MessageDocument> searchAll(String userId, String keyword) {
        // 搜索该用户所有相关消息（使用 match 查询，利用 IK 分词）
        Query query = Query.of(q -> q.match(m -> m
                .field("content")
                .query(keyword)
        ));
        
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .build();
        
        return elasticsearchOperations.search(nativeQuery, MessageDocument.class).stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }   
    
    /**
     * 将 Message 转换为 MessageDocument
     */
    private MessageDocument convertToDocument(Message message) {
        return MessageDocument.builder()
                .messageId(message.getMessageId())
                .fromUserId(message.getFromUserId())
                .toUserId(message.getToUserId())
                .groupId(message.getGroupId())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt().toString() : null)
                .build();
    }
}

