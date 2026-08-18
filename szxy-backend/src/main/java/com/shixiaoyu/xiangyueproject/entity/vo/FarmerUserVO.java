package com.shixiaoyu.xiangyueproject.entity.vo;

import com.shixiaoyu.xiangyueproject.enums.BusinessTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 农户 VO（联查 user 取用户名/手机号）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "农户VO")
public class FarmerUserVO {
    @Schema(description = "农户档案ID")
    private Long id;

    @Schema(description = "关联 user.id")
    private Long userId;

    @Schema(description = "所属农村ID")
    private Long villageId;

    @Schema(description = "经营类型：1民宿 2农产品 3文旅")
    private BusinessTypeEnum businessType;

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
