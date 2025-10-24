package com.zyzyz.im.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群组响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {
    private String groupId;
    private String groupName;
    private String creatorId;
    private String avatar;
    private String description;
    private Integer memberCount;
    private LocalDateTime createdAt;
    private List<String> memberIds; // 成员ID列表（可选）
}

