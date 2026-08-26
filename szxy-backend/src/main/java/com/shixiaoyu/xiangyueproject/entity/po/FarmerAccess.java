package com.shixiaoyu.xiangyueproject.entity.po;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shixiaoyu.xiangyueproject.enums.AccessStatusEnum;
import com.shixiaoyu.xiangyueproject.enums.BusinessTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 农户准入申请（游客申请 → 管理端审批）
 * info 库字段存 JSON；业务侧通过组合对象 userCacheInfo 读写
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("farmer_access")
@Schema(title = "农户准入申请", description = "游客申请成为农户的表单与审批记录")
public class FarmerAccess {

    @TableId(type = IdType.AUTO)
    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "申请人 user.id")
    private Long userId;

    /** 入库 JSON 字符串，对应列 info */
    @TableField("info")
    @Schema(description = "用户基础信息 JSON 快照")
    private String info;

    /** 组合对象：不入库，与 info 互转 */
    @TableField(exist = false)
    @Schema(description = "用户基础信息快照对象")
    private UserCacheInfo userCacheInfo;

    @Schema(description = "身份证号")
    private String idCard;

    @Schema(description = "所属农村ID")
    private Long villageId;

    @Schema(description = "经营类型")
    private BusinessTypeEnum businessType;

    @Schema(description = "申请状态：0待审 1已通过 2已拒绝")
    private AccessStatusEnum status;

    @Schema(description = "审批管理员 user.id")
    private Long auditUserId;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 将 userCacheInfo 序列化为 info（写库前调用） */
    public void packInfo() {
        if (userCacheInfo != null) {
            this.info = JSONUtil.toJsonStr(userCacheInfo);
        }
    }

    /** 将 info 反序列化为 userCacheInfo（读库后调用） */
    public void unpackInfo() {
        if (StrUtil.isNotBlank(info)) {
            this.userCacheInfo = JSONUtil.toBean(info, UserCacheInfo.class);
        }
    }
}
