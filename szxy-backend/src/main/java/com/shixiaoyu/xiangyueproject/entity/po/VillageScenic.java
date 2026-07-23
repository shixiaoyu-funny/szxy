package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "村落景区实体", description = "存储村落下属景区的基础信息和审核状态")
public class VillageScenic {
    /**
     * 景区ID
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "景区ID（自增主键）")
    private Long id;
    /**
     * 关联用户id
     */
    @Schema(description = "关联用户ID（关联用户基础表主键）")
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
     * 审核拒绝原因
     */
    @Schema(description = "景区审核拒绝时的原因说明（审核通过则为空）")
    private String rejectReason;

    /**
     * 审核状态
     */
    @Schema(description = "审核状态：0-待审核、1-审核通过、2-审核拒绝")
    private Integer status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间（自动填充）")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间（自动填充）")
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
    /**
     * 收藏量
     */
    @Schema(description = "收藏量")
    private Integer collections;
    /**
     * 点赞量
     */
    @Schema(description = "点赞量")
    private Integer likes;

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
}
