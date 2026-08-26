-- 已有库升级：村长准入申请
USE xiangyue;

CREATE TABLE IF NOT EXISTS `vghead_access` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `village_id`  BIGINT UNSIGNED NOT NULL COMMENT '村落ID（冗余，绑名称）',
    `fu_id`       BIGINT UNSIGNED NOT NULL COMMENT '申请人 farm_user.id',
    `status`      TINYINT         NOT NULL DEFAULT 0 COMMENT '0待审 1村长已审 2管理员已审 3已拒绝',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_village_status` (`village_id`, `status`),
    KEY `idx_fu` (`fu_id`)
) ENGINE = InnoDB COMMENT ='村长准入申请';
