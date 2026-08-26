package com.shixiaoyu.xiangyueproject.entity.vo;

import com.shixiaoyu.xiangyueproject.entity.po.UserCacheInfo;
import com.shixiaoyu.xiangyueproject.enums.AccessStatusEnum;
import com.shixiaoyu.xiangyueproject.enums.BusinessTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 农户准入申请 VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "农户申请VO")
public class FarmerAccessVO {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "申请人 user.id")
    private Long userId;

    @Schema(description = "用户基础信息快照")
    private UserCacheInfo userCacheInfo;

    @Schema(description = "身份证号")
    private String idCard;

    @Schema(description = "所属农村ID")
    private Long villageId;

    @Schema(description = "所属农村名称")
    private String villageName;

    @Schema(description = "经营类型")
    private BusinessTypeEnum businessType;

    @Schema(description = "申请状态")
    private AccessStatusEnum status;

    @Schema(description = "审批管理员ID")
    private Long auditUserId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间（改提/审批）")
    private LocalDateTime updateTime;
}
