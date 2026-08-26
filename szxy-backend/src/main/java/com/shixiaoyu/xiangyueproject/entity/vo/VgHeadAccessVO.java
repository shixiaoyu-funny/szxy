package com.shixiaoyu.xiangyueproject.entity.vo;

import com.shixiaoyu.xiangyueproject.enums.VgHeadStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 村长申请 VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "村长申请VO")
public class VgHeadAccessVO {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "村落ID")
    private Long villageId;

    @Schema(description = "村落名称")
    private String villageName;

    @Schema(description = "申请人 farm_user.id")
    private Long fuId;

    @Schema(description = "申请人 user.id")
    private Long userId;

    @Schema(description = "申请人用户名")
    private String username;

    @Schema(description = "申请人手机号")
    private String phone;

    @Schema(description = "申请人头像")
    private String avatar;

    @Schema(description = "状态")
    private VgHeadStatusEnum status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
