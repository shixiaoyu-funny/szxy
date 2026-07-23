package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("farmer_access")
@Schema(title = "农户资质申请信息", description = "农户资质申请信息")
public class FarmerAccess {
    /**
     * 主键id
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键id")
    private Long id;
    /**
     * 用户ID
     */
    @Schema(description = "关联用户表的主键ID")
    private Long userId;
    /**
     * 农村id
     */
    @Schema(description = "关联村落基础表的主键ID")
    private Long villageId;

    /**
     *  农户姓名
     */
    @Schema(description = "农户真实姓名")
    private String farmName;

    /**
     * 身份证号码
     */
    @Schema(description = "农户身份证号码（脱敏存储）")
    private String idCard;
    /**
     * 农户类型
     */
    @Schema(description = "农户类型：1-民宿经营者、2-农产品销售者、3-文旅服务者")
    private Integer type;

    @Schema(description = "申请状态")
    private Integer status;
}
