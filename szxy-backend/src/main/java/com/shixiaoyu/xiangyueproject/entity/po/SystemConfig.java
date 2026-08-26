package com.shixiaoyu.xiangyueproject.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统配置（KV）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("system_config")
@Schema(title = "系统配置")
public class SystemConfig {

    @TableId(value = "config_key", type = IdType.INPUT)
    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置值")
    private String configValue;
}
