package com.shixiaoyu.xiangyueproject.entity.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "用户收货地址")
public class UserAddressVO {
    @TableId(type = IdType.AUTO)
    @Schema(description = "地址ID")
    private Long id;

    @Schema(description = "所属用户 user.id")
    private Long userId;

    @Schema(description = "收件人姓名")
    private String receiverName;

    @Schema(description = "收件人手机号")
    private String phone;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区/县")
    private String county;

    @Schema(description = "详细地址（街道门牌等）")
    private String detail;

    @Schema(description = "是否默认地址：0否 1是")
    private Integer isDefault;
}
