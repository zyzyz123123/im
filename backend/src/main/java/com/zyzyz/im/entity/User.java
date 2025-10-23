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
public class User {

    private Long id;

    private String userId;

    private String password;

    private String nickname;

    private String avatar;

    private String email;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
