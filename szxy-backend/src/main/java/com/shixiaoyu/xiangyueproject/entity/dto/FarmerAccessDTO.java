package com.shixiaoyu.xiangyueproject.entity.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 农户资质申请。全局 Jackson 为 SNAKE_CASE，前端常用 camelCase 字段名，故用 JsonAlias 兼容两种命名。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "农户资质申请信息", description = "农户资质申请信息")
public class FarmerAccessDTO {
    /**
     * 用户ID
     */
    @Schema(description = "关联用户表的主键ID")
    @JsonAlias({"user_id", "userId"})
    private Long userId;
    /**
     * 农村名称
     */
    @Schema(description = "关联村落基础表的农村名称（须与 village_base.name 一致，也可传村落 id 数字串由服务端解析）")
    @JsonAlias({"village_name", "villageName"})
    private String villageName;

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
     * 农户类型
     */
    @Schema(description = "农户类型：1-民宿经营者、2-农产品销售者、3-文旅服务者")
    private Integer type;
}
