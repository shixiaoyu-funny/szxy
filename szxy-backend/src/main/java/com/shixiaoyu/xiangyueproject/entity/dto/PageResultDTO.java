package com.shixiaoyu.xiangyueproject.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页参数
 */
@Data
@Schema(title = "分页DTO")
public class PageResultDTO {
    @Schema(description = "页码（从1开始）")
    private Integer pageNo;

    @Schema(description = "每页条数")
    private Integer pageSize;
}
