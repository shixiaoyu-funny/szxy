package com.shixiaoyu.xiangyueproject.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端用户分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(title = "用户查询DTO")
public class UserQueryDTO extends PageResultDTO {

    @Schema(description = "关键词（用户名/手机号/邮箱模糊匹配）")
    private String keyword;

    @Schema(description = "角色：1游客 2农户 3村长 4管理员")
    private Integer role;

    @Schema(description = "账号状态：0禁用 1正常")
    private Integer status;
}
