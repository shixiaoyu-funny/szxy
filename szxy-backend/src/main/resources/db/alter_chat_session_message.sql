-- AI 会话（方案二：会话 + 消息分表）
-- 已有库增量执行：docker exec -i xiangyue-mysql mysql -uroot -p200609 xiangyue < alter_chat_session_message.sql

USE xiangyue;

CREATE TABLE IF NOT EXISTS `chat_session` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户 user.id',
    `simple_desc` VARCHAR(64)     DEFAULT NULL COMMENT '会话概要（侧栏标题）',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_update` (`user_id`, `update_time` DESC)
) ENGINE = InnoDB COMMENT ='AI 聊天会话';

CREATE TABLE IF NOT EXISTS `chat_message` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id`  BIGINT UNSIGNED NOT NULL COMMENT '会话 chat_session.id',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户 user.id（冗余）',
    `role`        TINYINT         NOT NULL COMMENT '1用户 2AI（ChatMessageRoleEnum）',
    `content`     TEXT            NOT NULL COMMENT '消息正文',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_time` (`session_id`, `create_time`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT ='AI 聊天消息';
