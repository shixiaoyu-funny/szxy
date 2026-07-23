package com.shixiaoyu.xiangyueproject.entity.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "农户资质申请VO", description = "用于管理端展示申请记录，包含基础用户信息")
public class FarmerAccessVO {

    @Schema(description = "申请记录ID")
    private Long id;

    @Schema(description = "关联用户ID")
    @JsonAlias({"user_id", "userId"})
    private Long userId;

    /**
     * 关键：这是从 User 表关联查出来的，方便管理员审核时知道是谁
     */
    @Schema(description = "申请人姓名")
    private String username;

    @Schema(description = "申请人手机号")
    private String phone;

    @Schema(description = "关联村落ID")
    @JsonAlias({"village_id", "villageId"})
    private Long villageId;

    @Schema(description = "农场/名称")
    @JsonAlias({"farm_name", "farmName"})
    private String farmName;

    @Schema(description = "身份证号码")
    @JsonAlias({"id_card", "idCard"})
    private String idCard;

    @Schema(description = "申请类型：1-民宿经营者、2-农产品销售者、3-文旅服务者")
    private Integer type;

    @Schema(description = "申请时间")
    @JsonAlias({"create_time", "createTime"})
    private LocalDateTime createTime;
    /**
     * 状态字段：如果你数据库 farmer_access 后来加了 status (0-待审, 1-通过, 2-驳回)
     */
    @Schema(description = "申请状态：0-待审核、1-已通过、2-已拒绝")
    private Integer status;
}