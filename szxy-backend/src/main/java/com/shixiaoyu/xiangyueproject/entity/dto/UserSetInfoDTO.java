package com.shixiaoyu.xiangyueproject.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息设置（登录后修改资料）
 * 请求体字段使用小驼峰：username/password/phone/email/avatar
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

    @Schema(description = "是否根据位置变化自动推荐：0关 1开")
    private Integer openPosAlter;
}
