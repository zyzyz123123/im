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
        // 搜索该用户所有相关消息（包括私聊、群聊、AI对话）
        // 使用 bool 查询：支持模糊匹配、通配符、精确匹配
        Query query = Query.of(q -> q.bool(b -> b
                // 内容匹配：使用多种匹配方式
                .must(m -> m.bool(mb -> mb
                        // 1. IK 分词的 match 查询（适合中文）
                        .should(s -> s.match(mt -> mt
                                .field("content")
                                .query(keyword)
                        ))
                        // 2. wildcard 通配符查询（适合英文和短词）
                        .should(s -> s.wildcard(w -> w
                                .field("content")
                                .value("*" + keyword.toLowerCase() + "*")
                        ))
                        // 3. match_phrase 短语匹配（精确匹配）
                        .should(s -> s.matchPhrase(mp -> mp
                                .field("content")
                                .query(keyword)
                        ))
                        .minimumShouldMatch("1")  // 至少匹配一种方式
                ))
                // 用户过滤：发送者或接收者
                .should(s -> s.term(t -> t
                        .field("fromUserId")
                        .value(userId)
                ))
                .should(s -> s.term(t -> t
                        .field("toUserId")
                        .value(userId)
                ))
                .minimumShouldMatch("1")  // 至少匹配一个 should 条件（发送者或接收者）
        ));
        
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withMaxResults(100)  // 限制最多返回100条结果
                .build();
        
        System.out.println("🔍 执行搜索查询 - userId: " + userId + ", keyword: " + keyword);
        
        SearchHits<MessageDocument> hits = elasticsearchOperations.search(nativeQuery, MessageDocument.class);
        List<MessageDocument> results = hits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
        
        System.out.println("📊 ES 返回结果数: " + results.size());
        
        // 打印前3条结果作为调试
        if (results.isEmpty()) {
            System.out.println("⚠️ 未找到任何结果，可能原因：");
            System.out.println("   1. ES 索引中没有数据");
            System.out.println("   2. 用户 " + userId + " 没有相关消息");
            System.out.println("   3. 关键词 '" + keyword + "' 不匹配任何内容");
        } else {
            System.out.println("📝 前3条结果示例：");
            for (int i = 0; i < Math.min(3, results.size()); i++) {
                MessageDocument doc = results.get(i);
                String preview = doc.getContent() != null && doc.getContent().length() > 30
                        ? doc.getContent().substring(0, 30) + "..."
                        : doc.getContent();
                System.out.println("   [" + (i + 1) + "] from=" + doc.getFromUserId() + 
                                 ", content=" + preview);
            }
        }
        
        return results;
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

