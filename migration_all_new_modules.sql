-- ============================================================
-- Consolidated Migration: All New Module Tables
-- Execute this to create all new tables at once
-- ============================================================
USE waimai;

-- Rider Income & Withdrawal
CREATE TABLE IF NOT EXISTS `rider_income` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `rider_id` BIGINT NOT NULL, `order_id` BIGINT NOT NULL,
    `amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00, `type` VARCHAR(16) NOT NULL DEFAULT 'DELIVERY',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), KEY `idx_rider_time` (`rider_id`, `create_time`), UNIQUE KEY `uk_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `rider_withdrawal` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `rider_id` BIGINT NOT NULL, `amount` DECIMAL(10,2) NOT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING', `remark` VARCHAR(256) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), KEY `idx_rider` (`rider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Rider enhancements (dynamic SQL for column existence check)
SET @s = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='waimai' AND TABLE_NAME='rider' AND COLUMN_NAME='balance') = 0,
    'ALTER TABLE rider ADD COLUMN balance DECIMAL(10,2) DEFAULT 0.00 AFTER score',
    'SELECT 1'
); PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='waimai' AND TABLE_NAME='rider' AND COLUMN_NAME='level') = 0,
    'ALTER TABLE rider ADD COLUMN level VARCHAR(16) DEFAULT ''BRONZE'' AFTER balance',
    'SELECT 1'
); PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='waimai' AND TABLE_NAME='rider' AND COLUMN_NAME='level_score') = 0,
    'ALTER TABLE rider ADD COLUMN level_score INT DEFAULT 0 AFTER level',
    'SELECT 1'
); PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Order enhancements (ETA + overtime)
SET @s = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='waimai' AND TABLE_NAME='order' AND COLUMN_NAME='estimated_minutes') = 0,
    'ALTER TABLE `order` ADD COLUMN estimated_minutes INT DEFAULT NULL AFTER complete_time',
    'SELECT 1'
); PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='waimai' AND TABLE_NAME='order' AND COLUMN_NAME='is_overtime') = 0,
    'ALTER TABLE `order` ADD COLUMN is_overtime TINYINT DEFAULT 0 AFTER estimated_minutes',
    'SELECT 1'
); PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- System Config
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `config_key` VARCHAR(64) NOT NULL,
    `config_value` VARCHAR(512) NOT NULL, `description` VARCHAR(256) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), UNIQUE KEY `uk_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO system_config (config_key, config_value, description) VALUES
('delivery_fee_default', '5.00', '默认配送费(元)'),
('auto_cancel_minutes', '30', '未支付自动取消时间(分钟)'),
('max_delivery_radius', '5.0', '最大配送半径(公里)'),
('rider_income_per_order', '5.00', '骑手每单基础收入(元)');

-- Coupon System
CREATE TABLE IF NOT EXISTS `coupon` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `name` VARCHAR(128) NOT NULL,
    `type` VARCHAR(32) NOT NULL, `threshold` DECIMAL(10,2) DEFAULT NULL,
    `discount_value` DECIMAL(10,2) NOT NULL, `total_count` INT NOT NULL DEFAULT 0,
    `received_count` INT NOT NULL DEFAULT 0, `used_count` INT NOT NULL DEFAULT 0,
    `merchant_id` BIGINT DEFAULT NULL, `valid_days` INT NOT NULL DEFAULT 7,
    `start_time` DATETIME DEFAULT NULL, `end_time` DATETIME DEFAULT NULL,
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_coupon` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `user_id` BIGINT NOT NULL, `coupon_id` BIGINT NOT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'UNUSED', `receive_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `use_time` DATETIME DEFAULT NULL, `expire_time` DATETIME NOT NULL,
    PRIMARY KEY (`id`), KEY `idx_user_status` (`user_id`, `status`), KEY `idx_coupon` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Order Dispute
CREATE TABLE IF NOT EXISTS `order_dispute` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `order_id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL,
    `type` VARCHAR(32) NOT NULL, `description` VARCHAR(512) NOT NULL,
    `images` VARCHAR(1024) DEFAULT NULL, `status` VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    `admin_remark` VARCHAR(512) DEFAULT NULL, `resolution` VARCHAR(256) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), KEY `idx_order` (`order_id`), KEY `idx_user` (`user_id`), KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Multi-Rider Collaborative Delivery
CREATE TABLE IF NOT EXISTS `delivery_batch` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `order_id` BIGINT NOT NULL,
    `batch_no` VARCHAR(32) NOT NULL, `total_sub_count` INT NOT NULL DEFAULT 0,
    `completed_sub_count` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(24) NOT NULL DEFAULT 'DISPATCHING',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), UNIQUE KEY `uk_batch_no` (`batch_no`), KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `delivery_sub_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT, `batch_id` BIGINT NOT NULL, `order_id` BIGINT NOT NULL,
    `rider_id` BIGINT DEFAULT NULL, `sub_address` VARCHAR(256) NOT NULL,
    `sub_address_lng` DECIMAL(10,7) DEFAULT NULL, `sub_address_lat` DECIMAL(10,7) DEFAULT NULL,
    `items_json` TEXT DEFAULT NULL,
    `status` VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    `estimated_minutes` INT DEFAULT NULL, `deliver_time` DATETIME DEFAULT NULL,
    `complete_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), KEY `idx_batch` (`batch_id`), KEY `idx_rider` (`rider_id`), KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
