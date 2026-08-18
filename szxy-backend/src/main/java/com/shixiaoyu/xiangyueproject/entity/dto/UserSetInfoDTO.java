package com.shixiaoyu.xiangyueproject.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息设置（登录后修改资料）
 * 全局 Jackson 为 SNAKE_CASE，请求体字段用 snake_case：username/password/phone/email/avatar
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "用户信息设置DTO")
public class UserSetInfoDTO {
    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码（明文，服务端 BCrypt 加密）")
    private String password;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像URL")
    private String avatar;
}
