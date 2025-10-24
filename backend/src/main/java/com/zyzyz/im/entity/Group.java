package com.zyzyz.im.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群组实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Group {
    private Long id;
    private String groupId;
    private String groupName;
    private String creatorId;
    private String avatar;
    private String description;
    private Integer memberCount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

