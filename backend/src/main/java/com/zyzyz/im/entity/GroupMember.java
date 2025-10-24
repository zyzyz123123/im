package com.zyzyz.im.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群成员实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {
    private Long id;
    private String groupId;
    private String userId;
    private String role;        // owner-群主, admin-管理员, member-成员
    private String nickname;    // 群内昵称
    private LocalDateTime joinedAt;
}

