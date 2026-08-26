package com.shixiaoyu.xiangyueproject.entity.po;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 农户申请时的用户基础信息快照（序列化进 farmer_access.info）
 * 仅展示用，不参与条件查询
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "用户缓存信息", description = "申请单中的用户基础信息快照")
public class UserCacheInfo {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像URL")
    private String avatar;
}
