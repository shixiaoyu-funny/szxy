package com.shixiaoyu.xiangyueproject.entity.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "村长资质申请VO", description = "管理端查看的村长申请列表")
public class ManagerAccessVO {
    private Long id;
    @JsonAlias({"user_id", "userId"})
    private Long userId;

    @Schema(description = "申请人姓名")
    private String username; // 关联 user 表查询

    @Schema(description = "申请人手机号")
    private String phone;    // 关联 user 表查询

    @JsonAlias({"village_id", "villageId"})
    private Long villageId;

    @Schema(description = "农场/个人名称")
    @JsonAlias({"farm_name", "farmName"})
    private String farmName;

    @JsonAlias({"id_card", "idCard"})
    private String idCard;

    @Schema(description = "状态：0-待审批，1-通过，2-拒绝")
    private Integer status;

    private Integer type; // 申请表中的 type 字段
}