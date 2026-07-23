package com.shixiaoyu.xiangyueproject.entity.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "村落基础信息实体DTO", description = "存储乡村的基础地理、介绍等核心信息")
public class VillageBaseDTO {
    /**
     * 农村id
     */
    @Schema(description = "村落ID（自增主键）")
    private Long id;
    /**
     * 村长id
     */
    @Schema(description = "村长ID（关联农户用户表主键）")
    @JsonAlias({"manage_id", "manageId"})
    private Long manageId;

    /**
     * 农村名称
     */
    @Schema(description = "村落名称（唯一）")
    private String name;

    /**
     * 省份
     */
    @Schema(description = "所属省份（如：浙江省）")
    private String province;

    /**
     * 城市
     */
    @Schema(description = "所属城市（如：杭州市）")
    private String city;

    /**
     * 区/县
     */
    @Schema(description = "所属区县（如：西湖区）")
    private String county;

    /**
     * 经度
     */
    @Schema(description = "村落经度（高精度坐标）")
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @Schema(description = "村落纬度（高精度坐标）")
    private BigDecimal latitude;

    /**
     * 村落类型（特色）
     */
    @Schema(description = "村落特色类型：1-古村落、2-生态村、3-民俗村、4-文旅村")
    private Integer type;

    /**
     * 村落介绍
     */
    @Schema(description = "村落详细介绍（文本）")
    private String intro;

    /**
     * 村落图片
     */
    @Schema(description = "村落封面图片URL（多张用逗号分隔）")
    private String image;

    /**
     * 最佳游玩时间
     */
    @Schema(description = "最佳游玩时间（如：3-5月、9-11月）")
    @JsonAlias({"best_time", "bestTime"})
    private String bestTime;

    /**
     * 季节性活动
     */
    @Schema(description = "季节性特色活动（如：油菜花节、丰收节）")
    private String activity;

    /**
     * 联系电话/邮箱
     */
    @Schema(description = "村落官方联系方式（电话/邮箱，用逗号分隔）")
    private String contact;
}
