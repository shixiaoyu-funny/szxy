package com.shixiaoyu.xiangyueproject.entity.dto;

import com.shixiaoyu.xiangyueproject.enums.BusinessTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 农户 DTO：管理端建档 / 村长维护本村农户用
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "农户DTO")
public class FarmerUserDTO {
    @Schema(description = "登录用户名（建档时必填，留空默认用手机号）")
    private String username;

    @Schema(description = "手机号（建档时必填，唯一）")
    private String phone;

    @Schema(description = "身份证号码")
    private String idCard;

    @Schema(description = "经营类型：1民宿 2农产品 3文旅")
    private BusinessTypeEnum businessType;
}
