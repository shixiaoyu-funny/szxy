package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户收货地址
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_address")
@Schema(title = "用户收货地址")
public class UserAddress {
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

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
