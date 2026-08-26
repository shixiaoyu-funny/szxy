-- =====================================================================
-- 「数智乡约」MVP 数据模型 DDL
-- 对应 docker-compose 的 MySQL（localhost:3399）
-- 使用：docker exec -i xiangyue-mysql mysql -uroot -p200609 < schema.sql
-- 或 docker exec -i xiangyue-mysql mysql -uroot -p200609 xiangyue < data.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS xiangyue DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xiangyue;

-- ---------------------------------------------------------------------
-- 1. user 全角色统一账号
--    role : 1游客 / 2农户 / 3村长 / 4管理员（与 RoleEnum 一一对应）
--    status: 1正常 / 0禁用
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50)     NOT NULL DEFAULT '' COMMENT '登录用户名（唯一）',
    `password`    VARCHAR(100)    NOT NULL DEFAULT '' COMMENT 'BCrypt 加密密码',
    `phone`       VARCHAR(20)     DEFAULT NULL COMMENT '手机号（唯一）',
    `email`       VARCHAR(100)    DEFAULT NULL COMMENT '邮箱（唯一）',
    `avatar`      VARCHAR(255)    DEFAULT NULL COMMENT '头像URL',
    `role`        TINYINT         NOT NULL DEFAULT 1 COMMENT '角色：1游客 2农户 3村长 4管理员',
    `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_role` (`role`)
) ENGINE = InnoDB COMMENT ='全角色统一账号';

-- ---------------------------------------------------------------------
-- 2. village_base 农村基础信息
--    manage_id : 村长 user.id（与 user.role=3 在事务内同步）
--    type      : 1古村落 2生态村 3民俗村 4文旅村（VillageTypeEnum）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `village_base` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '农村ID',
    `manage_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '村长 user.id',
    `name`        VARCHAR(100)    NOT NULL COMMENT '农村名称（唯一）',
    `province`    VARCHAR(50)     DEFAULT NULL COMMENT '省',
    `city`        VARCHAR(50)     DEFAULT NULL COMMENT '市',
    `county`      VARCHAR(50)     DEFAULT NULL COMMENT '区县',
    `longitude`   DECIMAL(10, 6)  DEFAULT NULL COMMENT '经度',
    `latitude`    DECIMAL(10, 6)  DEFAULT NULL COMMENT '纬度',
    `type`        TINYINT         DEFAULT NULL COMMENT '村落特色类型',
    `intro`       TEXT            COMMENT '村落介绍',
    `image`       VARCHAR(1000)   DEFAULT NULL COMMENT '封面图URL（多张逗号分隔）',
    `best_time`   VARCHAR(50)     DEFAULT NULL COMMENT '最佳游玩时间',
    `activity`    VARCHAR(200)    DEFAULT NULL COMMENT '季节性活动',
    `contact`     VARCHAR(200)    DEFAULT NULL COMMENT '联系方式',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_manage` (`manage_id`)
) ENGINE = InnoDB COMMENT ='农村基础信息';

-- ---------------------------------------------------------------------
-- 3. farm_user 农户档案（纯业务档案，无审核状态）
--    user_id    与 user 一一对应（唯一）
--    business_type : 1民宿经营者 2农产品销售者 3文旅服务者（BusinessTypeEnum）
--    禁用走 user.status，此处不重复存状态
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `farm_user` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '农户档案ID',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '关联 user.id（唯一）',
    `village_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '所属农村ID',
    `id_card`       VARCHAR(20)     DEFAULT NULL COMMENT '身份证号',
    `business_type` TINYINT         DEFAULT NULL COMMENT '经营类型：1民宿 2农产品 3文旅',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user` (`user_id`),
    KEY `idx_village` (`village_id`)
) ENGINE = InnoDB COMMENT ='农户档案';

-- ---------------------------------------------------------------------
-- 4. village_scenic 景点（直接上架，无审核状态）
--    user_id : 创建人 user.id
--    type    : 1自然景观 2人文景观 3娱乐体验 4民俗体验（ScenicTypeEnum）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `village_scenic` (
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '景点ID',
    `user_id`            BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人 user.id',
    `village_id`         BIGINT UNSIGNED NOT NULL COMMENT '所属农村ID',
    `name`               VARCHAR(100)    NOT NULL COMMENT '景点名称',
    `intro`              TEXT            COMMENT '景点介绍',
    `image`              VARCHAR(1000)   DEFAULT NULL COMMENT '图片URL（多张逗号分隔）',
    `price`              INT             NOT NULL DEFAULT 0 COMMENT '门票价格（元，0免费）',
    `type`               TINYINT         DEFAULT NULL COMMENT '景点类型',
    `has_accommodation`  TINYINT         NOT NULL DEFAULT 0 COMMENT '是否提供住宿：0否 1是',
    `accommodation_info` VARCHAR(500)    DEFAULT NULL COMMENT '住宿详情',
    `likes`              INT             NOT NULL DEFAULT 0 COMMENT '点赞量',
    `collections`        INT             NOT NULL DEFAULT 0 COMMENT '收藏量',
    `create_time`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_village` (`village_id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT ='景点';

-- ---------------------------------------------------------------------
-- 5. user_comment 评论（仅针对景点）
--    is_show : 0隐藏 1展示（CommentShowEnum）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_comment` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '评论人 user.id',
    `scenic_id`   BIGINT UNSIGNED NOT NULL COMMENT '景点ID',
    `parent_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '父评论ID，NULL为一级评论',
    `content`     VARCHAR(500)    DEFAULT NULL COMMENT '评论内容',
    `score`       TINYINT         DEFAULT NULL COMMENT '评分1-5',
    `comment_img` VARCHAR(1000)   DEFAULT NULL COMMENT '评论图片URL',
    `is_show`     TINYINT         NOT NULL DEFAULT 1 COMMENT '是否展示：0隐藏 1展示',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_scenic` (`scenic_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_parent` (`parent_id`)
) ENGINE = InnoDB COMMENT ='景点评论';

-- ---------------------------------------------------------------------
-- 6/7. user_like / user_collect 点赞/收藏（唯一约束保证幂等）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_like` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户 user.id',
    `scenic_id`   BIGINT UNSIGNED NOT NULL COMMENT '景点ID',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_scenic` (`user_id`, `scenic_id`),
    KEY `idx_scenic` (`scenic_id`)
) ENGINE = InnoDB COMMENT ='点赞';

CREATE TABLE IF NOT EXISTS `user_collect` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户 user.id',
    `scenic_id`   BIGINT UNSIGNED NOT NULL COMMENT '景点ID',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_scenic` (`user_id`, `scenic_id`),
    KEY `idx_scenic` (`scenic_id`)
) ENGINE = InnoDB COMMENT ='收藏';

-- ---------------------------------------------------------------------
-- 8. farmer_access 游客申请成为农户（审批表）
--    info : JSON 快照 {username,phone,email,avatar}，不做条件检索
--    status : 0待审 / 1已通过 / 2已拒绝
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- 9. vghead_access 农户申请成为村长
--    village_id 冗余存村，便于绑名称；fu_id = farm_user.id
--    status : 0待审 / 1村长已审 / 2管理员已审 / 3已拒绝
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- 10. system_config 系统配置（AI / 第三方服务等）
--     config_key 主键；业务密钥、模型名、Milvus 地址等
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `system_config` (
    `config_key`   VARCHAR(32)  NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(256) NOT NULL COMMENT '配置值',
    PRIMARY KEY (`config_key`)
) ENGINE = InnoDB COMMENT ='系统配置';

-- ---------------------------------------------------------------------
-- 11. chat_session AI 聊天会话（方案二：会话元数据）
--     simple_desc : 侧栏列表展示用概要/标题
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat_session` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户 user.id',
    `simple_desc` VARCHAR(64)     DEFAULT NULL COMMENT '会话概要（侧栏标题）',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_update` (`user_id`, `update_time` DESC)
) ENGINE = InnoDB COMMENT ='AI 聊天会话';

-- ---------------------------------------------------------------------
-- 12. chat_message AI 聊天消息（方案二：一条一行）
--     role : 1用户 / 2AI（ChatMessageRoleEnum）
--     加载上下文：WHERE session_id=? ORDER BY create_time ASC LIMIT N
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id`  BIGINT UNSIGNED NOT NULL COMMENT '会话 chat_session.id',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户 user.id（冗余）',
    `role`        TINYINT         NOT NULL COMMENT '1用户 2AI',
    `content`     TEXT            NOT NULL COMMENT '消息正文',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_time` (`session_id`, `create_time`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT ='AI 聊天消息';
