package com.shixiaoyu.xiangyueproject.entity.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "农户用户实体", description = "存储农户的基础信息和审核状态")
public class FarmerUserDTO {
    /**
     * 农户ID
     */
    @Schema(description = "农户ID（自增主键）")
    private Long id;

    /**
     * 用户ID
     */
    @Schema(description = "关联用户表的主键ID")
    @JsonAlias({"user_id", "userId"})
    private Long userId;

    /**
     * 农村id
     */
    @Schema(description = "关联村落基础表的主键ID")
    @JsonAlias({"village_id", "villageId"})
    private Long villageId;

    /**
     *  农户姓名
     */
    @Schema(description = "农户真实姓名")
    @JsonAlias({"farm_name", "farmName"})
    private String farmName;

    /**
     * 身份证号码
     */
    @Schema(description = "农户身份证号码（脱敏存储）")
    @JsonAlias({"id_card", "idCard"})
    private String idCard;

    /**
     * 账号状态
     */
    @Schema(description = "账号状态：0-待审核、1-已通过、2-已拒绝、3-已禁用")
    private Integer status;

    /**
     * 审核拒绝原因
     */
    @Schema(description = "审核拒绝时的原因说明")
    @JsonAlias({"reject_reason", "rejectReason"})
    private String rejectReason;

    /**
     * 农户类型
     */
    @Schema(description = "农户类型：1-民宿经营者、2-农产品销售者、3-文旅服务者")
    private Integer type;

    /**
     * 用户名
     */
    @Schema(description = "登录用户名（唯一）")
    private String username;

    /**
     * 手机号
     */
    @Schema(description = "用户手机号（唯一）")
    private String phone;
}
