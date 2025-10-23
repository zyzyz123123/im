package com.zyzyz.im.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage {
    private String type;        // 消息类型：chat(聊天消息) / user_online(用户上线) / user_offline(用户下线)
    private String fromUserId;
    private String toUserId;
    private String message;
}