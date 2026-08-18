package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shixiaoyu.xiangyueproject.enums.ScenicTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 景点（直接上架，无审核状态）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("village_scenic")
@Schema(title = "村落景区实体", description = "存储村落下属景区的基础信息")
public class VillageScenic {
    @TableId(type = IdType.AUTO)
    @Schema(description = "景区ID（自增主键）")
    private Long id;

    @Schema(description = "创建人 user.id")
    private Long userId;

    @Schema(description = "所属农村ID")
    private Long villageId;

    @Schema(description = "景区名称")
    private String name;

    @Schema(description = "景区详细介绍")
    private String intro;

    @Schema(description = "景区封面图片URL（多张用逗号分隔）")
    private String image;

    @Schema(description = "景区门票价格（单位：元，0表示免费）")
    private Integer price;

    @Schema(description = "景区类型：1自然景观 2人文景观 3娱乐体验 4民俗体验")
    private ScenicTypeEnum type;

    @Schema(description = "是否提供住宿：0-否、1-是")
    private Integer hasAccommodation;

    @Schema(description = "住宿详情（房型、价格、联系方式等）")
    private String accommodationInfo;

    @Schema(description = "点赞量")
    private Integer likes;

    @Schema(description = "收藏量")
    private Integer collections;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间（自动填充）")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "修改时间（自动填充）")
    private LocalDateTime updateTime;
}
