package com.shixiaoyu.xiangyueproject.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改购物车项（数量 / 勾选）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "修改购物车项")
public class CartAlterDTO {
    @Schema(description = "购买数量（传则更新，须 >= 1）")
    private Integer quantity;

    @Schema(description = "是否勾选结算：1是 0否（传则更新）")
    private Integer selected;
}
