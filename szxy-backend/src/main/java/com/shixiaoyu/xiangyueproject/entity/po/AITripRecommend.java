package com.shixiaoyu.xiangyueproject.entity.po;

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
@Schema(title = "AI行程推荐实体", description = "存储AI生成的乡村旅游行程推荐信息")
public class AITripRecommend {
    /**
     * 主键ID
     */
    @Schema(description = "主键ID（自增）")
    private Long id;

    /**
     * 村落ID（关联village_base.id）
     */
    @Schema(description = "村落ID（关联村落基础表主键）")
    private Long villageId;

    /**
     * 行程天数
     */
    @Schema(description = "行程天数（如1/2/3天）")
    private Integer tripDays;

    /**
     * 出行人数（1-单人/2-双人/3-家庭/4-多人）
     */
    @Schema(description = "出行人数类型：1-单人、2-双人、3-家庭、4-多人")
    private Integer tripPeople;

    /**
     * 行程偏好（1-休闲/2-打卡/3-亲子/4-康养）
     */
    @Schema(description = "行程偏好类型：1-休闲、2-打卡、3-亲子、4-康养")
    private Integer tripPrefer;

    /**
     * 详细行程内容
     */
    @Schema(description = "AI生成的详细行程内容（文本）")
    private String tripContent;

    /**
     * 封面图URL
     */
    @Schema(description = "行程封面图URL地址")
    private String tripCover;

    /**
     * 用户反馈（0-未评/1-满意/2-一般/3-不满意）
     */
    @Schema(description = "用户反馈状态：0-未评价、1-满意、2-一般、3-不满意")
    private Integer userFeedback;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间（自动填充）")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
