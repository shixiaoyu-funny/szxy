package com.shixiaoyu.xiangyueproject.entity.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(title = "农户VO", description = "农户VO")
public class FarmerUserVO {
    /**
     * 农户ID
     */
    @Schema(description = "农户ID（自增主键）")
    private Long id;

    /**
     * 村落名称
     */
    @Schema(description = "村落名称")
    @JsonAlias({"village_id", "villageId"})
    private Long villageId;

    /**
     *  农户姓名
     */
    @Schema(description = "农户真实姓名")
    @JsonAlias({"farm_name", "farmName"})
    private String farmName;

    /**
     * 账号状态
     */
    @Schema(description = "账号状态：0-待审核、1-已通过、2-已拒绝、3-已禁用")
    private Integer status;

    /**
     * 农户类型
     */
    @Schema(description = "农户类型：1-民宿经营者、2-农产品销售者、3-文旅服务者")
    private Integer type;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间（自动填充）")
    @JsonAlias({"create_time", "createTime"})
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间（自动填充）")
    @JsonAlias({"update_time", "updateTime"})
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /**
     * 用户名
     */
    @Schema(description = "登录用户名（唯一）")
    private String username;

    /**
     * 手机号
     */
    @Schema(description = "用户手机号（唯一）")
    private String phone;
}

