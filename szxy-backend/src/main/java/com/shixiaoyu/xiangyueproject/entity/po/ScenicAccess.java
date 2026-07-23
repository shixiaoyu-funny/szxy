package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "景区资质申请信息", description = "景区资质申请信息")
public class ScenicAccess {
    /**
     * 主键id
     */
    @Schema(description = "主键id")
    private Long id;
    /**
     * 申请人id
     */
    @Schema(description = "景区申请人ID（关联用户表主键）")
    private Long userId;
    /**
     * 关联村庄id
     */
    @Schema(description = "关联村落ID（关联村落基础表主键）")
    private Long villageId;

    /**
     * 景区名称
     */
    @Schema(description = "景区名称")
    private String name;

    /**
     * 景区介绍
     */
    @Schema(description = "景区详细介绍（文本）")
    private String intro;

    /**
     * 景区图片
     */
    @Schema(description = "景区封面图片URL（多张用逗号分隔）")
    private String image;

    /**
     * 景区价格
     */
    @Schema(description = "景区门票价格（单位：元，0表示免费）")
    private Integer price;

    /**
     * 景区类型
     */
    @Schema(description = "景区类型：1-自然景观、2-人文景观、3-娱乐体验、4-民俗体验")
    private Integer type;
    /**
     * 运营景区是否提供住宿
     */
    @Schema(description = "是否提供住宿：0-否、1-是")
    private Integer hasAccommodation;

    /**
     * 住宿信息
     */
    @Schema(description = "住宿详情（房型、价格、联系方式等）")
    private String accommodationInfo;

    @Schema(description = "审核状态: 0待审批 1通过 2拒绝)")
    private Integer status;
}
