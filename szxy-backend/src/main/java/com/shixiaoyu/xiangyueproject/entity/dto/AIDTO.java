package com.shixiaoyu.xiangyueproject.entity.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "AI通用DTO", description = "存储用户向AI发出的请求信息")
public class AIDTO {
    /**
     * 用户预期游览的农村所属省份
     */
    @Schema(description = "用户预期游览的农村所属省份，非必须")
    private String province;

    /**
     * 用户预期游览的农村所属地级市
     */
    @Schema(description = "用户预期游览的农村所属地级市，非必须")
    private String city;

    /**
     * 行程天数（AI行程）
     */
    @JsonAlias({"trip_days", "tripDays"})
    @Schema(description = "行程天数（如1/2/3天）")
    private Integer tripDays;

    /**
     * 出行人数（1-单人/2-双人/3-家庭/4-多人）（AI行程）
     */
    @JsonAlias({"trip_people", "tripPeople"})
    @Schema(description = "出行人数类型：1-单人、2-双人、3-家庭、4-多人")
    private Integer tripPeople;

    /**
     * 行程偏好（1-休闲/2-打卡/3-亲子/4-康养）（AI行程）
     */
    @JsonAlias({"trip_prefer", "tripPrefer"})
    @Schema(description = "行程偏好类型：1-休闲、2-打卡、3-亲子、4-康养")
    private Integer tripPrefer;

    /**
     * 预算（AI行程+住宿推荐）
     */
    @Schema(description = "预算")
    private Integer money;

    /**
     * 用户输入内容（行程+住宿推荐+智能体回答）
     */
    @Schema(description = "用户输入内容")
    private String content;

    /**
     * 多模态图片识别
     */
    @Schema(description = "图片url")
    private String url;
}
