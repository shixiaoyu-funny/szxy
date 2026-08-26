-- 已有库升级：游客申请成为农户
USE xiangyue;

CREATE TABLE IF NOT EXISTS `farmer_access` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `user_id`         BIGINT UNSIGNED NOT NULL COMMENT '申请人 user.id',
    `info`            VARCHAR(1000)   NOT NULL DEFAULT '' COMMENT '用户基础信息JSON快照',
    `id_card`         VARCHAR(20)     NOT NULL DEFAULT '' COMMENT '身份证号',
    `village_id`      BIGINT UNSIGNED NOT NULL COMMENT '所属农村ID',
    `business_type`   TINYINT         NOT NULL COMMENT '经营类型：1民宿 2农产品 3文旅',
    `status`          TINYINT         NOT NULL DEFAULT 0 COMMENT '0待审 1已通过 2已拒绝',
    `audit_user_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '审批管理员 user.id',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（改提/审批）',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT ='农户准入申请';
