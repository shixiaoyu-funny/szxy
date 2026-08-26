-- 已有库升级：景点评论支持楼中楼（parent_id）
ALTER TABLE `user_comment`
    ADD COLUMN `parent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '父评论ID，NULL为一级评论' AFTER `scenic_id`,
    ADD KEY `idx_parent` (`parent_id`);
