package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shixiaoyu.xiangyueproject.enums.RoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 全角色统一账号
 * role：1游客 2农户 3村长 4管理员；status：1正常 0禁用
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("user")
@Schema(title = "基础用户实体", description = "存储系统所有用户的通用基础信息")
public class User {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "用户ID（自增主键）")
    private Long id;

    @Schema(description = "登录用户名（唯一）")
    private String username;

    @Schema(description = "登录密码（BCrypt 加密存储）")
    private String password;

    @Schema(description = "用户手机号（唯一）")
    private String phone;

    @Schema(description = "用户邮箱（唯一）")
    private String email;

    @Schema(description = "用户头像URL")
    private String avatar;

    @Schema(description = "角色：1游客 2农户 3村长 4管理员")
    private RoleEnum role;

    @Schema(description = "账号状态：0-禁用、1-正常")
    private Integer status;

    @Schema(description = "账户余额（元）")
    private BigDecimal balance;

    @Schema(description = "是否根据位置变化自动推荐：0关 1开")
    private Integer openPosAlter;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间（自动填充）")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "修改时间（自动填充）")
    private LocalDateTime updateTime;
}
