package com.shixiaoyu.xiangyueproject.entity.vo;

import com.shixiaoyu.xiangyueproject.enums.RoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息 VO（不含密码）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "基础用户VO")
public class UserVO {
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

    @Schema(description = "是否根据位置变化自动推荐：0关 1开")
    private Integer openPosAlter;
}
