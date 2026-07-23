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
@Schema(title = "基础用户实体DTO", description = "存储系统所有用户的通用基础信息")
public class UserDTO {
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
     * 账号状态
     */
    @Schema(description = "账号状态：0-禁用、1-正常")
    private Integer status;

    /**
     * 用户类型
     */
    @Schema(description = "用户类型：1-普通用户、2-农户、3-管理员")
    private Integer type;
}
