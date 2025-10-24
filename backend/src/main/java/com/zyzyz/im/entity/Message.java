package com.zyzyz.im.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Message {

    private Long id;

    private String toUserId;

    private String fromUserId;

    private String content;
    
    private String messageId;

    private Integer messageType;  // 1-私聊, 2-群聊

    private Integer status;
    
    private String groupId;  // 群组ID（私聊时为null）
    
    private LocalDateTime createdAt;    
    
}
