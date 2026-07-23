package com.shixiaoyu.xiangyueproject.entity.dto;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "用户信息设置DTO", description = "存储用户的通用基础信息")
public class UserSetInfoDTO {
    /**
     * 用户ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "用户ID（自增主键）")
    private Long id;

    /**
     * 用户名
     */
    @Schema(description = "登录用户名（唯一）")
    private String username;

    /*
     * 密码
     */
    @Schema(description = "登录密码（加密存储）")
    private String password;

    /**
     * 手机号
     */
    @Schema(description = "用户手机号（唯一）")
    private String phone;

    /**
     * 邮箱
     */
    @Schema(description = "用户邮箱（可选）")
    private String email;

    /**
     * 头像
     */
    @Schema(description = "用户头像URL地址")
    private String avator;
}
