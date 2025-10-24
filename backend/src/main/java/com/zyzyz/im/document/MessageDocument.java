package com.zyzyz.im.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * 消息文档（ElasticSearch）
 * 用于全文搜索
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "im_messages")
public class MessageDocument {
    
    @Id
    private String messageId;
    
    @Field(type = FieldType.Keyword)
    private String fromUserId;
    
    @Field(type = FieldType.Keyword)
    private String toUserId;
    
    @Field(type = FieldType.Keyword)
    private String groupId;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;  // 全文搜索字段（使用 IK 中文分词器）
    
    @Field(type = FieldType.Integer)
    private Integer messageType;  // 1-私聊, 2-群聊
    
    @Field(type = FieldType.Keyword)
    private String createdAt;  // 使用 String 存储日期，避免格式转换问题
}

