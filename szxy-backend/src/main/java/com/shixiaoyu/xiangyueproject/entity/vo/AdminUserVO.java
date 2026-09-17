package com.shixiaoyu.xiangyueproject.entity.vo;

import com.shixiaoyu.xiangyueproject.enums.RoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端用户列表 VO（不含密码）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "管理端用户VO")
public class AdminUserVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "角色：1游客 2农户 3村长 4管理员")
    private RoleEnum role;

    @Schema(description = "账号状态：0禁用 1正常")
    private Integer status;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
