package com.shixiaoyu.xiangyueproject.entity.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.shixiaoyu.xiangyueproject.entity.po.VillageScenic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "AI推荐VO", description = "AI推荐VO")
public class AIRecommendVO {
    /**
     * AI推荐的农村及其下属景点列表（仅行程推荐使用）
     */
    @Schema(description = "AI推荐的乡村及其下属景点列表")
    @JsonAlias({"recommend_list", "recommendList"})
    private List<Map<String,List<VillageScenic>>> recommendList;

    /**
     * 详细行程内容（三种智能体都需要）
     */
    @Schema(description = "AI回复内容")
    private String content;

    /**
     * AI生成类型
     */
    @Schema(description = "AI生成类型")
    private Integer type;
}
