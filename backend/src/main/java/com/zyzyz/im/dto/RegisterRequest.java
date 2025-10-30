package com.zyzyz.im.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {
    // userId 由系统自动生成，不需要用户提供
    private String password;
    private String nickname;  // 昵称，作为登录凭证，需要唯一
    private String avatar;
    private String email;
}
