-- 即时通讯系统数据库初始化脚本
-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `im` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `im`;

-- 用户表
CREATE TABLE IF NOT EXISTS `t_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID（唯一标识）',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
  `nickname` VARCHAR(100) NOT NULL COMMENT '昵称',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 群组表
CREATE TABLE IF NOT EXISTS `t_group` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` VARCHAR(64) NOT NULL COMMENT '群组ID（唯一标识）',
  `group_name` VARCHAR(100) NOT NULL COMMENT '群组名称',
  `creator_id` VARCHAR(64) NOT NULL COMMENT '创建者用户ID',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '群组头像URL',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '群组描述',
  `member_count` INT NOT NULL DEFAULT 0 COMMENT '成员数量',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-解散，1-正常',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_id` (`group_id`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组表';

-- 群组成员表
CREATE TABLE IF NOT EXISTS `t_group_member` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `group_id` VARCHAR(64) NOT NULL COMMENT '群组ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `role` VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT '角色：owner-群主，admin-管理员，member-成员',
  `nickname` VARCHAR(100) DEFAULT NULL COMMENT '群内昵称',
  `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_joined_at` (`joined_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组成员表';

-- 消息表
CREATE TABLE IF NOT EXISTS `t_message` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `message_id` VARCHAR(64) NOT NULL COMMENT '消息ID（唯一标识）',
  `from_user_id` VARCHAR(64) NOT NULL COMMENT '发送者用户ID',
  `to_user_id` VARCHAR(64) DEFAULT NULL COMMENT '接收者用户ID（私聊时使用）',
  `group_id` VARCHAR(64) DEFAULT NULL COMMENT '群组ID（群聊时使用）',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `message_type` TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型：1-私聊，2-群聊，3-AI对话',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-删除，1-正常',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_id` (`message_id`),
  KEY `idx_from_user` (`from_user_id`, `created_at`),
  KEY `idx_to_user` (`to_user_id`, `created_at`),
  KEY `idx_group` (`group_id`, `created_at`),
  KEY `idx_message_type` (`message_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 插入测试数据（可选）
-- 创建测试用户
INSERT INTO `t_user` (`user_id`, `password`, `nickname`, `email`, `status`) VALUES
('user001', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '测试用户1', 'user1@example.com', 1),
('user002', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '测试用户2', 'user2@example.com', 1),
('user003', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '测试用户3', 'user3@example.com', 1)
ON DUPLICATE KEY UPDATE `user_id`=`user_id`;

-- 授予权限
GRANT ALL PRIVILEGES ON `im`.* TO 'imuser'@'%';
FLUSH PRIVILEGES;

-- 显示创建的表
SHOW TABLES;

