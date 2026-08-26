-- 已有库升级：系统配置表
USE xiangyue;

CREATE TABLE IF NOT EXISTS `system_config` (
    `config_key`   VARCHAR(32)  NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(256) NOT NULL COMMENT '配置值',
    PRIMARY KEY (`config_key`)
) ENGINE = InnoDB COMMENT ='系统配置';
