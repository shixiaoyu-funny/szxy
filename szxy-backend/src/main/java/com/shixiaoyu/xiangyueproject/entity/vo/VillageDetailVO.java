package com.shixiaoyu.xiangyueproject.entity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 农村详情 VO（含下属景点列表）
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(title = "农村详情VO")
public class VillageDetailVO extends VillageBaseVO {

    @Schema(description = "村内景点列表")
    private List<ScenicVO> scenics;
}
